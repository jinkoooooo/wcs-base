package operato.logis.wcs.common.service.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import operato.logis.wcs.entity.TbWcsAuditLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.manager.DbistQueryManager;
import xyz.elidom.sys.entity.User;
import xyz.elidom.util.ValueUtil;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * audit 직렬화·diff·insert 를 담당하는 비동기 worker.
 * 메인 스레드에서는 행위자(actor) 캡처와 객체 참조 전달까지만 하고,
 * diff·reflection·JSON 직렬화·persist 는 모두 executor 스레드에서 수행한다.
 *
 * AuditingInvocationHandler(@Primary IQueryManager 프록시)가 주입받아 실제 기록을 위임한다.
 */
@Service
public class AuditWorker {

    private static final Logger logger = LoggerFactory.getLogger(AuditWorker.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
    private static final int MAX_JSON_BYTES = 4000;

    private final IQueryManager qm;
    private final TaskExecutor executor;

    // qm 은 raw DbistQueryManager 로 받아 @Primary 감사 프록시 재진입(무한 재귀)을 차단한다.
    public AuditWorker(DbistQueryManager qm,
                       @Qualifier("auditExecutor") TaskExecutor executor) {
        this.qm = qm;
        this.executor = executor;
    }

    /** INSERT 진입점. actor 만 캡처하고 build·persist 는 executor 로 넘긴다. */
    void afterInsert(Object entity, String reason, String caller) {
        if (!auditable(entity)) return;
        final ActorSnapshot actor = ActorSnapshot.now();
        submit(() -> persist(buildInsert(entity, reason, caller, actor)));
    }

    /** INSERT 다건 진입점. 프록시가 모아 준 entity 목록을 한 번에 executor 로 넘긴다. */
    void afterInsertBatch(List<Object> entities, String reason, String caller) {
        if (ValueUtil.isEmpty(entities)) return;
        final ActorSnapshot actor = ActorSnapshot.now();
        submit(() -> {
            for (Object e : entities) {
                if (auditable(e)) persist(buildInsert(e, reason, caller, actor));
            }
        });
    }

    /** UPDATE 진입점. actor 만 캡처하고 diff·build·persist 는 executor 로 넘긴다. */
    void afterUpdate(Object before, Object after, String[] columns,
                     String reason, String caller) {
        final ActorSnapshot actor = ActorSnapshot.now();
        submit(() -> persist(buildUpdate(before, after, columns, reason, caller, actor)));
    }

    /**
     * UPDATE/UPSERT 다건 진입점. 프록시가 [before, after] 쌍만 캡처해 넘기면
     * diff·build·persist 는 executor 에서 수행한다.
     * UPSERT 는 before 가 비면 INSERT, 있으면 UPDATE 로 분기한다.
     */
    void afterUpdateBatch(String action, List<Object[]> pairs, String[] columns,
                          String reason, String caller) {
        if (ValueUtil.isEmpty(pairs)) return;
        final ActorSnapshot actor = ActorSnapshot.now();
        submit(() -> {
            for (Object[] pair : pairs) {
                Object before = pair[0];
                Object after = pair[1];
                if (!auditable(after)) continue;
                if ("UPSERT".equals(action)) {
                    if (ValueUtil.isEmpty(before)) persist(buildInsert(after, reason, caller, actor));
                    else persist(buildUpdate(before, after, columns, reason, caller, actor));
                } else {
                    persist(buildUpdate(before, after, columns, reason, caller, actor));
                }
            }
        });
    }

    /** DELETE 진입점. 삭제 직전 snapshot 직렬화를 executor 로 미룬다. */
    void afterDelete(Object entity, String reason, String caller) {
        if (!auditable(entity)) return;
        final ActorSnapshot actor = ActorSnapshot.now();
        submit(() -> persist(buildDelete(entity, reason, caller, actor)));
    }

    /** DELETE 다건 진입점. 프록시가 모아 준 삭제 대상 목록을 한 번에 executor 로 넘긴다. */
    void afterDeleteBatch(List<Object> entities, String reason, String caller) {
        if (ValueUtil.isEmpty(entities)) return;
        final ActorSnapshot actor = ActorSnapshot.now();
        submit(() -> {
            for (Object e : entities) {
                if (auditable(e)) persist(buildDelete(e, reason, caller, actor));
            }
        });
    }

    /** executeBySql 결과의 row 별 diff 진입점. actor 만 캡처하고, PK 매칭·diff 는 executor 에서 수행한다. */
    void afterSql(List<?> before, List<?> after, String pkField,
                  String reason, String caller) {
        final ActorSnapshot actor = ActorSnapshot.now();
        submit(() -> {
            // PK 인덱싱 후 합집합 키로 전/후 매칭 (executor 스레드에서 diff)
            Map<Object, Object> bMap = indexByPk(before, pkField);
            Map<Object, Object> aMap = indexByPk(after, pkField);
            Set<Object> keys = new LinkedHashSet<>();
            keys.addAll(bMap.keySet());
            keys.addAll(aMap.keySet());
            for (Object k : keys) {
                // null 이면 persist 내부에서 무시된다
                persist(buildUpdate(bMap.get(k), aMap.get(k), null, reason, caller, actor));
            }
        });
    }

    /** pkField = 단일 PK 또는 PK 컬렉션(IN)으로 영향 row 를 SELECT. 프록시의 벌크 Before 확보가 IN 으로 호출한다. */
    public <T> List<T> selectByPk(Class<T> cls, String pkField, Object pkValueOrList) {
        try {
            Query q = OrmUtil.newConditionForExecution();
            // 컬렉션이면 IN, 단일이면 등치 필터
            if (pkValueOrList instanceof Collection<?> coll) {
                if (ValueUtil.isEmpty(coll)) return Collections.emptyList();
                q.addFilter(pkField, OrmConstants.IN, pkValueOrList);
            } else {
                if (ValueUtil.isEmpty(pkValueOrList)) return Collections.emptyList();
                q.addFilter(pkField, pkValueOrList);
            }
            return qm.selectList(cls, q);
        } catch (Throwable t) {
            logger.warn("[ Audit ][ Worker ] selectByPk failed - cls={}, pk={}", cls.getSimpleName(), pkField, t);
            return Collections.emptyList();
        }
    }

    /** executor 로 task 제출. 거부·예외가 나도 비즈 동작에 영향 없이 경고만 남긴다. */
    private void submit(Runnable r) {
        if (ValueUtil.isEmpty(r)) return;
        try {
            executor.execute(r);
        } catch (Throwable t) {
            // DiscardPolicy 면 여기 안 옴. 다른 정책·예외일 때만 경고
            logger.warn("[ Audit ][ Worker ] enqueue failed", t);
        }
    }

    /** 별도 트랜잭션(REQUIRES_NEW)으로 audit log insert. 비즈 트랜잭션과 분리한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void insertLog(TbWcsAuditLog log) {
        qm.insert(log);
    }

    /** insertLog 호출 + 실패 격리. audit 실패가 비즈에 전파되지 않게 catch 한다. */
    private void persist(TbWcsAuditLog log) {
        if (ValueUtil.isEmpty(log)) return;
        try {
            insertLog(log);
        } catch (Throwable t) {
            logger.warn("[ Audit ][ Worker ] persist failed - action={}, table={}", log.getAction(), log.getTableName(), t);
        }
    }

    /** 감사 대상 판별 (로그 테이블 본인은 제외시켜 무한루프 방어). */
    private static boolean auditable(Object o) {
        return ValueUtil.isNotEmpty(o) && !(o instanceof TbWcsAuditLog);
    }

    /** INSERT audit log 빌드 (executor 스레드 실행). after = entity 직렬화. */
    private TbWcsAuditLog buildInsert(Object entity, String reason, String caller, ActorSnapshot actor) {
        try {
            if (ValueUtil.isEmpty(entity)) return null;
            TbWcsAuditLog log = baseLog(entity, "INSERT", reason, caller, actor);
            log.setAfterJson(toJson(entity));
            return log;
        } catch (Throwable t) {
            logger.warn("[ Audit ][ Worker ] buildInsert failed", t);
            return null;
        }
    }

    /** UPDATE audit log 빌드. 변경된 컬럼만 before/after 맵으로 기록하고, 변경 없으면 null 로 생략한다. */
    private TbWcsAuditLog buildUpdate(Object before, Object after, String[] columns,
                                      String reason, String caller, ActorSnapshot actor) {
        try {
            Object ref = ValueUtil.isNotEmpty(after) ? after : before;
            if (ValueUtil.isEmpty(ref)) return null;
            // 변경 컬럼 없으면 기록 생략
            Map<String, Object[]> diff = computeDiff(before, after, columns);
            if (ValueUtil.isEmpty(diff)) return null;
            TbWcsAuditLog log = baseLog(ref, "UPDATE", reason, caller, actor);
            // diff 를 before/after 맵으로 분리
            Map<String, Object> beforeMap = new LinkedHashMap<>();
            Map<String, Object> afterMap = new LinkedHashMap<>();
            for (Map.Entry<String, Object[]> e : diff.entrySet()) {
                beforeMap.put(e.getKey(), e.getValue()[0]);
                afterMap.put(e.getKey(), e.getValue()[1]);
            }
            log.setChangedColumns(String.join(",", diff.keySet()));
            log.setBeforeJson(toJson(beforeMap));
            log.setAfterJson(toJson(afterMap));
            return log;
        } catch (Throwable t) {
            logger.warn("[ Audit ][ Worker ] buildUpdate failed", t);
            return null;
        }
    }

    /** 공통 메타 채움. 행위자는 명시 actor 컬럼에 기록. */
    private TbWcsAuditLog baseLog(Object entity, String action, String reason,
                                  String caller, ActorSnapshot actor) {
        TbWcsAuditLog log = new TbWcsAuditLog();
        log.setAction(action);
        log.setEntityClass(entity.getClass().getSimpleName());
        log.setTableName(tableNameOf(entity.getClass()));
        log.setPkValue(pkValueStrOf(entity));
        log.setCaller(caller);
        log.setReason(reason);
        // 행위자 = 명시 actor 컬럼
        log.setActorType(actor.type.name());
        log.setActorId(actor.id);
        log.setActorName(actor.name);
        log.setChannel(actor.channel);
        // 비동기 스레드는 stamp hook 미발화 — 로그 row 작성자도 행위자로 채움
        log.setCreatorId(actor.id);
        log.setUpdaterId(actor.id);
        return log;
    }

    /** @Table.name 우선, 없으면 클래스 단순명으로 테이블명 결정. */
    private static String tableNameOf(Class<?> cls) {
        Table t = cls.getAnnotation(Table.class);
        return ValueUtil.isNotEmpty(t) ? t.name() : cls.getSimpleName();
    }

    /** @PrimaryKey 필드 값들을 배열로 추출 (select 인자용). */
    static Object[] pkValuesOf(Object entity) {
        if (ValueUtil.isEmpty(entity)) return new Object[0];
        List<Object> values = new ArrayList<>();
        try {
            for (Field f : allFields(entity.getClass())) {
                if (f.isAnnotationPresent(PrimaryKey.class)) {
                    f.setAccessible(true);
                    Object v = f.get(entity);
                    if (ValueUtil.isNotEmpty(v)) values.add(v);
                }
            }
        } catch (Throwable ignore) {
        }
        return values.toArray();
    }

    /** 단일 PK 컬럼 필드명 추출. 복합키거나 PK 가 없으면 null (벌크 IN 조회 가능 여부 판정용). */
    static String singlePkFieldNameOf(Class<?> cls) {
        if (ValueUtil.isEmpty(cls)) return null;
        String found = null;
        for (Field f : allFields(cls)) {
            if (f.isAnnotationPresent(PrimaryKey.class)) {
                if (found != null) return null; // 복합키면 벌크 IN 불가
                found = f.getName();
            }
        }
        return found;
    }

    /** 단일 PK 값을 매칭 key 로 추출. 복합키거나 PK 가 없으면 null (벌크 Before 매칭용). */
    static Object pkKeyOf(Object entity) {
        if (ValueUtil.isEmpty(entity)) return null;
        Object found = null;
        try {
            for (Field f : allFields(entity.getClass())) {
                if (f.isAnnotationPresent(PrimaryKey.class)) {
                    if (found != null) return null; // 복합키면 단일 key 로 못 씀
                    f.setAccessible(true);
                    found = f.get(entity);
                }
            }
        } catch (Throwable ignore) {
            return null;
        }
        return found;
    }

    /** @PrimaryKey 필드들을 "name=value;..." 문자열로 직렬화 (audit pk_value 컬럼용). */
    private static String pkValueStrOf(Object entity) {
        try {
            List<String> parts = new ArrayList<>();
            for (Field f : allFields(entity.getClass())) {
                if (f.isAnnotationPresent(PrimaryKey.class)) {
                    f.setAccessible(true);
                    Object v = f.get(entity);
                    if (ValueUtil.isNotEmpty(v)) parts.add(f.getName() + "=" + v);
                }
            }
            return ValueUtil.isEmpty(parts) ? "" : String.join(";", parts);
        } catch (Throwable t) {
            return "";
        }
    }

    /** list 를 pkField 값으로 인덱싱한 맵으로 변환 (전/후 row 매칭용). */
    private Map<Object, Object> indexByPk(List<?> list, String pkField) {
        Map<Object, Object> map = new LinkedHashMap<>();
        if (ValueUtil.isEmpty(list)) return map;
        for (Object o : list) {
            try {
                Field f = findField(o.getClass(), pkField);
                if (ValueUtil.isEmpty(f)) continue;
                f.setAccessible(true);
                Object v = f.get(o);
                if (ValueUtil.isNotEmpty(v)) map.put(v, o);
            } catch (Throwable ignore) {
            }
        }
        return map;
    }

    /** @Column 필드를 비교해 변경된 컬럼만 name -> [before, after] 로 수집. columns 지정 시 해당 컬럼만. */
    private static Map<String, Object[]> computeDiff(Object before, Object after, String[] columns) {
        Map<String, Object[]> result = new LinkedHashMap<>();
        Object ref = ValueUtil.isNotEmpty(after) ? after : before;
        if (ValueUtil.isEmpty(ref)) return result;
        Set<String> filter = (ValueUtil.isEmpty(columns) || columns.length == 0)
                ? null : new HashSet<>(Arrays.asList(columns));
        for (Field f : allFields(ref.getClass())) {
            if (ValueUtil.isNotEmpty(filter) && !filter.contains(f.getName())) continue;
            if (!f.isAnnotationPresent(Column.class)) continue;
            try {
                f.setAccessible(true);
                Object b = before != null ? f.get(before) : null;
                Object a = after != null ? f.get(after) : null;
                if (!Objects.equals(b, a)) result.put(f.getName(), new Object[]{b, a});
            } catch (Throwable ignore) {
            }
        }
        return result;
    }

    /** 상속 계층 전체의 선언 필드를 수집 (private 포함). */
    private static List<Field> allFields(Class<?> cls) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = cls; ValueUtil.isNotEmpty(c) && c != Object.class; c = c.getSuperclass()) {
            Collections.addAll(fields, c.getDeclaredFields());
        }
        return fields;
    }

    /** 이름으로 필드 1개 탐색. 없으면 null. */
    private static Field findField(Class<?> cls, String name) {
        for (Field f : allFields(cls)) {
            if (f.getName().equals(name)) return f;
        }
        return null;
    }

    /**
     * JSON 직렬화 + 컬럼 최대 byte 길이(UTF-8 기준) 안전 truncate.
     * char 단위 substring 은 한글이 들어가면 byte 길이를 초과해 insert 실패하므로 byte 기준으로 자른다.
     */
    private static String toJson(Object o) {
        try {
            String s = MAPPER.writeValueAsString(o);
            byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
            if (bytes.length <= MAX_JSON_BYTES) return s;
            // byte 기준 안전 truncate (UTF-8 멀티바이트 중간 잘림 방지)
            int safe = MAX_JSON_BYTES - 3; // for "..."
            // safe 위치가 multi-byte char 중간이면 한 칸 뒤로
            while (safe > 0 && (bytes[safe] & 0xC0) == 0x80) {
                safe--;
            }
            return new String(bytes, 0, safe, StandardCharsets.UTF_8) + "...";
        } catch (Throwable t) {
            return String.valueOf(o);
        }
    }

    /** DELETE audit log 빌드. before = entity 직렬화, after 는 null(삭제 후 상태 없음). */
    private TbWcsAuditLog buildDelete(Object entity, String reason, String caller, ActorSnapshot actor) {
        try {
            if (ValueUtil.isEmpty(entity)) return null;
            TbWcsAuditLog log = baseLog(entity, "DELETE", reason, caller, actor);
            log.setBeforeJson(toJson(entity));
            return log;
        } catch (Throwable t) {
            logger.warn("[ Audit ][ Worker ] buildDelete failed", t);
            return null;
        }
    }

    /** SQL 선두에서 테이블명을 best-effort 로 추출(insert into|update|delete from 다음 토큰). */
    private static final Pattern TABLE_PTN = Pattern.compile(
            "(?i)(?:insert\\s+into|update|delete\\s+from)\\s+([`\"]?)([A-Za-z0-9_.]+)\\1");

    /** 문장 쓰기 진입점. SQL/params/affected 를 한 건으로 기록(row diff 없음). */
    void afterStatement(String action, String statement, Map<?, ?> params, int affected,
                        String reason, String caller) {
        final ActorSnapshot actor = ActorSnapshot.now();
        submit(() -> persist(buildStatement(action, statement, params, affected, reason, caller, actor)));
    }

    /** 문장 감사 로그 빌드. sql/params/affected 는 afterJson 한 칸에 보관(신규 칼럼 없음). */
    private TbWcsAuditLog buildStatement(String action, String statement, Map<?, ?> params,
                                         int affected, String reason, String caller, ActorSnapshot actor) {
        try {
            TbWcsAuditLog log = new TbWcsAuditLog();
            log.setAction(action);
            log.setTableName(parseTable(statement));
            log.setCaller(caller);
            log.setReason(reason);
            log.setActorType(actor.type.name());
            log.setActorId(actor.id);
            log.setActorName(actor.name);
            log.setChannel(actor.channel);
            log.setCreatorId(actor.id);
            log.setUpdaterId(actor.id);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sql", statement);
            payload.put("params", params);
            payload.put("affected", affected);
            log.setAfterJson(toJson(payload));
            return log;
        } catch (Throwable t) {
            logger.warn("[ Audit ][ Worker ] buildStatement failed", t);
            return null;
        }
    }

    /** insert into|update|delete from 다음 토큰을 테이블명으로 추출. 실패 시 null. */
    private static String parseTable(String sql) {
        if (ValueUtil.isEmpty(sql)) return null;
        Matcher m = TABLE_PTN.matcher(sql);
        return m.find() ? m.group(2) : null;
    }

    /** 행위자 스냅샷. 호출 스레드에서 캡처해 비동기 task 로 전달한다. */
    private static final class ActorSnapshot {
        final ActorType type;
        final String id;
        final String name;
        final String channel;

        ActorSnapshot(ActorType type, String id, String name, String channel) {
            this.type = type;
            this.id = id;
            this.name = name;
            this.channel = channel;
        }

        /** ActorContext → 인증 User → SYSTEM 순으로 행위자 해석. */
        static ActorSnapshot now() {
            ActorContext.Actor actor = ActorContext.get();
            if (actor != null) {
                return new ActorSnapshot(actor.type(), actor.id(), actor.name(), actor.channel());
            }
            try {
                User u = User.currentUser();
                if (ValueUtil.isNotEmpty(u) && ValueUtil.isNotEmpty(u.getId())) {
                    return new ActorSnapshot(ActorType.USER, u.getId(), u.getName(), ActorContext.CH_HTTP_UI);
                }
            } catch (Throwable ignore) {
            }
            return new ActorSnapshot(ActorType.SYSTEM, "SYSTEM", "System", null);
        }
    }
}