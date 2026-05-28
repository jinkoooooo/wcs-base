package operato.logis.wcs.service.impl.allocation.port;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.LocType;
import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.consts.PortMode;
import operato.logis.wcs.consts.ShuttleOrderStatus;
import operato.logis.wcs.entity.ExtTbInventoryLocation;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.service.repository.InventoryLocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.Map;

import static operato.logis.wcs.common.util.check.Validator.hasLocationKey;

/**
 * 포트 트래픽 서비스 - 입고 진입 이벤트, active_task_count 증감, 포트 강제 우회.
 * As-Is WcsPortTrafficController 의 책임을 보존하되 컨트롤러가 아닌 서비스로 명명.
 */
@Service
@RequiredArgsConstructor
public class PortTrafficService extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(PortTrafficService.class);

    private final InventoryLocationRepository locationRepository;

    /**
     * 입고 파렛트가 포트에 진입했을 때 호출.
     *
     * 겸용 포트는 INBOUND 모드로 전환하여 출고 할당 차단.
     * OUTBOUND_PRIORITY 모드면 진입 거부.
     * 자기 host 의 재입고(task_id == scanningHostKey)는 mode flip 을 skip - sibling chain 정지 방지.
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean handleInboundEntry(String eqGroupId, String portCode, String scanningHostKey) {

        // 입력값 검증
        if (!hasLocationKey(eqGroupId, portCode)) {
            logger.warn("[ Allocation ][ Port ] inbound entry skip - params missing. eqGroupId={}, portCode={}",
                    eqGroupId, portCode);
            return false;
        }

        // 포트 조회
        ExtTbInventoryLocation port = locationRepository.findByEqGroupIdAndLocId(eqGroupId, portCode);
        if (ValueUtil.isEmpty(port)) {
            logger.warn("[ Allocation ][ Port ] inbound entry skip - port not found. portCode={}", portCode);
            return false;
        }

        // 전용 포트는 그대로 진입 허용
        boolean isSharedPort = LocType.IN_OUTBOUND_PORT.code().equalsIgnoreCase(port.getLocType());
        if (!isSharedPort) {
            logger.info("[ Allocation ][ Port ] inbound entry dedicated - portCode={}", portCode);
            return true;
        }

        // self-host bypass - 같은 host 의 sibling/재입고는 동일 lock key 공유 (PortService.tryLockForDispatch 규약)
        // task_id == scanningHostKey 면 mode flip skip + 진입만 허용 → sibling chain 의 다음 출고가 dispatch 가능
        if (ValueUtil.isNotEmpty(scanningHostKey)
                && ValueUtil.isNotEmpty(port.getTaskId())
                && port.getTaskId().equals(scanningHostKey)) {
            logger.info("[ Allocation ][ Port ] inbound entry self-host - port={}, host={}",
                    portCode, scanningHostKey);
            return true;
        }

        // 출고 우선 모드면 진입 거부
        if (PortMode.OUTBOUND_PRIORITY.code().equalsIgnoreCase(port.getPortMode())) {
            logger.warn("[ Allocation ][ Port ] inbound entry rejected - OUTBOUND_PRIORITY. portCode={}", portCode);
            return false;
        }

        // 겸용 포트 → INBOUND 모드로 전환 (OUTBOUND_PRIORITY 아닐 때만)
        String sql = """
                UPDATE tb_inventory_location
                   SET port_mode = :inboundMode
                 WHERE loc_group = :locGroup AND loc_id = :locId
                   AND port_mode != :priorityMode
                """;
        this.queryManager.executeBySql(sql, ValueUtil.newMap(
                "inboundMode,locGroup,locId,priorityMode",
                PortMode.INBOUND.code(), eqGroupId, portCode,
                PortMode.OUTBOUND_PRIORITY.code()));

        logger.info("[ Allocation ][ Port ] inbound entry mode flipped to INBOUND - portCode={}", portCode);
        return true;
    }

    /**
     * 출고 오더를 WAITING(25) 으로 보류한다.
     * OutboundPortAllocator 가 적합 포트를 찾지 못할 때 호출.
     */
    @Transactional(rollbackFor = Exception.class)
    public void addToWaitingQueue(String orderKey) {
        if (ValueUtil.isEmpty(orderKey)) return;

        // 현재 상태가 WAITING 미만일 때만 변경
        String sql = """
                UPDATE tb_wcs_shuttle_order SET order_status = :waitingStatus
                 WHERE order_key = :orderKey AND order_status < :waitingStatus
                """;
        Map<String, Object> params = ValueUtil.newMap("waitingStatus,orderKey",
                ShuttleOrderStatus.WAITING.codeAsIntOrNull(), orderKey);

        int updated = queryManager.executeBySql(sql, params);
        if (updated > 0) {
            logger.info("[ Allocation ][ Port ] outbound waiting enqueued - orderKey={}", orderKey);
        }
    }

    /**
     * 포트의 활성 작업 카운트 +1.
     */
    @Transactional(rollbackFor = Exception.class)
    public void incrementActiveTaskCount(String eqGroupId, String portCode) {
        if (!hasLocationKey(eqGroupId, portCode)) return;
        String sql = """
                UPDATE tb_inventory_location
                   SET active_task_count = COALESCE(active_task_count, 0) + 1
                 WHERE loc_group = :locGroup AND loc_id = :locId
                """;
        queryManager.executeBySql(sql, ValueUtil.newMap("locGroup,locId", eqGroupId, portCode));
    }

    /**
     * 포트의 활성 작업 카운트 -1 (음수 방지).
     */
    @Transactional(rollbackFor = Exception.class)
    public void decrementActiveTaskCount(String eqGroupId, String portCode) {
        if (!hasLocationKey(eqGroupId, portCode)) return;
        String sql = """
                UPDATE tb_inventory_location
                   SET active_task_count = GREATEST(0, COALESCE(active_task_count, 0) - 1)
                 WHERE loc_group = :locGroup AND loc_id = :locId
                """;
        queryManager.executeBySql(sql, ValueUtil.newMap("locGroup,locId", eqGroupId, portCode));
    }

    /**
     * 오더 기준으로 카운트 +1 (입고는 fromLoc, 출고는 toLoc).
     */
    @Transactional(rollbackFor = Exception.class)
    public void incrementByOrder(TbWcsShuttleOrder order) {
        if (ValueUtil.isEmpty(order)) return;
        String portCode = resolveRelatedPort(order);
        if (ValueUtil.isNotEmpty(portCode)) {
            incrementActiveTaskCount(order.getEqGroupId(), portCode);
            logger.info("[ Allocation ][ Port ] increment - orderKey={}, port={}", order.getOrderKey(), portCode);
        }
    }

    /**
     * 오더 기준으로 카운트 -1 (입고는 fromLoc, 출고는 toLoc).
     */
    @Transactional(rollbackFor = Exception.class)
    public void decrementByOrder(TbWcsShuttleOrder order) {
        if (ValueUtil.isEmpty(order)) return;
        String portCode = resolveRelatedPort(order);
        if (ValueUtil.isNotEmpty(portCode)) {
            decrementActiveTaskCount(order.getEqGroupId(), portCode);
            logger.info("[ Allocation ][ Port ] decrement - orderKey={}, port={}", order.getOrderKey(), portCode);
        }
    }

    /**
     * 고장 포트를 강제 우회 - DISABLED + active_task_count=0.
     * 해당 포트로 향하던 미완료 오더는 WAITING + to_loc_code=NULL 로 변경 (다음 폴링에서 재할당).
     */
    @Transactional(rollbackFor = Exception.class)
    public int forceBypassPort(String portCode) {

        // 입력값 검증
        if (ValueUtil.isEmpty(portCode)) {
            throw new IllegalArgumentException("portCode 는 필수입니다.");
        }
        logger.warn("[ Allocation ][ Port ] bypass start - portCode={}", portCode);

        // 포트 비활성화 + 카운트 0 + IDLE 모드
        String disablePortSql = """
                UPDATE tb_inventory_location
                   SET is_enabled        = false,
                       port_mode         = :idleMode,
                       active_task_count = 0
                 WHERE loc_id = :portCode
                """;
        queryManager.executeBySql(disablePortSql, ValueUtil.newMap(
                "idleMode,portCode", PortMode.IDLE.code(), portCode));

        // 해당 포트로 향하던 미완료 오더 → WAITING + to_loc_code 비움
        String rerouteSql = """
                UPDATE tb_wcs_shuttle_order
                   SET order_status = :waitingStatus,
                       to_loc_code  = NULL
                 WHERE to_loc_code  = :portCode
                   AND order_status < :completedStatus
                """;
        int rerouted = queryManager.executeBySql(rerouteSql, ValueUtil.newMap(
                "waitingStatus,portCode,completedStatus",
                ShuttleOrderStatus.WAITING.codeAsIntOrNull(),
                portCode,
                ShuttleOrderStatus.COMPLETED.codeAsIntOrNull()));

        logger.warn("[ Allocation ][ Port ] bypass done - portCode={}, rerouted={}", portCode, rerouted);
        return rerouted;
    }

    // 오더 유형별 연관 포트 - INBOUND 는 fromLocCode, OUTBOUND 는 toLocCode
    private String resolveRelatedPort(TbWcsShuttleOrder order) {
        if (OrderType.INBOUND.code().toString().equalsIgnoreCase(order.getOrderType())) {
            return order.getFromLocCode();
        }
        return order.getToLocCode();
    }
}
