package operato.logis.wcs.service.impl.inventory.reservation;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.entity.ExtTbInventoryLocation;
import operato.logis.wcs.service.impl.inventory.state.InventoryStockStateWriter;
import operato.logis.wcs.service.repository.InventoryLocationRepository;
import operato.logis.wcs.service.repository.InventoryStockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import xyz.elidom.util.ValueUtil;

import static operato.logis.wcs.common.util.check.Validator.hasLocationKey;

/**
 * 출고/이동(RELOCATION) 예약 마킹/완료/픽업 서비스.
 *
 * 출고와 이동은 "상태 전이 + 완료 처리" 패턴이 동일하므로 한 클래스에 묶는다.
 * 예약 해제/복원은 ReservationReleaseService 가 담당.
 */
@Service
@RequiredArgsConstructor
public class OutboundReservationService {

    private static final Logger logger = LoggerFactory.getLogger(OutboundReservationService.class);

    private final InventoryStockRepository stockRepository;
    private final InventoryLocationRepository locationRepository;
    private final InventoryStockStateWriter stockStateWriter;

    /**
     * 출고 시작 - fromLoc 의 stock 상태를 IDLE → OUTBOUND 로 전이. 매핑은 유지.
     */
    @Transactional(rollbackFor = Exception.class)
    public String markOutboundPendingOnlyStatus(String eqGroupId, String fromLocId) {

        // fromLoc 의 stockId 해석
        String stockId = resolveStockId(eqGroupId, fromLocId);
        if (ValueUtil.isEmpty(stockId)) return null;

        // OUTBOUND 예약 상태 전이
        stockStateWriter.markOutboundReserved(eqGroupId, stockId);
        return stockId;
    }

    /**
     * 출고 완료 - 물리 삭제.
     */
    @Transactional(rollbackFor = Exception.class)
    public void finalizeOutbound(String eqGroupId, String stockId) {

        // 입력값 검증
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(stockId)) {
            logger.warn("[ Inventory ][ Reserve ] outbound complete params missing - eqGroupId={}, stockId={}", eqGroupId, stockId);
            return;
        }

        // 재고 행 삭제
        int deleted = stockRepository.deleteByStockId(eqGroupId, stockId);
        logger.info("[ Inventory ][ Reserve ] outbound stock deleted - eqGroupId={}, stockId={}, deletedRows={}",
                eqGroupId, stockId, deleted);
    }

    /**
     * 폐기 출고 완료 - 물리 삭제 대신 논리 삭제 (item_qty=0 + is_enabled=false).
     * 로케이션 매핑은 해제. 감사 추적용으로 stock 행은 남긴다.
     */
    @Transactional(rollbackFor = Exception.class)
    public int finalizeDisposalOutbound(String eqGroupId, String stockId, String fromLocId) {

        // 입력값 검증
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(stockId)) {
            logger.warn("[ Inventory ][ Reserve ] disposal complete params missing - eqGroupId={}, stockId={}", eqGroupId, stockId);
            return 0;
        }

        // 논리 삭제 마킹
        int marked = stockRepository.markDisposed(eqGroupId, stockId);

        // 로케이션 매핑 해제 (fromLocId 있을 때만)
        if (StringUtils.hasText(fromLocId)) {
            locationRepository.clearStockId(eqGroupId, fromLocId);
        }
        logger.info("[ Inventory ][ Reserve ] disposal logical delete - eqGroupId={}, stockId={}, fromLocId={}",
                eqGroupId, stockId, fromLocId);
        return marked;
    }

    /**
     * 이동 시작 - fromLoc 의 stock 상태를 IDLE → RELOCATION 으로 전이. 매핑은 유지.
     */
    @Transactional(rollbackFor = Exception.class)
    public String markRelocationPendingOnlyStatus(String eqGroupId, String fromLocId) {

        // fromLoc 의 stockId 해석
        String stockId = resolveStockId(eqGroupId, fromLocId);
        if (ValueUtil.isEmpty(stockId)) return null;

        // RELOCATION 예약 상태 전이
        stockStateWriter.markRelocationReserved(eqGroupId, stockId);
        return stockId;
    }

    /**
     * 이동 완료 - toLoc 으로 stockId/barcode 매핑 후 stock 상태 RELOCATION → IDLE.
     */
    @Transactional(rollbackFor = Exception.class)
    public void finalizeRelocation(String eqGroupId, String toLocId, String stockId, String barcode) {

        // 입력값 검증
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(stockId) || ValueUtil.isEmpty(toLocId)) {
            logger.warn("[ Inventory ][ Reserve ] relocation complete params missing - eqGroupId={}, stockId={}, toLocId={}",
                    eqGroupId, stockId, toLocId);
            return;
        }

        // 상태 전이 + 새 로케이션 매핑
        stockStateWriter.markRelocationCompleted(eqGroupId, stockId);
        locationRepository.updateStockIdAndBarcode(eqGroupId, toLocId, stockId, barcode);
    }

    /**
     * 물리적 픽업 시점에 호출 - 출발 로케이션의 stockId/barcode 매핑을 NULL 로 해제.
     */
    @Transactional(rollbackFor = Exception.class)
    public void clearLocationMappingOnPickup(String eqGroupId, String fromLocId) {
        if (!hasLocationKey(eqGroupId, fromLocId)) return;
        locationRepository.clearStockId(eqGroupId, fromLocId);
        logger.info("[ Inventory ][ Reserve ] pickup mapping cleared - locId={}", fromLocId);
    }

    // fromLoc 에 매핑된 stockId 해석 - 매핑 없으면 null
    private String resolveStockId(String eqGroupId, String fromLocId) {
        ExtTbInventoryLocation loc = locationRepository.findByEqGroupIdAndLocId(eqGroupId, fromLocId);
        if (ValueUtil.isEmpty(loc) || ValueUtil.isEmpty(loc.getStockId())) {
            logger.warn("[ Inventory ][ Reserve ] location has no stockId - locId={}", fromLocId);
            return null;
        }
        return loc.getStockId();
    }
}
