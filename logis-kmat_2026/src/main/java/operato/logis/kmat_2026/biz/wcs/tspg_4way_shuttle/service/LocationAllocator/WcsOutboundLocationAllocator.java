package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.service.LocationAllocator;

import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.ErrorEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.LocTypeEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.OrderTypeEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.*;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.util.WcsLocationUtil;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsInventory;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsLocMst;
import operato.logis.kmat_2026.service.impl.TbWcsInventoryService;
import operato.logis.kmat_2026.service.impl.TbWcsLocMstService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.elidom.util.ValueUtil;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * [출고 할당 엔진 - 최종 고도화형]
 * 1. Inventory Check: 주문 품목(Multi-Item)을 모두 포함하는 재고지 검색
 * 2. Strategy: 출고 효율을 위해 샌드위치(ㅁ) 로케이션을 우선순위로 선정
 * 3. Execution: 정렬된 후보군을 기반으로 DB SKIP LOCKED 물리적 선점
 */
@Service
public class WcsOutboundLocationAllocator {
    private static final Logger logger = LoggerFactory.getLogger(WcsOutboundLocationAllocator.class);

    @Autowired protected TbWcsLocMstService locMstService;
    @Autowired protected TbWcsInventoryService inventoryService;
    @Autowired protected WcsOutboundPortAllocator portAllocator;

    public AllocationResult allocate(WcsOrderCommand command) {
        logger.info("========== [Strategic Outbound Allocation Start] : {} ==========", command.getSourceOrderKey());

        // 1. 주문 유형 검증
        if (!OrderTypeEnumCode.OUTBOUND.codeAsString().equalsIgnoreCase(command.getOrderType())) {
            return AllocationResult.fail(ErrorEnumCode.INVALID_PARAMETER.codeAsString(), "Not Outbound Order");
        }

        String eqGroupId = command.getEqGroupId();

        // 2. [Inventory Logic] 주문 내 모든 아이템이 존재하는 공통 로케이션 추출
        Set<String> candidateLocCodes = callInternalInventoryLogic(command);
        if (ValueUtil.isEmpty(candidateLocCodes)) {
            return AllocationResult.fail(ErrorEnumCode.NO_AVAILABLE_STOCK.codeAsString(), "조건을 만족하는 공통 재고지가 없습니다.");
        }

        // 3. [Strategy Phase] 지형 분석 및 출고 우선순위 결정
        // 출고는 통로 확보를 위해 안쪽(Sandwich)부터 비우는 것이 유리함
        List<LocWithPosition> allRacks = locMstService.findMultipleWithPosition(eqGroupId, null, LocTypeEnumCode.RACK);
        Map<String, Boolean> sandwichMap = WcsLocationUtil.buildSandwichMap(allRacks);

        List<LocWithPosition> tier1_sandwiches = new ArrayList<>(); // 1순위: 안쪽(Sandwich)
        List<LocWithPosition> tier2_edges = new ArrayList<>();      // 2순위: 바깥쪽(Edge)

        for (LocWithPosition rack : allRacks) {
            String code = rack.getLoc().getLocCode();
            if (!candidateLocCodes.contains(code) || rack.getLoc().getLockYn() == 1) continue;

            if (Boolean.TRUE.equals(sandwichMap.get(code))) tier1_sandwiches.add(rack);
            else tier2_edges.add(rack);
        }

        // 4. [Ordering] 전략에 따른 후보 코드 리스트 생성
        List<String> orderedCodes = Stream.concat(
                tier1_sandwiches.stream().sorted(Comparator.comparingInt(a -> a.getLoc().getLocSeq() != null ? a.getLoc().getLocSeq() : 999)),
                tier2_edges.stream().sorted(Comparator.comparingInt(a -> a.getLoc().getLocSeq() != null ? a.getLoc().getLocSeq() : 999))
        ).map(lwp -> lwp.getLoc().getLocCode()).collect(Collectors.toList());

        if (orderedCodes.isEmpty()) {
            return AllocationResult.fail(ErrorEnumCode.NO_AVAILABLE_LOCATION.codeAsString(), "가용 가능한 출고지가 없습니다.");
        }

        // 5. [Execution Phase] 물리적 선점 (Select Best Candidate & Lock)
        // 정렬된 후보 중 DB에서 실제로 락을 걸 수 있는 하나를 낚아챕니다.
        TbWcsLocMst lockedLoc = selectAndLockBestCandidate(eqGroupId, orderedCodes);

        if (lockedLoc == null) {
            return AllocationResult.fail(ErrorEnumCode.NO_AVAILABLE_LOCATION.codeAsString(), "모든 후보지가 타 작업에 의해 점유되었습니다.");
        }

        // 6. [Port Logic] 동적 출고 포트 할당
        String toLocCode = ValueUtil.isEmpty(command.getToLocCode())
                ? portAllocator.allocateBestPort(eqGroupId)
                : command.getToLocCode();

        logger.info("[Outbound Success] Final From: {}, To: {}, Strategy: {}",
                lockedLoc.getLocCode(), toLocCode, sandwichMap.getOrDefault(lockedLoc.getLocCode(), false) ? "SANDWICH_FIRST" : "EDGE_BACKUP");

        return AllocationResult.success(lockedLoc.getLocCode(), toLocCode, eqGroupId);
    }

    /**
     * 물리적 락 선점 실행 메서드 (내부 메서드 분리로 교체 용이성 확보)
     */
    protected TbWcsLocMst selectAndLockBestCandidate(String eqGroupId, List<String> orderedCodes) {
        // TbWcsLocMstService에 구현된 SKIP LOCKED 쿼리를 호출합니다.
        return locMstService.findAndLockBestOne(eqGroupId, orderedCodes);
    }

    /**
     * [재고 로직] 다품종 주문 대응 교집합 검색
     */
    private Set<String> callInternalInventoryLogic(WcsOrderCommand command) {
        List<WcsOrderCommandItem> items = command.getItems();
        if (ValueUtil.isEmpty(items)) return Collections.emptySet();

        List<Set<String>> perItemCandidates = new ArrayList<>();
        for (WcsOrderCommandItem item : items) {
            Set<String> locsForItem = findLocsWithEnoughStock(command.getEqGroupId(), command.getOwnerCode(), item);
            if (locsForItem.isEmpty()) return Collections.emptySet();
            perItemCandidates.add(locsForItem);
        }

        // 모든 품목이 공통으로 존재하는 로케이션만 추출
        Set<String> result = new HashSet<>(perItemCandidates.get(0));
        for (int i = 1; i < perItemCandidates.size(); i++) {
            result.retainAll(perItemCandidates.get(i));
        }
        return result;
    }

    private Set<String> findLocsWithEnoughStock(String eqGroupId, String ownerCode, WcsOrderCommandItem item) {
        List<TbWcsInventory> stocks = inventoryService.findAvailableStockWithLot(eqGroupId, ownerCode, item.getSkuCode(), item.getLotNo());
        Map<String, Integer> locQtyMap = new HashMap<>();

        for (TbWcsInventory inv : stocks) {
            int available = inv.getQty() - inv.getAllocQty();
            if (available > 0) {
                locQtyMap.merge(inv.getLocCode(), available, Integer::sum);
            }
        }

        return locQtyMap.entrySet().stream()
                .filter(entry -> entry.getValue() >= item.getQty())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}