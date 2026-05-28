package operato.logis.wcs.service.impl.inventory.reservation;

import lombok.RequiredArgsConstructor;
import operato.logis.inventory.consts.StockStatus;
import operato.logis.wcs.entity.ExtTbInventoryStock;
import operato.logis.wcs.service.impl.inventory.state.InventoryStockStateWriter;
import operato.logis.wcs.service.repository.InventoryLocationRepository;
import operato.logis.wcs.service.repository.InventoryStockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import xyz.elidom.util.ValueUtil;

/**
 * 예약 해제/복원/상태 조회 서비스.
 *
 * 책임:
 *   - 입고 산출 취소 (stock 삭제 + 매핑 해제)
 *   - 출고/이동 롤백 (RELOCATION/OUTBOUND → IDLE 복원 + 매핑 재설정)
 *   - 현재 재고 상태 조회 (read-only)
 */
@Service
@RequiredArgsConstructor
public class ReservationReleaseService {

    private static final Logger logger = LoggerFactory.getLogger(ReservationReleaseService.class);

    private final InventoryStockRepository stockRepository;
    private final InventoryLocationRepository locationRepository;
    private final InventoryStockStateWriter stockStateWriter;

    /**
     * 입고 산출 취소 - 아직 물리 입고 전이므로 stock 행 자체를 삭제하고 로케이션 매핑도 해제한다.
     * INBOUND_READY (BCR 미경유) / INBOUND (BCR 경유) 양쪽 모두 안전하게 정리.
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelInboundReservation(String eqGroupId, String stockId, String fromLocId) {

        // 입력값 검증
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(stockId)) {
            logger.warn("[ Inventory ][ Reserve ] inbound cancel params missing - eqGroupId={}, stockId={}", eqGroupId, stockId);
            return;
        }

        // INBOUND/INBOUND_READY 아니면 삭제 중단 (안전장치)
        StockStatus current = getStockStatus(eqGroupId, stockId);
        if (ValueUtil.isNotEmpty(current)
                && current != StockStatus.INBOUND
                && current != StockStatus.INBOUND_READY) {
            logger.warn("[ Inventory ][ Reserve ] inbound cancel abort - not INBOUND/INBOUND_READY. stockId={}, status={}",
                    stockId, current);
            return;
        }

        // 로케이션 매핑 해제 (fromLocId 있을 때만)
        if (StringUtils.hasText(fromLocId)) {
            locationRepository.clearStockId(eqGroupId, fromLocId);
        }

        // 재고 행 삭제
        int deleted = stockRepository.deleteByStockId(eqGroupId, stockId);
        logger.info("[ Inventory ][ Reserve ] inbound cancel done - eqGroupId={}, stockId={}, fromLocId={}, deletedRows={}",
                eqGroupId, stockId, fromLocId, deleted);
    }

    /**
     * 출고 롤백 - OUTBOUND → IDLE 복원 + 로케이션 매핑 재설정.
     */
    @Transactional(rollbackFor = Exception.class)
    public void restoreOutbound(String eqGroupId, String fromLocId, String stockId, String barcode) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(stockId)) return;

        // IDLE 복원
        stockStateWriter.markIdleAfterOutboundRollback(eqGroupId, stockId);

        // 로케이션 매핑 재설정
        if (StringUtils.hasText(fromLocId)) {
            locationRepository.updateStockIdAndBarcode(eqGroupId, fromLocId, stockId, barcode);
        }
    }

    /**
     * 이동 롤백 - RELOCATION → IDLE 복원 + 로케이션 매핑 재설정.
     */
    @Transactional(rollbackFor = Exception.class)
    public void restoreRelocation(String eqGroupId, String fromLocId, String stockId, String barcode) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(stockId)) return;

        // IDLE 복원
        stockStateWriter.markIdleAfterRelocationRollback(eqGroupId, stockId);

        // 로케이션 매핑 재설정
        if (StringUtils.hasText(fromLocId)) {
            locationRepository.updateStockIdAndBarcode(eqGroupId, fromLocId, stockId, barcode);
        }
    }

    /**
     * 현재 재고 상태 조회 - 없으면 null.
     */
    public StockStatus getStockStatus(String eqGroupId, String stockId) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(stockId)) return null;

        ExtTbInventoryStock stock = stockRepository.findAnyByStockId(eqGroupId, stockId);
        if (ValueUtil.isEmpty(stock) || ValueUtil.isEmpty(stock.getStockStatus())) return null;
        return StockStatus.valueOf(stock.getStockStatus());
    }

    /**
     * 물리 삭제 또는 논리 삭제(DISPOSAL) 여부 판단.
     */
    public boolean isOutboundFinalized(String eqGroupId, String stockId) {
        if (ValueUtil.isEmpty(stockId)) return false;

        ExtTbInventoryStock stock = stockRepository.findAnyByStockId(eqGroupId, stockId);

        // 행이 없으면 물리 삭제됨 = 출고 완료
        if (ValueUtil.isEmpty(stock)) return true;

        // 행이 있어도 is_enabled=false 면 논리 삭제됨 = 출고 완료
        return !Boolean.TRUE.equals(stock.getIsEnabled());
    }
}
