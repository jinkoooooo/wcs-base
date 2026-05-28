package operato.logis.wcs.service.impl.inventory.reservation;

import lombok.RequiredArgsConstructor;
import operato.logis.inventory.consts.StockStatus;
import operato.logis.wcs.entity.ExtTbInventoryLocation;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.entity.TbWcsShuttleOrderItem;
import operato.logis.wcs.service.impl.allocation.port.PortService;
import operato.logis.wcs.service.impl.inventory.state.InventoryStockStateWriter;
import operato.logis.wcs.service.impl.order.state.ShuttleOrderStateWriter;
import operato.logis.wcs.service.repository.InventoryLocationRepository;
import operato.logis.wcs.service.repository.ShuttleOrderItemRepository;
import operato.logis.wcs.service.repository.ShuttleOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static operato.logis.wcs.common.util.lang.CommonUtils.toInt;

/**
 * BCR 스캔 시점에 입고 오더의 최종 적치 위치를 확정하는 서비스.
 *
 * 처리 순서:
 *   1. barcode + eqGroupId 로 INBOUND 입고 오더 조회 (CREATED + READY)
 *   2. 오더 SKU/Owner (또는 빈 파렛트 여부) 와 일치하는 모든 INBOUND_READY 후보 수집
 *   3. loc_deep DESC, loc_row DESC, loc_col DESC 로 정렬하여 가장 안쪽 1건 선정
 *   4. 현재 매핑과 best 가 다르면 두 location 의 stock_id/barcode swap
 *   5. 오더의 toLocCode / carryingStockId 를 확정 값으로 업데이트
 *   6. 해당 stockId 의 stock_status 를 INBOUND_READY → INBOUND 로 전이
 */
@Service
@RequiredArgsConstructor
public class InboundBcrConfirmer {

    private static final Logger logger = LoggerFactory.getLogger(InboundBcrConfirmer.class);

    // (sku, lotNo) 결합 키 구분자
    private static final String ITEM_KEY_DELIMITER = "::";

    private final ShuttleOrderRepository shuttleOrderRepository;
    private final ShuttleOrderItemRepository shuttleOrderItemRepository;
    private final InventoryLocationRepository locationRepository;
    private final InventoryStockStateWriter stockStateWriter;
    private final ShuttleOrderStateWriter shuttleOrderStateWriter;
    private final PortService portService;

    /**
     * BCR 스캔 결과로 입고 오더의 적치 위치를 확정한다.
     * 매칭 후보가 없거나 swap 이 실패하면 null 반환.
     */
    @Transactional(rollbackFor = Exception.class)
    public TbWcsShuttleOrder confirmOnScan(String eqGroupId, String barcode, String scanPortCode) {

        // 입력값 검증
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(barcode)) {
            logger.warn("[ Inventory ][ Reserve ] bcr params missing - eqGroupId={}, barcode={}", eqGroupId, barcode);
            return null;
        }

        // 매칭되는 INBOUND READY 오더 조회
        TbWcsShuttleOrder order = shuttleOrderRepository.findReadyInboundByBarcode(eqGroupId, barcode);
        if (ValueUtil.isEmpty(order)) {
            logger.warn("[ Inventory ][ Reserve ] bcr no matching order - eqGroupId={}, barcode={}", eqGroupId, barcode);
            return null;
        }

        // 오더 아이템 조회 + 빈 파렛트 여부 판단
        List<TbWcsShuttleOrderItem> items = shuttleOrderItemRepository.findByOrderKey(order.getOrderKey());
        boolean emptyPallet = isEmptyPalletOrder(items);

        // INBOUND_READY 후보 수집 + 가장 안쪽 1건 선정
        List<InboundCandidate> candidates = findWaitingInboundCandidates(
                eqGroupId, order.getOwnerCode(), items, emptyPallet);
        if (ValueUtil.isEmpty(candidates)) {
            logger.warn("[ Inventory ][ Reserve ] bcr no candidate (sku/owner mismatch) - orderKey={}, barcode={}",
                    order.getOrderKey(), barcode);
            return null;
        }
        InboundCandidate best = candidates.get(0);
        logger.info("[ Inventory ][ Reserve ] bcr best candidate - locId={}, stockId={}, deep={}, row={}, col={}",
                best.locId(), best.stockId(), best.locDeep(), best.locRow(), best.locCol());

        // 본 오더의 현재 매핑 (이전 BCR 사이클에서 스왑 가능성 있어 order.toLocCode 만 신뢰 X)
        String currentStockId = order.getCarryingStockId();
        String currentLocId = resolveCurrentLocId(eqGroupId, currentStockId, order.getToLocCode());

        // best 와 다르면 swap
        boolean sameStock = best.stockId().equals(currentStockId);
        boolean sameLoc = best.locId().equals(currentLocId);
        if (!sameStock || !sameLoc) {
            boolean swapOk = swapLocationStockMapping(eqGroupId,
                    order.getOrderKey(),
                    currentLocId, currentStockId,
                    best.locId(), best.stockId(),
                    barcode);
            if (!swapOk) {
                logger.error("[ Inventory ][ Reserve ] bcr swap failed - orderKey={}", order.getOrderKey());
                return null;
            }
        }

        // 오더 확정 - barcode 는 산출 시점 값 그대로 유지
        order.setToLocCode(best.locId());
        if (ValueUtil.isEmpty(currentStockId)) {
            order.setCarryingStockId(best.stockId());
        }
        if (ValueUtil.isNotEmpty(scanPortCode)) {
            order.setFromLocCode(scanPortCode);
        }
        shuttleOrderRepository.update(order, "toLocCode", "carryingStockId", "fromLocCode");
        logger.info("[ Inventory ][ Reserve ] bcr confirmed - orderKey={}, toLocId={}, stockId={}, fromLocId={}",
                order.getOrderKey(), order.getToLocCode(), order.getCarryingStockId(), order.getFromLocCode());

        // 확정된 rack 의 task_id 를 placeholder("INBOUND_READY") 에서 실제 orderKey 로 교체
        // rack 락은 완료 시점까지 유지 - 도착 후 handleCompletion 에서 unlock
        updateConfirmedRackTaskId(eqGroupId, best.locId(), order.getOrderKey());

        // 포트 락 해제 - ECS 가 들고 있으므로 WCS 측 임무 종료
        // 정책: ECS 송신 시점에 host_order_key 로 잠그고, BCR 스캔 시점에 NULL 해제
        portService.unlockByHost(eqGroupId, scanPortCode, order.getHostOrderKey());

        // BCR 매칭된 carryingStockId 가 INBOUND 상태로 전이 - ECS 송신 가능 상태
        String confirmedStockId = order.getCarryingStockId();
        if (ValueUtil.isNotEmpty(confirmedStockId)) {
            stockStateWriter.markInboundBcrConfirmed(eqGroupId, confirmedStockId);
        }

        // 재입고(부모가 OUTBOUND) BCR 스캔 시점에 부모 출고의 followUpSince 해소
        shuttleOrderStateWriter.clearParentFollowUpOnReInboundScan(order);
        return order;
    }

    /**
     * 현재 stockId 가 매핑된 실제 locId 를 해석한다.
     * 매핑 없으면 order.toLocCode 를 fallback 으로 사용.
     */
    private String resolveCurrentLocId(String eqGroupId, String currentStockId, String fallbackLocId) {
        if (ValueUtil.isNotEmpty(currentStockId)) {
            ExtTbInventoryLocation cur = locationRepository.findByStockId(eqGroupId, currentStockId);
            if (ValueUtil.isNotEmpty(cur)) return cur.getLocId();
        }
        return fallbackLocId;
    }

    /**
     * 확정된 rack 의 task_id 를 orderKey 로 갱신한다.
     */
    private void updateConfirmedRackTaskId(String eqGroupId, String bestLocId, String orderKey) {
        ExtTbInventoryLocation confirmedLoc = locationRepository.findByEqGroupIdAndLocId(eqGroupId, bestLocId);
        if (ValueUtil.isNotEmpty(confirmedLoc)) {
            confirmedLoc.setTaskId(orderKey);
            locationRepository.update(confirmedLoc, "taskId");
            logger.info("[ Inventory ][ Reserve ] rack taskId updated - locId={}, taskId={}", bestLocId, orderKey);
        }
    }

    /**
     * INBOUND_READY 후보를 수집한다.
     * 매칭 정책: (SKU, lotNo) 집합 동일 + owner 동일 (빈 파렛트 제외).
     * 정렬: loc_deep DESC, loc_row DESC, loc_col DESC (가장 안쪽 우선).
     */
    private List<InboundCandidate> findWaitingInboundCandidates(String eqGroupId,
                                                                String ownerCode,
                                                                List<TbWcsShuttleOrderItem> items,
                                                                boolean emptyPallet) {

        // 오더 아이템 키 집합
        Set<String> expectedKeys = expectedItemKeys(items, emptyPallet);

        // INBOUND_READY 상태 row 조회
        List<Map> rows = locationRepository.findInboundReadyJoinedStock(eqGroupId, StockStatus.INBOUND_READY.value());
        if (ValueUtil.isEmpty(rows)) {
            logger.info("[ Inventory ][ Reserve ] bcr no INBOUND_READY row");
            return Collections.emptyList();
        }

        // stockId 단위 그룹핑 (혼적 파렛트의 (sku, lotNo) row 가 여러 개일 수 있음)
        Map<String, StockGroup> groups = new HashMap<>();
        for (Map r : rows) {
            String stockId = stringOf(r.get("stock_id"));
            if (ValueUtil.isEmpty(stockId)) continue;

            StockGroup g = groups.computeIfAbsent(stockId, k -> new StockGroup(
                    stringOf(r.get("loc_id")), stockId,
                    toInt(r.get("loc_deep")), toInt(r.get("loc_row")), toInt(r.get("loc_col")),
                    stringOf(r.get("barcode")), stringOf(r.get("item_owner"))));
            g.addContent(stringOf(r.get("sku")), stringOf(r.get("lot_no")));
        }

        // owner + 아이템 키 집합 일치 그룹만 후보로 채택
        List<InboundCandidate> matched = new ArrayList<>();
        for (StockGroup g : groups.values()) {
            if (!emptyPallet && ValueUtil.isNotEmpty(ownerCode) && !ownerCode.equals(g.itemOwner)) continue;
            if (!expectedKeys.equals(g.keys)) continue;
            matched.add(new InboundCandidate(g.locId, g.stockId, g.locDeep, g.locRow, g.locCol, g.barcode));
        }

        // 안쪽 우선 정렬 (deep DESC → row DESC → col DESC)
        matched.sort(Comparator
                .comparingInt(InboundCandidate::locDeep).reversed()
                .thenComparing(Comparator.comparingInt(InboundCandidate::locRow).reversed())
                .thenComparing(Comparator.comparingInt(InboundCandidate::locCol).reversed()));
        return matched;
    }

    /**
     * 오더 아이템을 (sku::lotNo) 키 집합으로 변환한다.
     * 빈 파렛트는 단일 placeholder 키.
     */
    private Set<String> expectedItemKeys(List<TbWcsShuttleOrderItem> items, boolean emptyPallet) {
        Set<String> keys = new HashSet<>();
        if (emptyPallet || ValueUtil.isEmpty(items)) {
            keys.add(itemKey("", ""));
            return keys;
        }
        for (TbWcsShuttleOrderItem it : items) {
            if (ValueUtil.isEmpty(it)) continue;
            keys.add(itemKey(it.getItemCode(), it.getLotNo()));
        }

        // 빈 키 셋 방어
        if (ValueUtil.isEmpty(keys) || keys.equals(Set.of(itemKey("", "")))) {
            keys.clear();
            keys.add(itemKey("", ""));
        }
        return keys;
    }

    /**
     * 두 위치의 (stock_id, barcode) 매핑을 swap 한다.
     * best 위치에 묶여있던 다른 INBOUND 오더의 toLocCode 도 currentLocId 로 갱신.
     */
    private boolean swapLocationStockMapping(String eqGroupId,
                                             String currentOrderKey,
                                             String currentLocId, String currentStockId,
                                             String bestLocId, String bestStockId,
                                             String currentBarcode) {

        // 현재 매핑 없으면 swap 불필요
        if (ValueUtil.isEmpty(currentLocId) || ValueUtil.isEmpty(currentStockId)) {
            logger.info("[ Inventory ][ Reserve ] bcr swap skip - no current mapping. bestLocId={}, bestStockId={}",
                    bestLocId, bestStockId);
            return true;
        }

        // 두 로케이션 조회 + 실제 stock_id 정합성 검증
        ExtTbInventoryLocation curLoc = locationRepository.findByEqGroupIdAndLocId(eqGroupId, currentLocId);
        ExtTbInventoryLocation bestLoc = locationRepository.findByEqGroupIdAndLocId(eqGroupId, bestLocId);

        if (ValueUtil.isEmpty(curLoc) || !currentStockId.equals(curLoc.getStockId())) {
            logger.error("[ Inventory ][ Reserve ] bcr swap currentLoc stockId mismatch - locId={}, expected={}, actual={}",
                    currentLocId, currentStockId, curLoc == null ? null : curLoc.getStockId());
            return false;
        }
        if (ValueUtil.isEmpty(bestLoc) || !bestStockId.equals(bestLoc.getStockId())) {
            logger.error("[ Inventory ][ Reserve ] bcr swap bestLoc stockId mismatch - locId={}, expected={}, actual={}",
                    bestLocId, bestStockId, bestLoc == null ? null : bestLoc.getStockId());
            return false;
        }
        String bestBarcode = bestLoc.getBarcode();

        // best 위치에 묶여있던 다른 오더 갱신 (없거나 1건이어야 정상)
        List<TbWcsShuttleOrder> others = shuttleOrderRepository.findOtherCreatedInboundOrdersAt(
                eqGroupId, bestLocId, bestStockId, currentOrderKey);
        if (others.size() > 1) {
            logger.error("[ Inventory ][ Reserve ] bcr swap integrity violated - one stockId on many orders. abort. bestStockId={}, count={}",
                    bestStockId, others.size());
            return false;
        }
        if (ValueUtil.isEmpty(others)) {
            logger.warn("[ Inventory ][ Reserve ] bcr swap no other order at best - bestLocId={}, bestStockId={}",
                    bestLocId, bestStockId);
        } else {
            TbWcsShuttleOrder other = others.get(0);
            other.setToLocCode(currentLocId);
            shuttleOrderRepository.update(other, "toLocCode");
            logger.info("[ Inventory ][ Reserve ] bcr swap other order updated - otherOrderKey={}, {} -> {}",
                    other.getOrderKey(), bestLocId, currentLocId);
        }

        // 양 위치의 stock_id/barcode swap
        locationRepository.updateStockIdAndBarcode(eqGroupId, currentLocId, bestStockId, bestBarcode);
        locationRepository.updateStockIdAndBarcode(eqGroupId, bestLocId, currentStockId, currentBarcode);
        logger.info("[ Inventory ][ Reserve ] bcr swap done - {} <- {} | {} <- {}",
                currentLocId, bestStockId, bestLocId, currentStockId);
        return true;
    }

    /**
     * 빈 파렛트 오더 여부를 판단한다.
     * itemCode 가 있는 아이템이 하나라도 있으면 false.
     */
    private boolean isEmptyPalletOrder(List<TbWcsShuttleOrderItem> items) {
        if (ValueUtil.isEmpty(items)) return true;
        for (TbWcsShuttleOrderItem it : items) {
            if (ValueUtil.isNotEmpty(it) && ValueUtil.isNotEmpty(it.getItemCode())) return false;
        }
        return true;
    }

    // (sku, lotNo) 결합 키 생성
    private static String itemKey(String sku, String lotNo) {
        return nullSafe(sku) + ITEM_KEY_DELIMITER + nullSafe(lotNo);
    }

    // null/empty → 빈 문자열 + trim
    private static String nullSafe(String v) {
        return ValueUtil.isEmpty(v) ? "" : v.trim();
    }

    // Object → String (null 유지)
    private static String stringOf(Object v) {
        return v == null ? null : v.toString();
    }

    // BCR 후보 - 로케이션 + stockId + 정렬 키
    private record InboundCandidate(String locId, String stockId, int locDeep, int locRow, int locCol, String barcode) {}

    // stockId 단위로 그룹핑된 후보 정보 (정렬 키 + 컨텐츠 집합 동시 보유로 record 부적합)
    private static final class StockGroup {
        final String locId;
        final String stockId;
        final int locDeep;
        final int locRow;
        final int locCol;
        final String barcode;
        final String itemOwner;
        final Set<String> keys = new HashSet<>();

        StockGroup(String locId, String stockId, int locDeep, int locRow, int locCol,
                   String barcode, String itemOwner) {
            this.locId = locId;
            this.stockId = stockId;
            this.locDeep = locDeep;
            this.locRow = locRow;
            this.locCol = locCol;
            this.barcode = barcode;
            this.itemOwner = itemOwner;
        }

        void addContent(String sku, String lotNo) {
            keys.add(itemKey(sku, lotNo));
        }
    }
}
