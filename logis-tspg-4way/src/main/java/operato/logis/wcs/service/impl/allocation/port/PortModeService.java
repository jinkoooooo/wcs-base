package operato.logis.wcs.service.impl.allocation.port;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.LocType;
import operato.logis.wcs.consts.PortMode;
import operato.logis.wcs.consts.WcsError;
import operato.logis.wcs.entity.ExtTbInventoryLocation;
import operato.logis.wcs.service.impl.event.RealtimeEventPublisher;
import operato.logis.wcs.service.repository.InventoryLocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 포트 모드 전환 서비스 (Drain & Switch).
 *
 * 전환 흐름:
 *   INBOUND  --(SWITCHING_TO_OUTBOUND)--> OUTBOUND
 *   OUTBOUND --(SWITCHING_TO_INBOUND)---> INBOUND
 *
 * 진행중 작업 0건이면 즉시 전환, 있으면 SWITCHING_* 상태로 드레인 시작.
 * runPortModeAutoSwitch() 가 진행중 작업이 빠지는 순간 자동 완료한다.
 *
 * 락 UPDATE 와 검색 등 나머지 포트 책임은 PortService 에 위치.
 */
@Service
@RequiredArgsConstructor
public class PortModeService extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(PortModeService.class);

    // 활성으로 간주하는 ECS IF 상태 - SENDING(10), SENT(20), ACK(30)
    private static final List<Integer> ACTIVE_ECS_IF_STATUSES = List.of(10, 20, 30);

    // 종료되지 않은 셔틀 오더 상태 임계 - 90 미만 = 진행중
    private static final int FINAL_STATUS_THRESHOLD = 90;

    private final InventoryLocationRepository locationRepository;

    // Optional - STOMP 브로커 부재 환경(테스트/임베디드)에서도 동작해야 함
    @Autowired(required = false)
    private RealtimeEventPublisher eventPublisher;

    /**
     * 운영자 요청 진입점. SWITCHING_* 는 시스템 내부 상태이므로 직접 지정 불가.
     * 동일 모드 요청은 fail, SWITCHING 중 같은 목표는 idempotent, 반대 목표는 드레인 취소.
     */
    @Transactional(rollbackFor = Exception.class)
    public PortService.ChangeResult changePortMode(String eqGroupId, String portCode,
                                                   PortMode newMode, String operator, String reason) {

        // 입력값 검증
        if (ValueUtil.isEmpty(newMode)) return PortService.ChangeResult.fail(WcsError.INVALID_PARAMETER, "newMode is required");
        if (ValueUtil.isEmpty(portCode)) return PortService.ChangeResult.fail(WcsError.INVALID_PARAMETER, "portCode is required");

        // SWITCHING_* 는 시스템 내부 모드라 직접 지정 거부
        if (newMode.isDraining()) {
            return PortService.ChangeResult.fail(WcsError.INVALID_PORT_MODE_CHANGE,
                    "SWITCHING_* is system-internal mode; choose INBOUND/OUTBOUND/IDLE instead");
        }

        // 대상 포트 조회 + 타입 검증
        ExtTbInventoryLocation port = locationRepository.findByEqGroupIdAndLocId(eqGroupId, portCode);
        if (ValueUtil.isEmpty(port)) return PortService.ChangeResult.fail(WcsError.ORDER_NOT_FOUND, "port not found: " + portCode);

        if (!LocType.IN_OUTBOUND_PORT.code().equalsIgnoreCase(port.getLocType())) {
            return PortService.ChangeResult.fail(WcsError.INVALID_PORT_MODE_CHANGE, "port is not switchable");
        }

        String prev = port.getPortMode();
        PortMode prevEnum = PortMode.from(prev);

        // 동일 모드 요청 거부
        if (newMode.code().equalsIgnoreCase(prev)) {
            return PortService.ChangeResult.fail(WcsError.INVALID_PORT_MODE_CHANGE,
                    "already in " + newMode.code() + " mode");
        }

        // 같은 방향으로 드레인 중이면 idempotent (no-op)
        if (ValueUtil.isNotEmpty(prevEnum) && prevEnum.isDraining() && prevEnum.pendingTarget() == newMode) {
            return PortService.ChangeResult.ok(prev, prev);
        }

        // 반대 방향 드레인 중이면 드레인 취소 (원래 모드로 복귀)
        if (ValueUtil.isNotEmpty(prevEnum) && prevEnum.isDraining() && prevEnum.drainingFrom() == newMode) {
            String restoreMode = prevEnum.drainingFrom().code();
            applyPortMode(eqGroupId, portCode, restoreMode);
            logger.warn("[ Allocation ][ Port ] drain cancelled on {} - {} -> {} by {}, reason={}",
                    portCode, prev, restoreMode, operator, reason);
            return PortService.ChangeResult.ok(prev, restoreMode);
        }

        // dispatch lock 걸려있으면 거부
        if (ValueUtil.isNotEmpty(port.getTaskId()) && !port.getTaskId().isBlank()) {
            return PortService.ChangeResult.fail(WcsError.INVALID_PORT_MODE_CHANGE,
                    "port is locked (dispatch_lock): " + port.getTaskId());
        }

        // 진행중 작업 0건이면 즉시 전환
        int activeCount = countActiveOrdersForMode(eqGroupId, portCode, prev);
        if (activeCount == 0) {
            applyPortMode(eqGroupId, portCode, newMode.code());
            logger.warn("[ Allocation ][ Port ] mode immediate {} -> {} on {} by {}, reason={}",
                    prev, newMode.code(), portCode, operator, reason);
            if (ValueUtil.isNotEmpty(eventPublisher)) {
                eventPublisher.publishPortModeChanged(eqGroupId, portCode, prev, newMode.code(), operator);
            }
            return PortService.ChangeResult.ok(prev, newMode.code());
        }

        // 진행중 작업 있으면 SWITCHING_* 로 드레인 시작
        PortMode switching = (newMode == PortMode.OUTBOUND) ? PortMode.SWITCHING_TO_OUTBOUND
                : (newMode == PortMode.INBOUND) ? PortMode.SWITCHING_TO_INBOUND
                : null;
        if (ValueUtil.isEmpty(switching)) {
            return PortService.ChangeResult.fail(WcsError.INVALID_PORT_MODE_CHANGE,
                    String.format("active orders exist (%d). Cancel running jobs or wait for completion.", activeCount));
        }

        applyPortMode(eqGroupId, portCode, switching.code());
        logger.warn("[ Allocation ][ Port ] drain start {} -> {} (target={}, activeOrders={}) on {} by {}, reason={}",
                prev, switching.code(), newMode.code(), activeCount, portCode, operator, reason);
        if (ValueUtil.isNotEmpty(eventPublisher)) {
            eventPublisher.publishPortModeChanged(eqGroupId, portCode, prev, switching.code(), operator);
        }
        return PortService.ChangeResult.ok(prev, switching.code());
    }

    /**
     * SWITCHING_* 포트 중 진행중 작업 0건이면 목표 모드로 자동 전환한다.
     * 스케줄러 진입점.
     */
    @Transactional(rollbackFor = Exception.class)
    public int runPortModeAutoSwitch() {
        String selectSql = """
                SELECT loc_group, loc_id, port_mode
                  FROM tb_inventory_location
                 WHERE port_mode IN ('SWITCHING_TO_INBOUND', 'SWITCHING_TO_OUTBOUND')
                   AND loc_type = 'IN_OUTBOUND_PORT'
                   AND is_enabled = true
                """;

        // 드레인 중 포트 조회
        List<Map> rows = this.queryManager.selectListBySql(
                selectSql, Collections.emptyMap(), Map.class, 0, 0);
        if (ValueUtil.isEmpty(rows)) return 0;

        int switched = 0;
        for (Map row : rows) {
            String eqGroupId = (String) row.get("loc_group");
            String locId = (String) row.get("loc_id");
            String currentMode = (String) row.get("port_mode");

            // 드레인 상태 검증
            PortMode currentEnum = PortMode.from(currentMode);
            if (ValueUtil.isEmpty(currentEnum) || !currentEnum.isDraining()) continue;

            // 드레인 진행 방향의 활성 오더가 남아있으면 대기
            String drainDirection = currentEnum.drainingFrom().code();
            int activeCount = countActiveOrdersForMode(eqGroupId, locId, drainDirection);
            if (activeCount > 0) {
                logger.debug("[ Allocation ][ Port ] auto switch waiting - port={}, drainFrom={}, active={}",
                        locId, drainDirection, activeCount);
                continue;
            }

            // 모두 빠졌으면 목표 모드로 전환
            String targetMode = currentEnum.pendingTarget().code();
            applyPortMode(eqGroupId, locId, targetMode);
            logger.warn("[ Allocation ][ Port ] drain complete on {} - {} -> {}", locId, currentMode, targetMode);
            if (ValueUtil.isNotEmpty(eventPublisher)) {
                eventPublisher.publishPortModeChanged(eqGroupId, locId, currentMode, targetMode, "AUTO_SWITCH");
            }
            switched++;
        }
        return switched;
    }

    /**
     * 포트의 현재 모드 방향 활성 오더 수.
     *
     * 모드별 조건:
     *   - INBOUND  : order_type='INBOUND'                  AND to_loc_code=portCode
     *   - OUTBOUND : order_type IN ('OUTBOUND','MOVE')     AND (from|to)=portCode
     *
     * 공통 필터: order_status < 90, ecs_if_status IN (10,20,30).
     */
    public int countActiveOrdersForMode(String eqGroupId, String portCode, String currentMode) {
        if (ValueUtil.isEmpty(currentMode)) return 0;

        // 모드별 필터 결정
        String orderTypeFilter;
        String locColumnFilter;
        switch (currentMode.toUpperCase()) {
            case "INBOUND" -> {
                orderTypeFilter = "order_type = 'INBOUND'";
                locColumnFilter = "to_loc_code = :portCode";
            }
            case "OUTBOUND", "OUTBOUND_PRIORITY" -> {
                orderTypeFilter = "order_type IN ('OUTBOUND', 'MOVE')";
                locColumnFilter = "(from_loc_code = :portCode OR to_loc_code = :portCode)";
            }
            default -> {
                return 0;
            }
        }

        // 활성 오더 카운트 쿼리
        String sql = String.format("""
                SELECT COUNT(*) AS cnt
                  FROM tb_wcs_shuttle_order
                 WHERE eq_group_id = :eqGroupId
                   AND %s
                   AND %s
                   AND order_status < %d
                   AND ecs_if_status IN (:activeStatuses)
                """, orderTypeFilter, locColumnFilter, FINAL_STATUS_THRESHOLD);

        Map<String, Object> params = ValueUtil.newMap(
                "eqGroupId,portCode,activeStatuses",
                eqGroupId, portCode, ACTIVE_ECS_IF_STATUSES);

        List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 1);
        if (ValueUtil.isEmpty(rows)) return 0;
        Object cnt = rows.get(0).get("cnt");
        return cnt instanceof Number n ? n.intValue() : 0;
    }

    // 포트의 port_mode 컬럼을 newModeCode 로 UPDATE
    private void applyPortMode(String eqGroupId, String portCode, String newModeCode) {
        String sql = """
                UPDATE tb_inventory_location SET port_mode = :m
                 WHERE loc_group = :eg AND loc_id = :pc
                """;
        int updated = this.queryManager.executeBySql(
                sql, ValueUtil.newMap("m,eg,pc", newModeCode, eqGroupId, portCode));
        if (updated <= 0) {
            logger.warn("[ Allocation ][ Port ] apply mode affected 0 rows - eg={}, pc={}, mode={}",
                    eqGroupId, portCode, newModeCode);
        }
    }
}
