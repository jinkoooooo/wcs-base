package operato.logis.wcs.service.impl.order.host;

import lombok.RequiredArgsConstructor;
import operato.logis.inventory.consts.StockStatus;
import operato.logis.wcs.consts.HostOrderType;
import operato.logis.wcs.consts.LocType;
import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.consts.StockType;
import operato.logis.wcs.entity.ExtTbInventoryLocation;
import operato.logis.wcs.entity.TbWcsHostOrder;
import operato.logis.wcs.service.impl.allocation.location.LocationService;
import operato.logis.wcs.service.repository.InventoryLocationRepository;
import operato.logis.wcs.service.repository.InventoryStockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.util.ValueUtil;

/**
 * host_order 단계 통합 예약 서비스.
 *
 * host_order 에 from/to 가 명시되어 있으면 셔틀 생성 전 단계에서 점유 마킹:
 *   - OUTBOUND (from 지정)         : from stock 을 HOST_PENDING + fromLoc.task_id 점유
 *   - MOVE     (from 항상, to 선택) : from stock 을 HOST_PENDING + fromLoc/toLoc task_id 점유
 *   - INBOUND  (to 지정)           : toLoc.task_id 점유 (stock 자체는 아직 없음)
 *
 * cancel/fail 시 releaseForHostOrder 로 원복.
 * 이미 셔틀 단계로 넘어갔으면 자기 키 아니라서 자동 no-op.
 */
@Service
@RequiredArgsConstructor
public class HostReservationService {

    private static final Logger logger = LoggerFactory.getLogger(HostReservationService.class);

    private final LocationService locationService;
    private final InventoryStockRepository stockRepository;
    private final InventoryLocationRepository inventoryLocationRepository;

    /**
     * host_order 등록 직후 호출. from/to 가 지정된 경우에만 점유.
     * 지정 안 된 일반 HOST 출고/입고 흐름은 no-op (allocator 가 나중에 산출).
     */
    @Transactional(rollbackFor = Exception.class)
    public void reserveForHostOrder(TbWcsHostOrder host) {
        if (ValueUtil.isEmpty(host)) return;

        OrderType type = HostOrderType.resolveBaseType(host.getOrderType());
        String eqGroupId = host.getEqGroupId();
        String hostKey = host.getHostOrderKey();
        String fromLoc = host.getFromLocCode();
        String toLoc = host.getToLocCode();

        switch (type) {
            case OUTBOUND -> {
                // 출고 포트(toLoc)는 보통 host 단계에 안 정해지므로 skip
                if (ValueUtil.isNotEmpty(fromLoc)) {
                    reserveFromStock(eqGroupId, fromLoc, hostKey);
                }
            }
            case MOVE -> {
                if (ValueUtil.isNotEmpty(fromLoc)) {
                    reserveFromStock(eqGroupId, fromLoc, hostKey);
                }
                if (ValueUtil.isNotEmpty(toLoc)) {
                    locationService.softReserve(eqGroupId, toLoc, hostKey);
                }
            }
            case INBOUND -> {
                // INBOUND 는 host 단계에 stock 이 아직 없음. toLoc 점유만.
                if (ValueUtil.isNotEmpty(toLoc)) {
                    locationService.softReserve(eqGroupId, toLoc, hostKey);
                }
            }
            default -> logger.debug("[ Order ][ Host ] reservation skipped - type={}, hostKey={}", type, hostKey);
        }
    }

    /**
     * host_order 취소/실패 시 호출. 자기 키로 잡혀있던 reservation 해제.
     * 이미 셔틀 단계로 넘어갔으면 (taskId = wcsOrderKey) 자동 no-op.
     */
    @Transactional(rollbackFor = Exception.class)
    public void releaseForHostOrder(TbWcsHostOrder host) {
        if (ValueUtil.isEmpty(host)) return;

        OrderType type = HostOrderType.resolveBaseType(host.getOrderType());
        String eqGroupId = host.getEqGroupId();
        String hostKey = host.getHostOrderKey();
        String fromLoc = host.getFromLocCode();
        String toLoc = host.getToLocCode();

        // 자기 키로 잡힌 location.task_id 해제 (양쪽 다 시도, 멱등).
        // 단, 포트 락은 PortService 가 BCR 스캔 시점에 관리 — host 완료 시점에 풀면
        // PortLockPolicy 가 잡은 재입고용 포트 락이 같이 풀려 책임 분리 위반.
        if (ValueUtil.isNotEmpty(fromLoc) && !isPortLocation(eqGroupId, fromLoc)) {
            locationService.softRelease(eqGroupId, fromLoc, hostKey);
        }
        if (ValueUtil.isNotEmpty(toLoc) && !isPortLocation(eqGroupId, toLoc)) {
            locationService.softRelease(eqGroupId, toLoc, hostKey);
        }

        // HOST_PENDING 복원 (OUTBOUND/MOVE 의 from stock):
        //   - NORMAL                                → IDLE  (정상 재고)
        //   - 그 외 (RETURN/DISPOSAL/QC_*/NIA_PENDING) → HOLD  (격리 재고)
        // 둘 다 시도 — 이미 셔틀 단계로 넘어간 stock 은 status 가 OUTBOUND/RELOCATION 이라
        // 두 CAS 모두 0행 매치 → 자동 no-op.
        if ((type == OrderType.OUTBOUND || type == OrderType.MOVE)
                && ValueUtil.isNotEmpty(fromLoc)) {

            // NORMAL 재고: HOST_PENDING → IDLE
            int toIdle = stockRepository.transitionStatusByLocWithTypeFilter(
                    eqGroupId, fromLoc,
                    StockStatus.HOST_PENDING, StockStatus.IDLE,
                    true, StockType.NORMAL);

            // 격리 재고: HOST_PENDING → HOLD
            int toHold = stockRepository.transitionStatusByLocWithTypeFilter(
                    eqGroupId, fromLoc,
                    StockStatus.HOST_PENDING, StockStatus.HOLD,
                    false, StockType.NORMAL);

            if (toIdle + toHold > 0) {
                logger.info("[ Order ][ Host ] reservation released stock - locId={}, toIdle={}, toHold={}",
                        fromLoc, toIdle, toHold);
            }
        }

        logger.info("[ Order ][ Host ] reservation released - hostKey={}, type={}", hostKey, type);
    }

    /**
     * 포트 location 여부 — INBOUND_PORT / OUTBOUND_PORT / IN_OUTBOUND_PORT.
     * 포트는 PortService 가 별도 lifecycle 로 관리하므로 host 완료 시점 release 대상에서 제외.
     */
    private boolean isPortLocation(String eqGroupId, String locId) {
        ExtTbInventoryLocation loc = inventoryLocationRepository.findByEqGroupIdAndLocId(eqGroupId, locId);
        if (ValueUtil.isEmpty(loc) || ValueUtil.isEmpty(loc.getLocType())) return false;
        String type = loc.getLocType();
        return LocType.INBOUND_PORT.code().equalsIgnoreCase(type)
                || LocType.OUTBOUND_PORT.code().equalsIgnoreCase(type)
                || LocType.IN_OUTBOUND_PORT.code().equalsIgnoreCase(type);
    }

    /**
     * OUTBOUND/MOVE 의 fromLoc 예약 — location.task_id + stock_status 둘 다.
     *
     * stock_status 시작점 IDLE / HOLD 모두 허용:
     *   - IDLE → HOST_PENDING : NORMAL 재고의 정상 출고 흐름
     *   - HOLD → HOST_PENDING : RETURN/DISPOSAL/QC_FAIL 등 격리 재고의 운영자 수동 출고
     *     (HOLD 는 자동 입출고 차단 의미일 뿐, 운영자 명시 출고는 허용)
     *
     * 두 시작점 모두 실패 = 이미 HOST_PENDING/INBOUND/OUTBOUND/RELOCATION 작업 중
     * → STOCK_BUSY, 트랜잭션 롤백으로 location.task_id 도 자동 NULL 복원.
     */
    private void reserveFromStock(String eqGroupId, String fromLocId, String hostKey) {
        // 1) location.task_id 점유 (CAS) — 실패 시 STOCK_BUSY
        locationService.softReserve(eqGroupId, fromLocId, hostKey);

        // 2-a) stock_status: IDLE → HOST_PENDING (CAS) — NORMAL 재고
        int transitioned = stockRepository.transitionStatusByLoc(
                eqGroupId, fromLocId,
                StockStatus.IDLE, StockStatus.HOST_PENDING);

        // 2-b) IDLE 가 없으면 HOLD → HOST_PENDING (CAS) — 격리 재고
        if (transitioned == 0) {
            transitioned = stockRepository.transitionStatusByLoc(
                    eqGroupId, fromLocId,
                    StockStatus.HOLD, StockStatus.HOST_PENDING);
        }

        if (transitioned == 0) {
            // 트랜잭션 롤백으로 location.task_id 도 자동 NULL 복원
            throw new ElidomRuntimeException(
                    "STOCK_BUSY",
                    "재고가 사용 중이거나 존재하지 않습니다 (locId=" + fromLocId + ")");
        }

        logger.info("[ Order ][ Host ] reservation reserved stock - eqGroupId={}, fromLocId={}, hostKey={}, transitioned={}",
                eqGroupId, fromLocId, hostKey, transitioned);
    }

}
