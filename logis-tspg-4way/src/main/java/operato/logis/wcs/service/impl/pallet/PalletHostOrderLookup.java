package operato.logis.wcs.service.impl.pallet;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.common.util.lang.CommonUtils;
import operato.logis.wcs.entity.ExtTbInventoryItemMaster;
import operato.logis.wcs.entity.TbWcsHostOrder;
import operato.logis.wcs.entity.TbWcsHostOrderItem;
import operato.logis.wcs.entity.TbWcsPalletBox;
import operato.logis.wcs.service.repository.HostOrderItemRepository;
import operato.logis.wcs.service.repository.HostOrderRepository;
import operato.logis.wcs.service.repository.InventoryItemMasterRepository;
import operato.logis.wcs.service.repository.PalletBoxRepository;
import org.springframework.stereotype.Service;
import xyz.elidom.exception.server.ElidomRuntimeException;

import static operato.logis.wcs.common.util.check.Validator.requireFound;
import static operato.logis.wcs.common.util.check.Validator.requireNotEmpty;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 파렛트의 호스트 주문/아이템 조회 (Lookup).
 *
 * 박스 편집 시 (item, lot) 검증·EA 환산에 필요한 host_order / host_order_item 을 해석한다.
 * 상태 변경은 수행하지 않는다 — 읽기 전용.
 */
@Service
@RequiredArgsConstructor
public class PalletHostOrderLookup {

    private final PalletBoxRepository boxRepository;
    private final HostOrderRepository hostOrderRepository;
    private final HostOrderItemRepository hostOrderItemRepository;
    private final InventoryItemMasterRepository itemMasterRepository;
    private final PalletBoxFactory palletBoxFactory;

    /** 파렛트의 호스트 주문 해석 — 기존 박스에서 hostOrderKey 추출 후 조회. */
    TbWcsHostOrder resolveHostOrder(String palletBarcode) {
        List<TbWcsPalletBox> existing = boxRepository.findByPalletBarcode(palletBarcode);
        requireFound(existing, "BOX_NOT_FOUND", "해당 파렛트에 기존 박스가 없습니다. (파렛트: " + palletBarcode + ")");
        String hostOrderKey = existing.stream()
                .map(TbWcsPalletBox::getHostOrderKey)
                .filter(ValueUtil::isNotEmpty)
                .findFirst()
                .orElseThrow(() -> new ElidomRuntimeException("HOST_ORDER_NOT_FOUND",
                        "박스의 호스트 주문을 식별할 수 없습니다. (파렛트: " + palletBarcode + ")"));
        TbWcsHostOrder host = hostOrderRepository.findByHostOrderKey(hostOrderKey);
        requireFound(host, "HOST_ORDER_NOT_FOUND", "호스트 주문을 찾을 수 없습니다. (host_order_key: " + hostOrderKey + ")");
        return host;
    }

    /** 호스트 주문의 아이템 목록. */
    List<TbWcsHostOrderItem> hostItemsOf(TbWcsHostOrder host) {
        return hostOrderItemRepository.findByHostOrderKey(host.getHostSystemCode(), host.getHostOrderKey());
    }

    /** (item, lot) 으로 host_order_item 매칭. 없으면 ITEM_LOT_NOT_IN_ORDER. */
    static TbWcsHostOrderItem matchHostItem(List<TbWcsHostOrderItem> items, String itemCode, String lotNo) {
        return items.stream()
                .filter(it -> Objects.equals(it.getItemCode(), itemCode))
                .filter(it -> Objects.equals(CommonUtils.nullToEmpty(it.getLotNo()), CommonUtils.nullToEmpty(lotNo)))
                .findFirst()
                .orElseThrow(() -> new ElidomRuntimeException("ITEM_LOT_NOT_IN_ORDER",
                        "주문에 없는 품목/Lot 입니다. (item: " + itemCode + ", lot: " + lotNo + ")"));
    }

    /** "박스 추가" 모달용 — host_order_item 목록을 EA 수량과 함께 반환. */
    List<Map<String, Object>> listHostItemsForPallet(String palletBarcode) {
        requireNotEmpty(palletBarcode, "INVALID_PARAMETER", "파렛트 바코드가 비어있습니다.");

        // 박스가 없으면 빈 리스트 — 신규 파렛트 응답
        List<TbWcsPalletBox> boxes = boxRepository.findByPalletBarcode(palletBarcode);
        if (ValueUtil.isEmpty(boxes)) return List.of();

        String hostOrderKey = boxes.stream()
                .map(TbWcsPalletBox::getHostOrderKey)
                .filter(ValueUtil::isNotEmpty)
                .findFirst()
                .orElse(null);
        if (ValueUtil.isEmpty(hostOrderKey)) return List.of();

        TbWcsHostOrder host = hostOrderRepository.findByHostOrderKey(hostOrderKey);
        if (ValueUtil.isEmpty(host)) return List.of();

        // 각 host_order_item 에 대해 EA 환산 후 행 빌드
        List<TbWcsHostOrderItem> items = hostOrderItemRepository.findByHostOrderKey(
                host.getHostSystemCode(), hostOrderKey);

        List<Map<String, Object>> out = new ArrayList<>();
        for (TbWcsHostOrderItem it : items) {
            ExtTbInventoryItemMaster master = itemMasterRepository.findByOwnerAndCode(
                    host.getOwnerCode(), it.getItemCode());
            int eaQty = palletBoxFactory.toEaQty(master, it);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("itemCode", it.getItemCode());
            row.put("lotNo", it.getLotNo());
            row.put("qty", eaQty);
            row.put("uom", "EA");
            row.put("produceDate", it.getProduceDate());
            row.put("expiryDate", it.getExpiryDate());
            out.add(row);
        }
        return out;
    }
}
