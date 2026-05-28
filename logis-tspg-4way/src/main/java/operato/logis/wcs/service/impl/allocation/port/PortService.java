package operato.logis.wcs.service.impl.allocation.port;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.common.service.audit.ActorContext;
import operato.logis.wcs.consts.PortMode;
import operato.logis.wcs.consts.WcsError;
import operato.logis.wcs.service.impl.event.RealtimeEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 포트 도메인 진입점 - 배차 락 + 검색.
 *
 * 책임:
 *   - tryLockForDispatch / unlock / unlockByHost / forceUnlock / isLocked / sweepStalePortLocks
 *   - listPorts / listLocks (READ)
 *   - 모드 전환은 PortModeService 로 위임
 */
@Service
@RequiredArgsConstructor
public class PortService extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(PortService.class);

    // 포트 타입 - SQL 리터럴
    private static final String PORT_LOC_TYPES_LITERAL =
            "'INBOUND_PORT','OUTBOUND_PORT','IN_OUTBOUND_PORT'";

    private final PortModeService portModeService;

    // Optional - STOMP 브로커 부재 환경에서도 동작
    @Autowired(required = false)
    private RealtimeEventPublisher eventPublisher;

    // 락 보유 한도 (시간). 기본 6h - 운영 데이터 확보 후 합의해 조정
    @Value("${wcs.port-lock.max-hold-hours:6}")
    private int sweepMaxHoldHours;

    /**
     * 포트 락 UPDATE - 순수 실행. "걸어야 하는가" 판단은 PortLockPolicy 가 끝낸 상태로 호출된다.
     * 반환 false = 포트가 이미 다른 task 로 잠김 (동시성 선점). 이번 tick 만 skip.
     */
    public boolean tryLockForDispatch(String eqGroupId, String portCode, String parentHostKey) {
        String lockKey = ValueUtil.isEmpty(parentHostKey) ? "DISPATCH_LOCK" : parentHostKey;

        // 락 UPDATE - 빈 자리 또는 자기 키만 허용
        String sql = """
                UPDATE tb_inventory_location
                   SET task_id = :orderKey,
                       updated_at = NOW()
                 WHERE loc_group = :eqGroupId
                   AND loc_id  = :portCode
                   AND (task_id IS NULL OR task_id = '' OR task_id = :orderKey)
                """;
        int updated = this.queryManager.executeBySql(sql, ValueUtil.newMap(
                "orderKey,eqGroupId,portCode", lockKey, eqGroupId, portCode));

        boolean ok = updated == 1;
        if (!ok) {
            logger.debug("[ Allocation ][ Port ] lock preempted - port={}, eqGroupId={}, orderKey={}",
                    portCode, eqGroupId, parentHostKey);
        } else if (ValueUtil.isNotEmpty(eventPublisher)) {
            eventPublisher.publishPortLockAcquired(eqGroupId, portCode, parentHostKey);
        }
        return ok;
    }

    /**
     * 포트 락 해제 - orderKey 지정 시 해당 키와 일치할 때만 NULL.
     */
    public void unlock(String eqGroupId, String portCode, String orderKey) {
        if (ValueUtil.isEmpty(portCode)) return;

        // orderKey 유무에 따라 가드 SQL 추가
        String sql = """
                UPDATE tb_inventory_location
                   SET task_id = NULL,
                       updated_at = NOW()
                 WHERE loc_group = :eqGroupId
                   AND loc_id  = :portCode
                """ + (ValueUtil.isEmpty(orderKey) ? "" : "   AND task_id = :orderKey ");
        Map<String, Object> params = ValueUtil.isEmpty(orderKey)
                ? ValueUtil.newMap("eqGroupId,portCode", eqGroupId, portCode)
                : ValueUtil.newMap("eqGroupId,portCode,orderKey", eqGroupId, portCode, orderKey);

        int updated = this.queryManager.executeBySql(sql, params);
        logger.info("[ Allocation ][ Port ] unlock - port={}, eqGroupId={}, orderKey={}, updated={}",
                portCode, eqGroupId, orderKey, updated);
        if (updated > 0 && ValueUtil.isNotEmpty(eventPublisher)) {
            eventPublisher.publishPortLockReleased(eqGroupId, portCode, orderKey, false);
        }
    }

    /**
     * host 비교 가드 내장 unlock - scanningHostKey == task_id 인 경우만 NULL.
     * 다른 host 의 dispatch lock 은 보존. SQL 레벨 가드라 race condition 안전.
     * PortTrafficService.handleInboundEntry 의 self-host bypass 와 동일 규약.
     *
     * scanningHostKey 가 null/empty 면 0 rows update (모든 락 보존).
     */
    public void unlockByHost(String eqGroupId, String portCode, String scanningHostKey) {
        if (ValueUtil.isEmpty(portCode) || ValueUtil.isEmpty(scanningHostKey)) return;

        String sql = """
                UPDATE tb_inventory_location
                   SET task_id = NULL,
                       updated_at = NOW()
                 WHERE loc_group = :eqGroupId
                   AND loc_id   = :portCode
                   AND task_id  = :hostKey
                """;
        Map<String, Object> params = ValueUtil.newMap(
                "eqGroupId,portCode,hostKey", eqGroupId, portCode, scanningHostKey);

        int updated = this.queryManager.executeBySql(sql, params);
        logger.info("[ Allocation ][ Port ] unlockByHost - port={}, eqGroupId={}, hostKey={}, updated={}",
                portCode, eqGroupId, scanningHostKey, updated);
        if (updated > 0 && ValueUtil.isNotEmpty(eventPublisher)) {
            eventPublisher.publishPortLockReleased(eqGroupId, portCode, scanningHostKey, false);
        }
    }

    /**
     * 운영자 강제 해제 - 가드 없이 NULL.
     */
    public void forceUnlock(String eqGroupId, String portCode, String operator, String reason) {
        String sql = """
                UPDATE tb_inventory_location
                   SET task_id = NULL,
                       updated_at = NOW()
                 WHERE loc_group = :eqGroupId
                   AND loc_id  = :portCode
                """;
        int updated = this.queryManager.executeBySql(sql,
                ValueUtil.newMap("eqGroupId,portCode", eqGroupId, portCode));
        logger.warn("[ Allocation ][ Port ] FORCE UNLOCK - port={}, eqGroupId={}, operator={}, reason={}, updated={}",
                portCode, eqGroupId, operator, reason, updated);
        if (updated > 0 && ValueUtil.isNotEmpty(eventPublisher)) {
            eventPublisher.publishPortLockReleased(eqGroupId, portCode, operator, true);
        }
    }

    /**
     * 락 보유 시간이 wcs.port-lock.max-hold-hours 를 초과한 포트를 강제 해제한다.
     *
     * tb_inventory_location.updated_at 이 락 획득 시 함께 갱신되는 점을 이용해
     * 별도 locked_at 컬럼 없이 안전망 구현. forceUnlock 의 logger.warn 이 알림 채널.
     */
    @Scheduled(fixedDelayString = "${wcs.port-lock.sweep-interval-ms:300000}")
    public void sweepStalePortLocks() {
        // 스케줄러 발생 쓰기를 SCHEDULER 행위자로 감사 태깅
        ActorContext.set(ActorContext.scheduler("PortLockSweeper"));
        try {
            String sql = """
                    SELECT loc_group, loc_id, task_id
                      FROM tb_inventory_location
                     WHERE task_id IS NOT NULL AND task_id <> ''
                       AND loc_type IN (%s)
                       AND updated_at < NOW() - (:hours::text || ' hours')::interval
                    """.formatted(PORT_LOC_TYPES_LITERAL);

            // 보유 한도 초과 포트 조회
            List<Map> rows = this.queryManager.selectListBySql(
                    sql, ValueUtil.newMap("hours", sweepMaxHoldHours), Map.class, 0, 100);
            if (ValueUtil.isEmpty(rows)) return;

            // 일괄 강제 해제
            for (Map row : rows) {
                String eqGroupId = (String) row.get("loc_group");
                String portCode = (String) row.get("loc_id");
                forceUnlock(eqGroupId, portCode, "TIMEOUT_SWEEPER",
                        "held > " + sweepMaxHoldHours + "h");
            }
        } finally {
            ActorContext.clear();
        }
    }

    /**
     * 포트 락 여부 조회 - task_id 가 비어있지 않으면 잠긴 것.
     */
    public boolean isLocked(String eqGroupId, String portCode) {
        String sql = """
                SELECT task_id FROM tb_inventory_location
                 WHERE loc_group = :eqGroupId AND loc_id = :portCode
                """;
        List<Map> rows = this.queryManager.selectListBySql(
                sql, ValueUtil.newMap("eqGroupId,portCode", eqGroupId, portCode), Map.class, 0, 1);
        if (ValueUtil.isEmpty(rows)) return false;
        Object taskId = rows.get(0).get("task_id");
        return ValueUtil.isNotEmpty(taskId) && !String.valueOf(taskId).isBlank();
    }

    /**
     * 모드 전환 - PortModeService 위임.
     */
    public ChangeResult changePortMode(String eqGroupId, String portCode,
                                       PortMode newMode, String operator, String reason) {
        return portModeService.changePortMode(eqGroupId, portCode, newMode, operator, reason);
    }

    /**
     * 드레인 자동 완료 스케줄러 - PortModeService 위임.
     */
    public int runPortModeAutoSwitch() {
        return portModeService.runPortModeAutoSwitch();
    }

    /**
     * 모드 방향 활성 오더 카운트 - PortModeService 위임.
     */
    public int countActiveOrdersForMode(String eqGroupId, String portCode, String currentMode) {
        return portModeService.countActiveOrdersForMode(eqGroupId, portCode, currentMode);
    }

    /**
     * 포트 목록 조회 - 필터: locType / portMode / locked / keyword.
     */
    public List<Map<String, Object>> listPorts(String eqGroupId, String locType,
                                               String portMode, Boolean locked, String keyword) {
        StringBuilder sql = new StringBuilder("""
                SELECT loc_group, loc_id, loc_type, port_mode, task_id, is_enabled, active_task_count
                  FROM tb_inventory_location
                 WHERE loc_type IN (%s)
                """.formatted(PORT_LOC_TYPES_LITERAL));

        // 동적 필터 구성
        Map<String, Object> params = new LinkedHashMap<>();
        if (ValueUtil.isNotEmpty(eqGroupId)) { sql.append(" AND loc_group = :eqGroupId "); params.put("eqGroupId", eqGroupId); }
        if (ValueUtil.isNotEmpty(locType))   { sql.append(" AND loc_type  = :locType ");   params.put("locType", locType); }
        if (ValueUtil.isNotEmpty(portMode))  { sql.append(" AND port_mode = :portMode ");  params.put("portMode", portMode); }
        if (ValueUtil.isNotEmpty(locked)) {
            sql.append(locked ? " AND task_id IS NOT NULL AND task_id <> '' "
                    : " AND (task_id IS NULL OR task_id = '') ");
        }
        if (ValueUtil.isNotEmpty(keyword)) { sql.append(" AND loc_id LIKE :kw "); params.put("kw", "%" + keyword + "%"); }
        sql.append(" ORDER BY loc_group, loc_id ");

        List<Map> rows = this.queryManager.selectListBySql(sql.toString(), params, Map.class, 0, 500);
        return rows.stream().map(r -> (Map<String, Object>) r).toList();
    }

    /**
     * 락이 걸린 포트 목록 - updated_at 오름차순.
     */
    public List<Map<String, Object>> listLocks(String eqGroupId) {
        StringBuilder sql = new StringBuilder("""
                SELECT loc_group, loc_id, loc_type, task_id, updated_at
                  FROM tb_inventory_location
                 WHERE loc_type IN (%s)
                   AND task_id IS NOT NULL AND task_id <> ''
                """.formatted(PORT_LOC_TYPES_LITERAL));
        Map<String, Object> params = new LinkedHashMap<>();
        if (ValueUtil.isNotEmpty(eqGroupId)) { sql.append(" AND loc_group = :eqGroupId "); params.put("eqGroupId", eqGroupId); }
        sql.append(" ORDER BY updated_at ASC ");

        List<Map> rows = this.queryManager.selectListBySql(sql.toString(), params, Map.class, 0, 500);
        return rows.stream().map(r -> (Map<String, Object>) r).toList();
    }

    /**
     * 모드 전환 결과 - 외부 API 호환을 위해 PortService 에 유지.
     */
    public record ChangeResult(boolean success, String previousMode, String currentMode,
                               String errorCode, String errorDesc) {
        public static ChangeResult ok(String prev, String cur) {
            return new ChangeResult(true, prev, cur, null, null);
        }
        public static ChangeResult fail(WcsError e, String desc) {
            return new ChangeResult(false, null, null, e.codeAsString(), desc);
        }
    }
}
