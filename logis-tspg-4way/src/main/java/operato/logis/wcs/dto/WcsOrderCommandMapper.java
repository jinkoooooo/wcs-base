package operato.logis.wcs.dto;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.HostOrderType;
import operato.logis.wcs.consts.UomType;
import operato.logis.wcs.consts.WcsError;
import operato.logis.wcs.entity.ExtTbInventoryItemMaster;
import operato.logis.wcs.entity.TbWcsHostOrder;
import operato.logis.wcs.entity.TbWcsHostOrderItem;
import operato.logis.wcs.service.repository.InventoryItemMasterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 외부 요청 DTO를 내부 공통 처리 모델(WcsOrderCommand)로 변환한다.
 */
@Service
@RequiredArgsConstructor
public class WcsOrderCommandMapper {

    private static final Logger logger = LoggerFactory.getLogger(WcsOrderCommandMapper.class);

    private final InventoryItemMasterRepository inventoryItemMasterRepository;

    /** HOST 수신 요청 → WcsOrderCommand (EA 환산 포함) */
    public WcsOrderCommand fromHostRequest(HostOrderApi.Request request) {
        if (ValueUtil.isEmpty(request)) return null;

        List<WcsOrderCommand.Item> commandItems = toCommandItemsFromHostRequest(
                request.getOwnerCode(), request.getItems());

        HostOrderType hostType = HostOrderType.from(request.getOrderType());
        String baseType = ValueUtil.isNotEmpty(hostType)
                ? hostType.baseOrderType().codeAsString() : request.getOrderType();
        String subType = hostType != null && hostType.subOrderType() != null
                ? hostType.subOrderType().code() : null;

        return WcsOrderCommand.builder()
                .hostSystemCode(request.getHostSystemCode())
                .hostOrderKey(request.getHostOrderKey())
                .orderType(baseType)
                .subOrderType(subType)
                .ownerCode(request.getOwnerCode())
                .eqGroupId(request.getEqGroupId())
                .priority(ValueUtil.isNotEmpty(request.getPriority()) ? request.getPriority() : 5)
                .fromLocId(request.getFromLocId())
                .toLocId(request.getToLocId())
                .rawPayload(request.getRawPayload())
                .barCode(request.getBarcode())
                .persistHostOrder(true)
                .items(commandItems)
                .build();
    }

    /** DB 엔티티 → WcsOrderCommand (재산출/복구용, EA 환산 포함) */
    public WcsOrderCommand fromHostOrder(TbWcsHostOrder hostOrder, List<TbWcsHostOrderItem> items) {
        if (ValueUtil.isEmpty(hostOrder)) return null;

        List<WcsOrderCommand.Item> commandItems = toCommandItemsFromHostEntities(
                hostOrder.getOwnerCode(), items);

        // host_order.order_type 은 HostOrderType (확장 카테고리) 코드.
        // shuttle_order.order_type 은 ECS 호환 base type, sub_order_type 으로 카테고리 분기.
        HostOrderType hostType = HostOrderType.from(hostOrder.getOrderType());
        String baseType = ValueUtil.isNotEmpty(hostType)
                ? hostType.baseOrderType().codeAsString() : hostOrder.getOrderType();
        String subType = hostType != null && hostType.subOrderType() != null
                ? hostType.subOrderType().code() : null;

        return WcsOrderCommand.builder()
                .hostSystemCode(hostOrder.getHostSystemCode())
                .hostOrderKey(hostOrder.getHostOrderKey())
                .orderType(baseType)
                .subOrderType(subType)
                .ownerCode(hostOrder.getOwnerCode())
                .eqGroupId(hostOrder.getEqGroupId())
                .priority(hostOrder.getPriority())
                .rawPayload(hostOrder.getRawPayload())
                .barCode(hostOrder.getBarcode())
                .persistHostOrder(false)
                .fromLocId(hostOrder.getFromLocCode())
                .toLocId(hostOrder.getToLocCode())
                .items(commandItems)
                .build();
    }

    /** HOST 요청 아이템 → Command 아이템 변환 (마스터 일괄 로드 후 EA 환산). */
    private List<WcsOrderCommand.Item> toCommandItemsFromHostRequest(
            String ownerCode, List<HostOrderApi.Item> rawItems) {

        if (ValueUtil.isEmpty(rawItems)) return new ArrayList<>();

        Map<String, ExtTbInventoryItemMaster> masterMap = loadMasterMap(ownerCode,
                rawItems.stream()
                        .filter(i -> ValueUtil.isNotEmpty(i) && ValueUtil.isNotEmpty(i.getItemCode()))
                        .map(HostOrderApi.Item::getItemCode)
                        .distinct()
                        .collect(Collectors.toList()));

        List<WcsOrderCommand.Item> result = new ArrayList<>();
        for (HostOrderApi.Item item : rawItems) {
            if (ValueUtil.isEmpty(item)) continue;

            int originalQty = ValueUtil.isEmpty(item.getQty()) ? 0 : item.getQty();
            UomType originalUom = UomType.fromOrDefault(item.getUom());
            int eaQty = convertToEa(ownerCode, item.getItemCode(), originalUom, originalQty, masterMap);

            result.add(WcsOrderCommand.Item.builder()
                    .itemCode(item.getItemCode())
                    .lotNo(item.getLotNo())
                    .qty(eaQty)
                    .uom(UomType.EA.code())
                    .issueQty(originalQty)
                    .issueUom(originalUom.code())
                    .produceDate(item.getProduceDate())
                    .expiryDate(item.getExpiryDate())
                    .rawAttr(item.getRawAttr())
                    .testRequestNo(item.getTestRequestNo())
                    .testNo(item.getTestNo())
                    .testStatus(null)
                    .build());
        }
        return result;
    }

    /** DB 엔티티 아이템 → Command 아이템 변환 (마스터 일괄 로드 후 EA 환산). */
    private List<WcsOrderCommand.Item> toCommandItemsFromHostEntities(
            String ownerCode, List<TbWcsHostOrderItem> rawItems) {

        if (ValueUtil.isEmpty(rawItems)) return new ArrayList<>();

        Map<String, ExtTbInventoryItemMaster> masterMap = loadMasterMap(ownerCode,
                rawItems.stream()
                        .filter(i -> ValueUtil.isNotEmpty(i) && ValueUtil.isNotEmpty(i.getItemCode()))
                        .map(TbWcsHostOrderItem::getItemCode)
                        .distinct()
                        .collect(Collectors.toList()));

        List<WcsOrderCommand.Item> result = new ArrayList<>();
        for (TbWcsHostOrderItem item : rawItems) {
            if (ValueUtil.isEmpty(item)) continue;

            int originalQty = item.getQty();
            UomType originalUom = UomType.fromOrDefault(item.getUom());
            int eaQty = convertToEa(ownerCode, item.getItemCode(), originalUom, originalQty, masterMap);

            result.add(WcsOrderCommand.Item.builder()
                    .itemCode(item.getItemCode())
                    .lotNo(item.getLotNo())
                    .qty(eaQty)
                    .uom(UomType.EA.code())
                    .issueQty(originalQty)
                    .issueUom(originalUom.code())
                    .produceDate(item.getProduceDate())
                    .expiryDate(item.getExpiryDate())
                    .rawAttr(item.getRawAttr())
                    .testRequestNo(item.getTestRequestNo())
                    .testNo(item.getTestNo())
                    .testStatus(item.getTestStatus())
                    .build());
        }
        return result;
    }

    /** owner + sku 목록으로 아이템 마스터를 Map 으로 일괄 로드. */
    private Map<String, ExtTbInventoryItemMaster> loadMasterMap(String ownerCode, List<String> skuCodes) {
        if (ValueUtil.isEmpty(skuCodes)) return Map.of();
        return inventoryItemMasterRepository.findAsMapByOwnerAndCodes(ownerCode, skuCodes);
    }

    /** 요청 UOM 수량을 EA 로 환산. EA 면 그대로, 그 외는 마스터의 입수량으로 변환. */
    private int convertToEa(String ownerCode, String itemCode,
                            UomType uom, int qty,
                            Map<String, ExtTbInventoryItemMaster> masterMap) {

        if (qty <= 0) {
            throw new ElidomRuntimeException(
                    WcsError.INVALID_ORDER_ITEM.codeAsString(),
                    String.format("수량이 0 이하: sku=%s, qty=%d", itemCode, qty));
        }

        if (uom == UomType.EA) {
            return qty;
        }

        ExtTbInventoryItemMaster master = masterMap.get(itemCode);
        if (ValueUtil.isEmpty(master)) {
            throw new ElidomRuntimeException(
                    WcsError.INVALID_ORDER_ITEM.codeAsString(),
                    String.format("아이템 마스터 없음 (UOM=%s 환산 불가): owner=%s, sku=%s",
                            uom.code(), ownerCode, itemCode));
        }

        try {
            int eaQty = master.toEaQty(uom, qty);
            logger.debug("[ Order ][ Mapper ] uom converted - sku={}, qty={}{}, eaQty={}",
                    itemCode, qty, uom.code(), eaQty);
            return eaQty;
        } catch (IllegalStateException e) {
            // 스택트레이스 보존 — e 를 마지막 인자로, 재던지기 시 cause 유지
            logger.error("[ Order ][ Mapper ] EA conversion failed - sku={}, qty={}, uom={}",
                    itemCode, qty, uom.code(), e);
            throw new ElidomRuntimeException(
                    WcsError.INVALID_ORDER_ITEM.codeAsString(),
                    "EA 환산 실패", e);
        }
    }
}