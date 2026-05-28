package operato.logis.wcs.service.impl.inventory.reservation;

import lombok.RequiredArgsConstructor;
import operato.logis.inventory.consts.StockStatus;
import operato.logis.wcs.entity.ExtTbInventoryStock;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.entity.TbWcsShuttleOrderItem;
import operato.logis.wcs.service.impl.inventory.state.InventoryStockStateWriter;
import operato.logis.wcs.service.repository.InventoryLocationRepository;
import operato.logis.wcs.service.repository.InventoryStockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import xyz.elidom.util.ValueUtil;

import java.util.List;
import java.util.UUID;

import static operato.logis.wcs.common.util.lang.CommonUtils.nz;

/**
 * 입고 예약/완료 진입점 서비스.
 *
 * 책임:
 *   - 입고 예약 (stock 생성 + toLoc 선매핑)
 *   - 입고 완료 (NORMAL / AWAITING_TEST / AWAITING_NIA / RETURN 4분기)
 *
 * 위임 책임:
 *   - QC 결과 전파 → InboundQcPropagator
 *   - BCR 위치 확정 → InboundBcrConfirmer
 */
@Service
@RequiredArgsConstructor
public class InboundReservationService {

    private static final Logger logger = LoggerFactory.getLogger(InboundReservationService.class);

    // 빈 파렛트 stock 의 SKU 마커 (tb_inventory_stock.sku NOT NULL 회피용)
    public static final String EMPTY_PALLET_SKU = "";

    // 빈 파렛트 stock 의 owner 마커 (NOT NULL 컬럼 회피용)
    public static final String EMPTY_PALLET_OWNER = "";

    private final InventoryStockRepository stockRepository;
    private final InventoryLocationRepository locationRepository;
    private final InventoryStockStateWriter stockStateWriter;
    private final InboundQcPropagator qcPropagator;
    private final InboundBcrConfirmer bcrConfirmer;

    /**
     * QC 결과를 stock 카테고리로 전파한다. (위임)
     */
    public int propagateQcResultToStocks(String hostOrderKey, boolean allPassed, boolean anyFailed) {
        return qcPropagator.propagateQcResultToStocks(hostOrderKey, allPassed, anyFailed);
    }

    /**
     * 해당 host_order 의 nia_required 여부 조회. (위임)
     */
    public boolean isNiaRequiredForHostOrder(String hostOrderKey) {
        return qcPropagator.isNiaRequiredForHostOrder(hostOrderKey);
    }

    /**
     * BCR 스캔 시점의 입고 위치 확정. (위임)
     */
    public TbWcsShuttleOrder confirmInboundLocationOnBcrScan(String eqGroupId, String barcode, String scanPortCode) {
        return bcrConfirmer.confirmOnScan(eqGroupId, barcode, scanPortCode);
    }

    /**
     * 입고 산출 - stock 을 INBOUND_READY 로 미리 생성하고 toLocId 가 있으면 선매핑한다.
     * 실제 위치 확정은 BCR 스캔 시점 (confirmInboundLocationOnBcrScan) 에 이루어진다.
     * 성공 시 생성된 stockId, 실패 시 null 반환.
     */
    @Transactional(rollbackFor = Exception.class)
    public String reserveInboundPallet(String eqGroupId, String toLocId, String itemOwner,
                                       List<TbWcsShuttleOrderItem> items, String barcode,
                                       String originHostOrderKey) {

        // 입력값 검증
        if (ValueUtil.isEmpty(eqGroupId)) {
            logger.error("[ Inventory ][ Reserve ] eqGroupId missing");
            return null;
        }
        if (ValueUtil.isEmpty(barcode)) {
            logger.error("[ Inventory ][ Reserve ] barcode missing - eqGroupId={}", eqGroupId);
            return null;
        }

        // 빈 파렛트 여부 + non-empty 의 owner 검증
        boolean emptyPallet = ValueUtil.isEmpty(items);
        if (!emptyPallet && ValueUtil.isEmpty(itemOwner)) {
            logger.error("[ Inventory ][ Reserve ] itemOwner missing (non-empty pallet) - eqGroupId={}", eqGroupId);
            return null;
        }

        // stockId 발급
        String stockId = UUID.randomUUID().toString();
        logger.info("[ Inventory ][ Reserve ] start - eqGroupId={}, toLocId={}, stockId={}, emptyPallet={}, itemCount={}, originHostOrderKey={}",
                eqGroupId, toLocId, stockId, emptyPallet, emptyPallet ? 0 : items.size(), originHostOrderKey);

        // 빈 파렛트는 placeholder row 1건, 일반은 아이템별로 stock row 생성
        if (emptyPallet) {
            ExtTbInventoryStock created = stockRepository.createOrIncreaseQty(
                    eqGroupId, stockId,
                    ValueUtil.isEmpty(itemOwner) ? EMPTY_PALLET_OWNER : itemOwner,
                    EMPTY_PALLET_SKU, null, 0,
                    null, null,
                    StockStatus.INBOUND_READY);
            applyOriginHostOrderKey(created, originHostOrderKey);
        } else {
            boolean anyCreated = false;
            for (TbWcsShuttleOrderItem item : items) {

                // 한 건이라도 유효하면 진행
                if (!isValidItem(item)) {
                    logger.warn("[ Inventory ][ Reserve ] invalid item skip - orderKey={}, sku={}, qty={}",
                            item == null ? null : item.getOrderKey(),
                            item == null ? null : item.getItemCode(),
                            item == null ? null : item.getQty());
                    continue;
                }
                ExtTbInventoryStock created = stockRepository.createOrIncreaseQty(
                        eqGroupId, stockId, itemOwner,
                        item.getItemCode(), item.getLotNo(), item.getQty(),
                        item.getProduceDate(), item.getExpiryDate(),
                        StockStatus.INBOUND_READY);
                applyOriginHostOrderKey(created, originHostOrderKey);
                anyCreated = true;
            }
            if (!anyCreated) {
                logger.warn("[ Inventory ][ Reserve ] no valid item - eqGroupId={}, stockId={}", eqGroupId, stockId);
                return null;
            }
        }

        // toLocId 있으면 로케이션 선매핑
        if (StringUtils.hasText(toLocId)) {
            locationRepository.updateStockIdAndBarcode(eqGroupId, toLocId, stockId, barcode);
        }
        return stockId;
    }

    /**
     * 일반 입고 완료 - IDLE + NORMAL.
     */
    @Transactional(rollbackFor = Exception.class)
    public void completeInboundAsNormal(String eqGroupId, String stockId, String toLocId, String barcode,
                                        String sourceHostOrderKey) {
        if (!validate(eqGroupId, stockId)) return;
        stockStateWriter.markInboundCompleteNormal(eqGroupId, stockId);
        applyInboundCompletionSideEffects(eqGroupId, stockId, toLocId, barcode, sourceHostOrderKey);
    }

    /**
     * 입고 완료 (시험 결과 대기) - HOLD + QC_PENDING.
     */
    @Transactional(rollbackFor = Exception.class)
    public void completeInboundAwaitingTest(String eqGroupId, String stockId, String toLocId, String barcode,
                                            String sourceHostOrderKey) {
        if (!validate(eqGroupId, stockId)) return;
        stockStateWriter.markInboundCompleteAwaitingTest(eqGroupId, stockId);
        applyInboundCompletionSideEffects(eqGroupId, stockId, toLocId, barcode, sourceHostOrderKey);
    }

    /**
     * 입고 완료 (국검 미승인 대기) - HOLD + NIA_PENDING.
     */
    @Transactional(rollbackFor = Exception.class)
    public void completeInboundAwaitingNia(String eqGroupId, String stockId, String toLocId, String barcode,
                                           String sourceHostOrderKey) {
        if (!validate(eqGroupId, stockId)) return;
        stockStateWriter.markInboundCompleteAwaitingNia(eqGroupId, stockId);
        applyInboundCompletionSideEffects(eqGroupId, stockId, toLocId, barcode, sourceHostOrderKey);
    }

    /**
     * 반품 입고 완료 - HOLD + RETURN.
     */
    @Transactional(rollbackFor = Exception.class)
    public void completeInboundAsReturn(String eqGroupId, String stockId, String toLocId, String barcode,
                                        String sourceHostOrderKey) {
        if (!validate(eqGroupId, stockId)) return;
        stockStateWriter.markInboundCompleteReturnGoods(eqGroupId, stockId);
        applyInboundCompletionSideEffects(eqGroupId, stockId, toLocId, barcode, sourceHostOrderKey);
    }

    // 입고 완료 공통 파라미터 검증
    private boolean validate(String eqGroupId, String stockId) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(stockId)) {
            logger.warn("[ Inventory ][ Reserve ] inbound complete params missing - eqGroupId={}, stockId={}", eqGroupId, stockId);
            return false;
        }
        return true;
    }

    // 입고 완료 공통 부수 처리 - source_host_order_key 기록 + 로케이션 매핑 확정
    private void applyInboundCompletionSideEffects(String eqGroupId, String stockId, String toLocId,
                                                   String barcode, String sourceHostOrderKey) {
        if (StringUtils.hasText(sourceHostOrderKey)) {
            stockRepository.updateSourceHostOrderKey(eqGroupId, stockId, sourceHostOrderKey);
        }
        if (StringUtils.hasText(toLocId)) {
            locationRepository.updateStockIdAndBarcode(eqGroupId, toLocId, stockId, barcode);
        }
        logger.info("[ Inventory ][ Reserve ] inbound completed - eqGroupId={}, stockId={}, toLocId={}, sourceHostOrderKey={}",
                eqGroupId, stockId, toLocId, sourceHostOrderKey);
    }

    // 아이템 유효성 - sku + 양수 qty
    private boolean isValidItem(TbWcsShuttleOrderItem item) {
        return ValueUtil.isNotEmpty(item)
                && ValueUtil.isNotEmpty(item.getItemCode())
                && nz(item.getQty()) > 0;
    }

    // stock 행에 origin_host_order_key 적용
    private void applyOriginHostOrderKey(ExtTbInventoryStock stock, String originHostOrderKey) {
        if (ValueUtil.isEmpty(stock) || ValueUtil.isEmpty(originHostOrderKey)) return;
        stock.setOriginHostOrderKey(originHostOrderKey);
        stockRepository.update(stock);
    }
}
