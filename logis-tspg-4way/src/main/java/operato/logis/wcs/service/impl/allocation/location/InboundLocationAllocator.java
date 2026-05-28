package operato.logis.wcs.service.impl.allocation.location;

import lombok.RequiredArgsConstructor;
import operato.logis.inventory.dto.InboundLocationRequestDto;
import operato.logis.inventory.dto.ItemIdentifierDto;
import operato.logis.inventory.entity.TbInventoryLocation;
import operato.logis.inventory.service.InventoryInboundLocationService;
import operato.logis.wcs.consts.WcsError;
import operato.logis.wcs.dto.AllocationResult;
import operato.logis.wcs.dto.WcsOrderCommand;
import operato.logis.wcs.entity.ExtTbInventoryItemMaster;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.entity.TbWcsShuttleOrderItem;
import operato.logis.wcs.service.impl.allocation.port.InboundPortAllocator;
import operato.logis.wcs.service.repository.InventoryItemMasterRepository;
import operato.logis.wcs.service.repository.ShuttleOrderItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 입고 로케이션 할당기.
 *
 * 처리 순서:
 *   1. ExtTbInventoryItemMaster 에서 itemType / totalWeight 조회
 *   2. InventoryInboundLocationService.calculateInboundLocation → 입고 목적지(랙) 산출
 *   3. InboundPortAllocator.allocateBestPort → 입고 출발지(포트) 산출
 *
 * 포트는 락하지 않는다 (여러 주문 순차 투입 가능).
 * 방해물 산출은 호출자에서 락 이후 별도 수행.
 */
@Service
@RequiredArgsConstructor
public class InboundLocationAllocator {

    private static final Logger logger = LoggerFactory.getLogger(InboundLocationAllocator.class);

    // 기본 아이템 타입 (마스터에서 못 찾았을 때 fallback)
    private static final String DEFAULT_ITEM_TYPE = "PALLET";

    private final InventoryInboundLocationService inventoryInboundLocationService;
    private final InventoryItemMasterRepository itemMasterRepository;
    private final InboundPortAllocator inboundPortAllocator;
    private final ShuttleOrderItemRepository shuttleOrderItemRepository;

    /**
     * 입고 목적지(랙) + 출발지(포트) 산출.
     * toLoc 이 지정되어 있으면 자동 산출은 스킵.
     */
    @Transactional(rollbackFor = Exception.class)
    public AllocationResult allocate(WcsOrderCommand command) {
        logger.info("[ Allocation ][ Loc ] inbound start - hostOrderKey={}", command.getHostOrderKey());
        String eqGroupId = command.getEqGroupId();
        String toLocId = command.getToLocId();

        // toLoc 지정된 경우 자동 산출 스킵 (host 단계에서 이미 정해진 위치 사용)
        if (ValueUtil.isEmpty(toLocId)) {

            // 자동 산출
            InboundLocationRequestDto requestDto = buildRequest(command);
            TbInventoryLocation resultLoc;
            try {
                resultLoc = inventoryInboundLocationService.calculateInboundLocation(requestDto);
            } catch (Exception e) {
                logger.error("[ Allocation ][ Loc ] inbound calc failed - eqGroupId={}, hostOrderKey={}",
                        eqGroupId, command.getHostOrderKey(), e);
                return AllocationResult.fail(
                        WcsError.NO_AVAILABLE_LOCATION.codeAsString(),
                        "입고 위치 계산 실패: " + e.getMessage());
            }

            // 산출 실패 시 fail 반환
            if (ValueUtil.isEmpty(resultLoc)) {
                logger.warn("[ Allocation ][ Loc ] inbound no available rack - eqGroupId={}", eqGroupId);
                return AllocationResult.fail(
                        WcsError.NO_AVAILABLE_LOCATION.codeAsString(),
                        "조건에 맞는 입고 로케이션이 없습니다.");
            }
            toLocId = resultLoc.getLocId();
        } else {
            logger.info("[ Allocation ][ Loc ] inbound toLocId pre-reserved (host stage) - toLocId={}", toLocId);
        }

        logger.info("[ Allocation ][ Loc ] inbound rack confirmed - toLocId={}", toLocId);

        // 출발지(포트) 산출 - 지정값 우선, 없으면 동적 할당
        String fromLocId = command.getFromLocId();
        if (ValueUtil.isEmpty(fromLocId)) {
            fromLocId = inboundPortAllocator.allocateBestPort(eqGroupId);
        }

        // 가용 입고 포트 없으면 실패
        if (ValueUtil.isEmpty(fromLocId)) {
            logger.warn("[ Allocation ][ Loc ] inbound no available port - eqGroupId={}", eqGroupId);
            return AllocationResult.fail(
                    WcsError.NO_AVAILABLE_LOCATION.codeAsString(),
                    "가용 입고 포트가 없습니다. eqGroupId=" + eqGroupId);
        }

        logger.info("[ Allocation ][ Loc ] inbound success - from(port)={}, to(rack)={}", fromLocId, toLocId);
        return AllocationResult.success(fromLocId, toLocId, eqGroupId);
    }

    /**
     * 이중입고 복구용 재산출.
     * 에러 발생 로케이션은 이미 task_id='DOUBLE_IN' 마킹되어 재고 모듈 쿼리에서 자연 배제된다.
     */
    @Transactional(rollbackFor = Exception.class)
    public String reallocate(TbWcsShuttleOrder order) {
        logger.info("[ Allocation ][ Loc ] inbound reallocate start - orderKey={}", order.getOrderKey());

        // 재산출용 DTO 구성 (오더 아이템 기반)
        InboundLocationRequestDto dto = new InboundLocationRequestDto();
        dto.setLocGroup(order.getEqGroupId());

        // 오더 아이템 조회 + ItemIdentifier 변환 + itemType 결정
        List<TbWcsShuttleOrderItem> items = shuttleOrderItemRepository.findByOrderKey(order.getOrderKey());
        List<ItemIdentifierDto> itemList = new ArrayList<>();
        String itemType = DEFAULT_ITEM_TYPE;

        if (ValueUtil.isNotEmpty(items)) {

            // 첫 아이템 기준으로 itemType 결정
            TbWcsShuttleOrderItem firstItem = items.get(0);
            ExtTbInventoryItemMaster master =
                    itemMasterRepository.findByOwnerAndCode(order.getOwnerCode(), firstItem.getItemCode());
            if (ValueUtil.isNotEmpty(master) && ValueUtil.isNotEmpty(master.getItemType())) {
                itemType = master.getItemType();
            }

            // 전체 아이템 → ItemIdentifierDto 변환
            for (TbWcsShuttleOrderItem item : items) {
                itemList.add(toItemIdentifier(order.getOwnerCode(), item.getItemCode(), item.getQty()));
            }
        }

        dto.setItemList(itemList);
        dto.setItemType(itemType);
        dto.setTotalWeight(1);
        dto.setTotalHeight(1);

        // 재산출 시도
        try {
            TbInventoryLocation resultLoc = inventoryInboundLocationService.calculateInboundLocation(dto);
            if (ValueUtil.isNotEmpty(resultLoc) && ValueUtil.isNotEmpty(resultLoc.getLocId())) {
                logger.info("[ Allocation ][ Loc ] inbound reallocate success - newToLocId={}", resultLoc.getLocId());
                return resultLoc.getLocId();
            }
        } catch (Exception e) {
            logger.error("[ Allocation ][ Loc ] inbound reallocate failed - orderKey={}", order.getOrderKey(), e);
        }
        return null;
    }

    // WcsOrderCommand 기반 입고 위치 산출 요청 DTO 구성
    private InboundLocationRequestDto buildRequest(WcsOrderCommand command) {
        InboundLocationRequestDto dto = new InboundLocationRequestDto();
        dto.setLocGroup(command.getEqGroupId());

        List<ItemIdentifierDto> itemList = new ArrayList<>();
        int totalWeight = 0;
        String itemType = DEFAULT_ITEM_TYPE;

        if (ValueUtil.isNotEmpty(command.getItems())) {

            // 중복 제거된 sku 코드 목록 추출
            List<String> skuCodes = command.getItems().stream()
                    .filter(i -> ValueUtil.isNotEmpty(i) && ValueUtil.isNotEmpty(i.getItemCode()))
                    .map(WcsOrderCommand.Item::getItemCode)
                    .distinct()
                    .toList();

            // 아이템 마스터 일괄 조회
            Map<String, ExtTbInventoryItemMaster> masterMap =
                    itemMasterRepository.findAsMapByOwnerAndCodes(command.getOwnerCode(), skuCodes);

            // 첫 아이템 기준으로 itemType 결정
            WcsOrderCommand.Item firstItem = command.getItems().get(0);
            if (ValueUtil.isNotEmpty(firstItem) && ValueUtil.isNotEmpty(firstItem.getItemCode())) {
                ExtTbInventoryItemMaster firstMaster = masterMap.get(firstItem.getItemCode());
                if (ValueUtil.isNotEmpty(firstMaster) && ValueUtil.isNotEmpty(firstMaster.getItemType())) {
                    itemType = firstMaster.getItemType();
                }
            }

            // 아이템별 ItemIdentifierDto 변환 + 총 무게 계산
            for (WcsOrderCommand.Item item : command.getItems()) {
                if (ValueUtil.isEmpty(item)) continue;

                itemList.add(toItemIdentifier(command.getOwnerCode(), item.getItemCode(), item.getQty()));

                ExtTbInventoryItemMaster master = masterMap.get(item.getItemCode());
                if (ValueUtil.isNotEmpty(master) && ValueUtil.isNotEmpty(item.getQty())) {
                    double weightKg = master.calculateWeightByEa(item.getQty());
                    totalWeight += (int) Math.ceil(weightKg);
                }
            }
        }

        dto.setItemList(itemList);
        dto.setItemType(itemType);
        dto.setTotalWeight(Math.max(totalWeight, 1));
        dto.setTotalHeight(1);
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
}
