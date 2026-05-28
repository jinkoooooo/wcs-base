package operato.logis.wcs.common.service.audit;

import operato.logis.wcs.entity.TbWcsAuditLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.util.ValueUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DB 작업을 가로채는 프록시 핸들러.
 * 읽기(조회)는 그냥 통과시키고, 쓰기(CUD)만 가로채 감사 워커(AuditWorker)로 넘긴다.
 * 가짜 프록시가 아닌 진짜 객체(delegate)를 직접 호출해 무한 재진입을 막는다.
 *
 * 메인 스레드에서는 Before/After 객체를 '캡처'하는 일만 하고,
 * 무거운 직렬화나 diff 계산은 모두 워커의 비동기 스레드로 미룬다.
 * 단, Before 조회는 덮어쓰기 전에 끝나야 하므로 메인 스레드에서 '벌크 1회'로 모아 가져온다.
 */
class AuditingInvocationHandler implements InvocationHandler {

    private static final Logger logger = LoggerFactory.getLogger(AuditingInvocationHandler.class);

    /** 감사 로그를 남길 쓰기 메서드 이름 목록 */
    private static final Set<String> ENTITY_WRITES = Set.of(
            "insert", "insertBatch", "update", "updateBatch",
            "upsert", "upsertBatch", "delete", "deleteBatch",
            "deleteByCondition", "deleteList");

    private final IQueryManager delegate; // 진짜 DB 실행 객체
    private final AuditWorker worker;     // 감사 로그 기록 작업자

    /** 분석한 파라미터를 한 형태로 모아 둔다 (엔티티 클래스, 데이터 목록, 변경 컬럼) */
    private record Targets(Class<?> entityClass, List<Object> entities, String[] columns) {}

    AuditingInvocationHandler(IQueryManager delegate, AuditWorker worker) {
        this.delegate = delegate;
        this.worker = worker;
    }

    /** 프록시 진입점. 모든 DB 요청이 가장 먼저 도착하는 곳이며, 메서드 이름으로 분기한다. */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();

        // 조회면 로그 없이 바로 통과
        if (!isWrite(name)) {
            return invokeDelegate(method, args);
        }
        // SQL 문장 직접 실행이면 문장 단위로 기록
        if (name.startsWith("execute") || name.startsWith("callProcedure")) {
            return auditStatement(method, args);
        }
        // 일반 엔티티 쓰기(Insert/Update/Delete)
        return auditEntityWrite(name, method, args);
    }

    /** 쓰기 작업인지 판별한다. */
    private boolean isWrite(String name) {
        return ENTITY_WRITES.contains(name)
                || name.startsWith("execute") || name.startsWith("callProcedure");
    }

    /**
     * 진짜 DB 쿼리를 실행한다.
     * 프록시가 덧씌우는 예외(InvocationTargetException)를 벗겨 원래 예외를 그대로 던져,
     * 비즈니스 트랜잭션의 롤백 동작을 깨뜨리지 않는다.
     */
    private Object invokeDelegate(Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(delegate, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    /** 순수 SQL을 실행한 뒤, 문장·파라미터·영향 건수를 워커로 넘긴다. */
    private Object auditStatement(Method method, Object[] args) throws Throwable {
        Object result = invokeDelegate(method, args); // DB 먼저 실행
        safeAudit(() -> {
            String statement = firstString(args);
            Map<?, ?> params = firstMap(args);
            int affected = (result instanceof Integer i) ? i : -1;
            worker.afterStatement("EXEC", statement, params, affected, AuditReason.get(), caller());
        });
        return result;
    }

    /**
     * 엔티티 쓰기 메인 로직.
     * Before 확보(UPDATE/UPSERT만) -> DB 실행 -> After 캡처 후 워커 위임 순서로 진행한다.
     * 메인 스레드의 일은 '객체 참조를 모으는 것'까지이며, 직렬화·diff는 워커가 맡는다.
     */
    private Object auditEntityWrite(String name, Method method, Object[] args) throws Throwable {
        // 파라미터를 일정한 형태로 정리한다. 정리에 실패하면 로그를 포기하고 DB만 실행한다.
        Targets t;
        try {
            t = resolveTargets(name, args);
        } catch (Throwable ex) {
            logger.warn("[ Audit ][ Proxy ] resolveTargets failed - method={}", name, ex);
            return invokeDelegate(method, args);
        }

        String action = actionOf(name);
        String reason = AuditReason.get();
        String caller = caller();

        // DELETE: 삭제 전 데이터는 resolveDeleteTargets 단계에서 이미 확보해 두었다.
        // DB 실행 후, 감사 대상 객체 참조만 추려 한 번에 워커로 넘긴다.
        if ("DELETE".equals(action)) {
            List<Object> deletes = new ArrayList<>();
            for (Object b : t.entities()) {
                if (auditable(b)) deletes.add(b);
            }
            Object result = invokeDelegate(method, args);
            safeAudit(() -> worker.afterDeleteBatch(deletes, reason, caller));
            return result;
        }

        // INSERT: Before가 없으므로 After 참조만 캡처해 위임한다.
        if ("INSERT".equals(action)) {
            Object result = invokeDelegate(method, args);
            safeAudit(() -> {
                List<Object> inserted = new ArrayList<>();
                for (Object a : t.entities()) {
                    if (auditable(a)) inserted.add(a);
                }
                worker.afterInsertBatch(inserted, reason, caller);
            });
            return result;
        }

        // UPDATE/UPSERT: 덮어쓰기 전에 기존 Before를 '벌크 1회'로 모아 가져온다. (N+1 제거)
        // beforeByPk: PK -> Before 엔티티. 다건이면 IN 한 방, 단건이면 단건 조회로 끝난다.
        Map<Object, Object> beforeByPk = loadBeforesInBulk(t.entityClass(), t.entities());

        // 실제 DB 업데이트 실행
        Object result = invokeDelegate(method, args);

        // After 참조와 매칭된 Before 참조를 [before, after] 쌍으로 묶어 워커로 넘긴다.
        // 여기까지가 메인 스레드의 일이고, 이후 diff/직렬화는 모두 워커의 비동기 스레드에서 처리된다.
        final String act = action;
        safeAudit(() -> {
            List<Object[]> pairs = new ArrayList<>(t.entities().size()); // 각 원소: [before, after]
            for (Object after : t.entities()) {
                if (!auditable(after)) continue;
                Object pk = AuditWorker.pkKeyOf(after);
                Object before = (pk != null) ? beforeByPk.get(pk) : null;
                pairs.add(new Object[]{before, after});
            }
            worker.afterUpdateBatch(act, pairs, t.columns(), reason, caller);
        });
        return result;
    }

    /**
     * Before 엔티티를 PK 벌크 IN 조회 한 번으로 확보한다. (기존 건건 loadBefore 루프 대체)
     * 다건이면 PK 컬렉션으로 한 방에 SELECT, 단건이면 단건 조회로 끝낸다.
     * 어떤 이유로든 실패하면 빈 맵을 돌려주고, 해당 row는 Before 없이 After만 기록한다(안전 폴백).
     */
    private Map<Object, Object> loadBeforesInBulk(Class<?> cls, List<Object> entities) {
        Map<Object, Object> map = new LinkedHashMap<>();
        if (cls == null || ValueUtil.isEmpty(entities)) return map;

        try {
            // 단일 PK 컬럼명을 찾는다. 복합키거나 못 찾으면 단건 조회로 폴백한다.
            String pkField = AuditWorker.singlePkFieldNameOf(cls);

            if (ValueUtil.isNotEmpty(pkField) && entities.size() > 1) {
                // PK 값 목록 수집
                List<Object> pkValues = new ArrayList<>(entities.size());
                for (Object e : entities) {
                    Object pk = AuditWorker.pkKeyOf(e);
                    if (pk != null) pkValues.add(pk);
                }
                if (!pkValues.isEmpty()) {
                    // IN 절 한 번으로 Before 목록을 가져온다.
                    List<?> befores = worker.selectByPk(cls, pkField, pkValues);
                    for (Object b : befores) {
                        Object pk = AuditWorker.pkKeyOf(b);
                        if (pk != null) map.put(pk, b);
                    }
                    return map;
                }
            }

            // 단건이거나 벌크 조건이 안 맞으면 단건 조회로 처리한다.
            for (Object e : entities) {
                Object before = loadBefore(cls, e);
                Object pk = AuditWorker.pkKeyOf(e);
                if (pk != null && ValueUtil.isNotEmpty(before)) {
                    map.put(pk, before);
                }
            }
        } catch (Throwable t) {
            logger.warn("[ Audit ][ Proxy ] loadBeforesInBulk failed - cls={}",
                    cls == null ? "?" : cls.getSimpleName(), t);
        }
        return map;
    }

    /** 메서드 이름으로 액션을 구분한다. */
    private String actionOf(String name) {
        if (name.startsWith("insert")) return "INSERT";
        if (name.startsWith("update")) return "UPDATE";
        if (name.startsWith("upsert")) return "UPSERT";
        return "DELETE";
    }

    /** 지저분한 인자(배열, List 등)를 통일된 Targets 형태로 규격화한다. */
    private Targets resolveTargets(String name, Object[] args) {
        if ("DELETE".equals(actionOf(name))) {
            return resolveDeleteTargets(name, args);
        }
        String[] columns = (args.length > 0 && args[args.length - 1] instanceof String[] sa) ? sa : null;
        Class<?> entityClass = null;
        int idx = 0;
        if (args[0] instanceof Class<?> c) {
            entityClass = c;
            idx = 1;
        } else if (args[0] instanceof String s) {
            entityClass = delegate.getClass(s);
            idx = 1;
        }
        List<Object> entities = toEntityList(args[idx]);
        if (entityClass == null && !entities.isEmpty()) {
            entityClass = entities.get(0).getClass();
        }
        return new Targets(entityClass, entities, columns);
    }

    /** 삭제 전용. 사라지기 전의 데이터를 미리 DB에서 긁어 온다. */
    private Targets resolveDeleteTargets(String name, Object[] args) {
        // deleteBatch
        if (name.equals("deleteBatch")) {
            List<Object> entities = toEntityList(args[args.length - 1]);
            Class<?> cls = entities.isEmpty() ? null : entities.get(0).getClass();
            return new Targets(cls, entities, null);
        }
        // deleteList / deleteByCondition
        if (name.equals("deleteList") || name.equals("deleteByCondition")) {
            Class<?> cls = classOf(args[0]);
            List<Object> before = (cls == null)
                    ? new ArrayList<>()
                    : new ArrayList<>(delegate.selectList(cls, args[1]));
            return new Targets(cls, before, null);
        }
        // 단건 delete(Object)
        if (args.length == 1 && !(args[0] instanceof Class<?>) && !(args[0] instanceof String)) {
            Object data = args[0];
            Object before = loadBefore(data.getClass(), data);
            return new Targets(data.getClass(),
                    toEntityList(ValueUtil.isNotEmpty(before) ? before : data), null);
        }
        // PK로 delete
        Class<?> cls = classOf(args[0]);
        Object pk = args[1];
        Object before = null;
        try {
            before = (pk instanceof Object[] arr) ? delegate.select(cls, arr) : delegate.select(cls, pk);
        } catch (Throwable ignore) {
        }
        return new Targets(cls, toEntityList(before), null);
    }

    /** 인자가 클래스인지 테이블명인지 확인해 클래스로 변환한다. */
    private Class<?> classOf(Object arg) {
        if (arg instanceof Class<?> c) return c;
        if (arg instanceof String s) return delegate.getClass(s);
        return null;
    }

    /** 단일 객체든 List든 무조건 List 형태로 맞춘다. */
    private List<Object> toEntityList(Object payload) {
        if (payload instanceof List<?> list) return new ArrayList<>(list);
        List<Object> one = new ArrayList<>(1);
        if (ValueUtil.isNotEmpty(payload)) one.add(payload);
        return one;
    }

    /** PK로 변경 전 데이터(Before)를 1건 조회한다. 벌크가 안 될 때의 폴백 경로다. */
    private Object loadBefore(Class<?> cls, Object after) {
        try {
            Object[] pk = AuditWorker.pkValuesOf(after);
            if (cls == null || pk.length == 0) return null;
            return delegate.select(cls, pk); // 진짜 실행기로 조회
        } catch (Throwable t) {
            return null;
        }
    }

    /** 감사 대상인지 판별한다. 로그 테이블 자신은 제외해 무한 루프를 막는다. */
    private boolean auditable(Object o) {
        return ValueUtil.isNotEmpty(o) && !(o instanceof TbWcsAuditLog);
    }

    /**
     * 메인 비즈니스 로직을 지키는 보호망.
     * 로그를 남기다 에러가 나도 메인 트랜잭션이 멈추지 않도록 예외를 삼킨다.
     */
    private void safeAudit(Runnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            logger.warn("[ Audit ][ Proxy ] audit emit failed", t);
        }
    }

    /** 인자 중 첫 번째 문자열을 꺼낸다. */
    private static String firstString(Object[] args) {
        if (args == null) return null;
        for (Object a : args) {
            if (a instanceof String s) return s;
        }
        return null;
    }

    /** 인자 중 첫 번째 Map을 꺼낸다. */
    private static Map<?, ?> firstMap(Object[] args) {
        if (args == null) return null;
        for (Object a : args) {
            if (a instanceof Map<?, ?> m) return m;
        }
        return null;
    }

    /**
     * 이 DB 쿼리를 실제로 호출한 비즈니스 메서드를 찾는다.
     * 스프링·JDK 프록시 같은 군더더기 스택은 건너뛰고 진짜 호출자를 골라낸다.
     */
    private static String caller() {
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        for (int i = 2; i < st.length; i++) {
            String cn = st[i].getClassName();
            if (cn.equals(AuditingInvocationHandler.class.getName())) continue;
            if (cn.startsWith("jdk.proxy") || cn.startsWith("com.sun.proxy")) continue;
            if (cn.startsWith("java.") || cn.startsWith("jdk.") || cn.startsWith("sun.")
                    || cn.startsWith("org.springframework.") || cn.contains("$$")) continue;
            int dot = cn.lastIndexOf('.');
            return (dot < 0 ? cn : cn.substring(dot + 1)) + "." + st[i].getMethodName();
        }
        return "?";
    }
}