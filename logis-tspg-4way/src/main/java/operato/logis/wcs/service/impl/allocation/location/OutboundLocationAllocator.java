package operato.logis.wcs.service.impl.allocation.location;

import lombok.RequiredArgsConstructor;
import operato.logis.inventory.consts.OutboundCalculateStrategy;
import operato.logis.inventory.dto.ItemIdentifierDto;
import operato.logis.inventory.dto.OutboundStockRequestDto;
import operato.logis.inventory.entity.TbInventoryStock;
import operato.logis.inventory.service.InventoryOutboundStockService;
import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.consts.WcsError;
import operato.logis.wcs.dto.AllocationResult;
import operato.logis.wcs.dto.WcsOrderCommand;
import operato.logis.wcs.entity.ExtTbInventoryLocation;
import operato.logis.wcs.entity.ExtTbInventoryStock;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.entity.TbWcsShuttleOrderItem;
import operato.logis.wcs.service.impl.allocation.port.OutboundPortAllocator;
import operato.logis.wcs.service.repository.InventoryLocationRepository;
import operato.logis.wcs.service.repository.InventoryStockRepository;
import operato.logis.wcs.service.repository.ShuttleOrderItemRepository;
import operato.logis.wcs.service.repository.ShuttleOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static operato.logis.wcs.common.util.lang.CommonUtils.nullToEmpty;
import static operato.logis.wcs.common.util.lang.CommonUtils.nz;

/**
 * 출고 할당기 - 다중 파렛트 + 잔여 재고(putback) 감지.
 *
 * 처리 순서:
 *   1. InventoryOutboundStockService.calculateOutboundStock → 출고 대상 재고 산출
 *   2. stockId 별 그룹핑, 로케이션별 LocationAllocation 생성
 *   3. 포트 할당 (지정 우선, 없으면 OutboundPortAllocator)
 *   4. 잔여 감지(putback) - handleCompletion 에서 같은 host_order 의 재입고 발행에 사용
 */
@Service
@RequiredArgsConstructor
public class OutboundLocationAllocator {

    private static final Logger logger = LoggerFactory.getLogger(OutboundLocationAllocator.class);

    // SKU + LOT 결합 키 구분자
    private static final String KEY_DELIMITER = "|";

    private final InventoryOutboundStockService inventoryOutboundStockService;
    private final OutboundPortAllocator portAllocator;
    private final InventoryLocationRepository locationRepository;
    private final InventoryStockRepository stockRepository;
    private final ShuttleOrderItemRepository shuttleOrderItemRepository;
    private final ShuttleOrderRepository shuttleOrderRepository;

    /**
     * 출고 대상 재고를 산출하고 로케이션별로 그룹핑한 뒤 포트 할당과 잔여 감지를 수행한다.
     */
    @Transactional(rollbackFor = Exception.class)
    public AllocationResult allocate(WcsOrderCommand command) {
        logger.info("[ Allocation ][ Loc ] outbound start - hostOrderKey={}", command.getHostOrderKey());

        // 주문 유형 검증
        if (!OrderType.OUTBOUND.matches(command.getOrderType())) {
            return AllocationResult.fail(WcsError.INVALID_PARAMETER.codeAsString(), "Not Outbound Order");
        }

        // 동일 파렛트 진행 중 OUTBOUND 가드 (중복 출고 방지)
        if (ValueUtil.isNotEmpty(command.getBarCode())
                && shuttleOrderRepository.hasInProgressOrderByBarcode(command.getBarCode(), OrderType.OUTBOUND)) {
            logger.warn("[ Allocation ][ Loc ] outbound rejected - duplicate barcode in progress. barcode={}, hostOrderKey={}",
                    command.getBarCode(), command.getHostOrderKey());
            return AllocationResult.fail(
                    WcsError.NO_AVAILABLE_LOCATION.codeAsString(),
                    "동일 파렛트의 진행 중 출고 작업이 존재합니다.");
        }

        // 출고 대상 재고 산출
        String eqGroupId = command.getEqGroupId();
        List<TbInventoryStock> outboundStocks = resolveOutboundStocks(command, eqGroupId);
        if (ValueUtil.isEmpty(outboundStocks)) {
            logger.warn("[ Allocation ][ Loc ] outbound rejected - no available stock. eqGroupId={}", eqGroupId);
            return AllocationResult.fail(
                    WcsError.NO_AVAILABLE_STOCK.codeAsString(),
                    "조건을 만족하는 출고 재고가 없습니다.");
        }

        // 산출 결과 로깅
        for (int i = 0; i < outboundStocks.size(); i++) {
            TbInventoryStock s = outboundStocks.get(i);
            logger.info("[ Allocation ][ Loc ] outbound stock#{} - stockId={}, sku={}, lot={}, qty={}",
                    i, s.getStockId(), s.getItemCode(), s.getLotNo(), s.getItemQty());
        }

        // stockId 별 그룹핑
        Map<String, List<TbInventoryStock>> stocksByLoc = outboundStocks.stream()
                .filter(s -> ValueUtil.isNotEmpty(s.getStockId()))
                .collect(Collectors.groupingBy(TbInventoryStock::getStockId));

        // 로케이션별 LocationAllocation 생성
        List<AllocationResult.LocationAllocation> locationAllocations = new ArrayList<>();
        for (Map.Entry<String, List<TbInventoryStock>> entry : stocksByLoc.entrySet()) {
            String stockId = entry.getKey();
            List<TbInventoryStock> stocks = entry.getValue();

            // stockId → 로케이션 ID 해석
            String fromLocId = resolveLocIdByStockId(eqGroupId, stockId);
            if (ValueUtil.isEmpty(fromLocId)) {
                logger.warn("[ Allocation ][ Loc ] outbound stockId loc not found - stockId={}", stockId);
                throw new ElidomRuntimeException("stockId (" + stockId + ") 에 대한 로케이션을 찾을 수 없습니다.");
            }

            // 출고 포트(toLocId) 결정 - 지정 우선, 없으면 동적 할당
            String toLocId = resolveOutboundPort(command, eqGroupId, fromLocId);
            if (ValueUtil.isEmpty(toLocId)) {
                return AllocationResult.fail(
                        WcsError.NO_AVAILABLE_LOCATION.codeAsString(),
                        "출고 포트 가용 0");
            }

            // LocationAllocation 구성
            AllocationResult.LocationAllocation locAlloc = new AllocationResult.LocationAllocation();
            locAlloc.setFromLocId(fromLocId);
            locAlloc.setToLocId(toLocId);
            locAlloc.setEqGroupId(eqGroupId);
            locAlloc.setItems(toAllocationItems(stocks));
            locationAllocations.add(locAlloc);
        }

        if (ValueUtil.isEmpty(locationAllocations)) {
            return AllocationResult.fail(
                    WcsError.NO_AVAILABLE_LOCATION.codeAsString(),
                    "출고 로케이션을 결정할 수 없습니다.");
        }

        // 로케이션별 잔여 재고(putback) 감지
        for (AllocationResult.LocationAllocation locAlloc : locationAllocations) {
            detectPartialPicking(locAlloc, eqGroupId, locAlloc.getFromLocId(), locAlloc.getItems());
        }

        logger.info("[ Allocation ][ Loc ] outbound success - locCount={}, stockCount={}",
                locationAllocations.size(), outboundStocks.size());

        return AllocationResult.successMulti(locationAllocations, eqGroupId);
    }

    /**
     * 공출고 복구용 재산출.
     * 격리된 fromLoc 은 task_id 마킹으로 자동 배제되므로 동일 SKU/LOT 의 다른 로케이션이 산출된다.
     */
    @Transactional(rollbackFor = Exception.class)
    public String reallocate(TbWcsShuttleOrder order) {

        // 입력값 검증
        if (ValueUtil.isEmpty(order) || ValueUtil.isEmpty(order.getOrderKey())) {
            logger.warn("[ Allocation ][ Loc ] outbound reallocate - order empty");
            return null;
        }

        logger.info("[ Allocation ][ Loc ] outbound reallocate start - orderKey={}, prevFromLocId={}",
                order.getOrderKey(), order.getFromLocCode());

        // 오더 아이템 조회
        String eqGroupId = order.getEqGroupId();
        List<TbWcsShuttleOrderItem> items = shuttleOrderItemRepository.findByOrderKey(order.getOrderKey());
        if (ValueUtil.isEmpty(items)) {
            logger.warn("[ Allocation ][ Loc ] outbound reallocate - items empty. orderKey={}", order.getOrderKey());
            return null;
        }

        // 재산출용 DTO 구성
        OutboundStockRequestDto dto = new OutboundStockRequestDto();
        dto.setLocGroup(eqGroupId);

        List<ItemIdentifierDto> itemList = new ArrayList<>();
        for (TbWcsShuttleOrderItem item : items) {
            if (ValueUtil.isEmpty(item) || ValueUtil.isEmpty(item.getItemCode())) continue;
            itemList.add(toItemIdentifier(order.getOwnerCode(), item.getItemCode(), item.getQty()));
        }
        dto.setItemList(itemList);
        dto.setOutboundCalculateStrategy(OutboundCalculateStrategy.FIFO);

        // 재고 재산출
        List<TbInventoryStock> outboundStocks;
        try {
            outboundStocks = inventoryOutboundStockService.calculateOutboundStock(dto);
        } catch (Exception e) {
            logger.error("[ Allocation ][ Loc ] outbound reallocate failed - orderKey={}", order.getOrderKey(), e);
            return null;
        }

        if (ValueUtil.isEmpty(outboundStocks)) {
            logger.warn("[ Allocation ][ Loc ] outbound reallocate - no alternative stock. orderKey={}", order.getOrderKey());
            return null;
        }

        // 첫 stockId → 로케이션 해석
        String newStockId = outboundStocks.get(0).getStockId();
        if (ValueUtil.isEmpty(newStockId)) return null;

        String newFromLocId = resolveLocIdByStockId(eqGroupId, newStockId);
        if (ValueUtil.isEmpty(newFromLocId)) {
            logger.warn("[ Allocation ][ Loc ] outbound reallocate - new stock loc not found. stockId={}", newStockId);
            return null;
        }

        // 이전 격리 위치와 동일하면 의미 없음
        if (newFromLocId.equals(order.getFromLocCode())) {
            logger.warn("[ Allocation ][ Loc ] outbound reallocate - same as prev blocked loc. orderKey={}, locId={}",
                    order.getOrderKey(), newFromLocId);
            return null;
        }

        logger.info("[ Allocation ][ Loc ] outbound reallocate success - orderKey={}, newFromLocId={}",
                order.getOrderKey(), newFromLocId);
        return newFromLocId;
    }

    // 출고 포트 결정 - 지정 toLocId 우선, 없으면 동적 할당 + 카운트 증가
    private String resolveOutboundPort(WcsOrderCommand command, String eqGroupId, String fromLocId) {
        if (ValueUtil.isNotEmpty(command.getToLocId())) {
            return command.getToLocId();
        }
        String toLocId = portAllocator.allocateBestPort(eqGroupId, command.getWcsOrderKey());
        if (ValueUtil.isEmpty(toLocId)) {
            logger.warn("[ Allocation ][ Loc ] outbound port none - hostOrderKey={}, fromLoc={}",
                    command.getHostOrderKey(), fromLocId);
            return null;
        }
        locationRepository.incrementActiveTaskCount(eqGroupId, toLocId);
        return toLocId;
    }

    // 출고 대상 재고 조회 - fromLocId 지정 시 해당 로케이션, 아니면 동적 산출
    private List<TbInventoryStock> resolveOutboundStocks(WcsOrderCommand command, String eqGroupId) {
        if (ValueUtil.isNotEmpty(command.getFromLocId())) {
            logger.info("[ Allocation ][ Loc ] outbound designated - eqGroupId={}, fromLocId={}", eqGroupId, command.getFromLocId());
            // 지정 출고: stock_status 가 IDLE 이든 HOST_PENDING 이든 모두 후보
            return new ArrayList<>(stockRepository.findByEqGroupIdAndLocId(eqGroupId, command.getFromLocId()));
        }

        // 자동 FIFO 산출: HOST_PENDING 은 자동 제외 (다른 host_order 가 이미 잡아둠)
        try {
            return inventoryOutboundStockService.calculateOutboundStock(buildRequest(command));
        } catch (Exception e) {
            logger.error("[ Allocation ][ Loc ] outbound calculate failed - eqGroupId={}, hostOrderKey={}",
                    eqGroupId, command.getHostOrderKey(), e);
            return new ArrayList<>();
        }
    }

    // TbInventoryStock 리스트 → AllocationResult.Item 리스트 변환
    private List<AllocationResult.Item> toAllocationItems(List<TbInventoryStock> stocks) {
        List<AllocationResult.Item> items = new ArrayList<>();
        for (TbInventoryStock stock : stocks) {
            AllocationResult.Item ai = new AllocationResult.Item();
            ai.setItemCode(stock.getItemCode());
            ai.setLotNo(stock.getLotNo());
            ai.setQty(nz(stock.getItemQty()));
            items.add(ai);
        }
        return items;
    }

    // 로케이션 실재고 vs 출고 수량(SKU+LOT 키) 비교해 잔여 판단
    // 잔여 > 0 이면 LocationAllocation 에 partialPicking 플래그 + remainingStocks 스냅샷 채움
    private void detectPartialPicking(AllocationResult.LocationAllocation locAlloc,
                                      String eqGroupId,
                                      String locId,
                                      List<AllocationResult.Item> allocatedItems) {
        if (ValueUtil.isEmpty(locId)) return;

        // 현재 로케이션 재고 조회
        List<ExtTbInventoryStock> currentStocks;
        try {
            currentStocks = stockRepository.findByEqGroupIdAndLocId(eqGroupId, locId);
        } catch (Exception e) {
            logger.error("[ Allocation ][ Loc ] partial detect failed - locId={}", locId, e);
            return;
        }
        if (ValueUtil.isEmpty(currentStocks)) return;

        // SKU+LOT 키 기준으로 재고 합산
        Map<String, Integer> remaining = new HashMap<>();
        for (ExtTbInventoryStock s : currentStocks) {
            if (ValueUtil.isEmpty(s)) continue;
            remaining.merge(keyOf(s.getItemCode(), s.getLotNo()), nz(s.getItemQty()), Integer::sum);
        }

        // 출고 수량만큼 차감
        if (ValueUtil.isNotEmpty(allocatedItems)) {
            for (AllocationResult.Item a : allocatedItems) {
                if (ValueUtil.isEmpty(a)) continue;
                remaining.merge(keyOf(a.getItemCode(), a.getLotNo()), -a.getQty(), Integer::sum);
            }
        }

        // 잔여 > 0 인 키만 추출
        List<AllocationResult.Item> remainingList = new ArrayList<>();
        int totalRemaining = 0;
        for (Map.Entry<String, Integer> e : remaining.entrySet()) {
            if (ValueUtil.isEmpty(e.getValue()) || e.getValue() <= 0) continue;
            String[] parts = e.getKey().split("\\" + KEY_DELIMITER, 2);
            AllocationResult.Item r = new AllocationResult.Item();
            r.setItemCode(parts[0]);
            r.setLotNo(parts.length > 1 && ValueUtil.isNotEmpty(parts[1]) ? parts[1] : null);
            r.setQty(e.getValue());
            remainingList.add(r);
            totalRemaining += e.getValue();
        }

        // 잔여가 있으면 LocationAllocation 에 마킹
        if (ValueUtil.isNotEmpty(remainingList)) {
            locAlloc.setPartialPicking(true);
            locAlloc.setRemainingQty(totalRemaining);
            locAlloc.setRemainingStocks(remainingList);
            logger.info("[ Allocation ][ Loc ] putback detected - locId={}, remainingItems={}, remainingQty={}",
                    locId, remainingList.size(), totalRemaining);
        }
    }

    // stockId → 로케이션 ID 해석
    private String resolveLocIdByStockId(String eqGroupId, String stockId) {
        ExtTbInventoryLocation loc = locationRepository.findByStockId(eqGroupId, stockId);
        return ValueUtil.isNotEmpty(loc) ? loc.getLocId() : null;
    }

    // WcsOrderCommand 기반 출고 재고 산출 요청 DTO 구성
    private OutboundStockRequestDto buildRequest(WcsOrderCommand command) {
        OutboundStockRequestDto dto = new OutboundStockRequestDto();
        dto.setLocGroup(command.getEqGroupId());

        List<ItemIdentifierDto> itemList = new ArrayList<>();
        if (ValueUtil.isNotEmpty(command.getItems())) {
            for (WcsOrderCommand.Item item : command.getItems()) {
                itemList.add(toItemIdentifier(command.getOwnerCode(), item.getItemCode(), item.getQty()));
            }
        }
        dto.setItemList(itemList);
        dto.setOutboundCalculateStrategy(OutboundCalculateStrategy.FIFO);
        return dto;
    }

    // ItemIdentifierDto 생성 헬퍼 (반복 코드 제거용)
    private ItemIdentifierDto toItemIdentifier(String ownerCode, String itemCode, Integer qty) {
        ItemIdentifierDto dto = new ItemIdentifierDto();
        dto.setItemOwner(ownerCode);
        dto.setItemCode(itemCode);
        dto.setItemQty(qty);
        return dto;
    }

    // SKU + LOT 결합 키 생성
    private static String keyOf(String sku, String lotNo) {
        return nullToEmpty(sku) + KEY_DELIMITER + nullToEmpty(lotNo);
    }
}
