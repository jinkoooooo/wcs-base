package operato.logis.wcs.service.impl.pallet;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.BoxStatus;
import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.consts.ShuttleOrderStatus;
import operato.logis.wcs.entity.ExtTbInventoryItemMaster;
import operato.logis.wcs.entity.TbWcsHostOrder;
import operato.logis.wcs.entity.TbWcsHostOrderItem;
import operato.logis.wcs.entity.TbWcsPalletBox;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.service.repository.HostOrderItemRepository;
import operato.logis.wcs.service.repository.HostOrderRepository;
import operato.logis.wcs.service.repository.InventoryItemMasterRepository;
import operato.logis.wcs.service.repository.PalletBoxRepository;
import operato.logis.wcs.service.repository.ShuttleOrderRepository;
import org.springframework.stereotype.Component;
import xyz.elidom.exception.server.ElidomRuntimeException;

import static operato.logis.wcs.common.util.check.Validator.requireFound;
import static operato.logis.wcs.common.util.check.Validator.requireNotEmpty;
import xyz.elidom.util.ValueUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 입고 전(pre-inbound) 상태 검증과 박스 합계 정합성 검증을 담당.
 *
 * 입고 전이란 — 해당 파렛트에 INBOUND 모드의 셔틀 주문이 없거나, 있어도 CREATED 상태
 * (아직 ECS 송신 전) 인 상태. 박스 수량 수정 / 추가 / DRAFT 박스 삭제는 이 상태에서만 허용된다.
 *
 * 박스 합계 정합성 — (item_code, lot_no) 그룹별 박스 total_qty 합이 host_order_item 의
 * EA 수량과 정확히 일치해야 한다. 변경 트랜잭션 내부에서 호출되어 mismatch 시
 * ElidomRuntimeException 으로 롤백을 유도한다.
 */
@Component
@RequiredArgsConstructor
public class PalletBoxPreInboundGuard {

    private final PalletBoxRepository boxRepository;
    private final HostOrderRepository hostOrderRepository;
    private final HostOrderItemRepository hostOrderItemRepository;
    private final ShuttleOrderRepository shuttleOrderRepository;
    private final InventoryItemMasterRepository itemMasterRepository;
    private final PalletBoxFactory palletBoxFactory;

    /**
     * 파렛트가 입고 전 상태인지 검증.
     * INBOUND 셔틀 주문이 존재하고 CREATED 보다 진행됐다면 예외.
     */
    public void ensurePreInboundState(String palletBarcode) {
        requireNotEmpty(palletBarcode, "INVALID_PARAMETER", "파렛트 바코드가 비어있습니다.");
        // 파렛트의 INBOUND 셔틀 주문 중 CREATED 이상·종료 전 상태가 있으면 거부
        for (TbWcsShuttleOrder so : shuttleOrderRepository.findByBarcode(palletBarcode)) {
            if (!OrderType.INBOUND.matches(so.getOrderType())) continue;
            Integer st = so.getOrderStatus();
            if (ValueUtil.isEmpty(st)) continue;
            int created = ShuttleOrderStatus.CREATED.code();
            if (st > created && !ShuttleOrderStatus.isFinalStatus(st)) {
                throw new ElidomRuntimeException("PALLET_NOT_PRE_INBOUND",
                        "이미 입고 작업이 진행 중인 파렛트입니다. 박스 추가/수정/삭제는 입고 시작 전에만 가능합니다. (셔틀주문: %s, 상태: %s)"
                                .formatted(so.getOrderKey(), st));
            }
        }
    }

    /**
     * (item_code, lot_no) 그룹별 박스 total_qty 합이 host_order_item EA 수량과 정확히 일치하는지 검증.
     * VOID 박스는 합계에서 제외. 박스에만 있는 (item, lot) 그룹이 있으면 예외.
     */
    public void validateBoxSumMatchesHostOrder(String palletBarcode) {
        requireNotEmpty(palletBarcode, "INVALID_PARAMETER", "파렛트 바코드가 비어있습니다.");

        // 박스 전수 로드 — 빈 파렛트는 검증 자체 불가
        List<TbWcsPalletBox> all = boxRepository.findByPalletBarcode(palletBarcode);
        requireFound(all, "BOX_NOT_FOUND", "해당 파렛트에 박스가 없습니다. (파렛트: " + palletBarcode + ")");

        // (itemCode, lotNo) 그룹별 sum(totalQty) — VOID 박스 제외
        Map<ItemLotKey, Integer> sumByKey = new HashMap<>();
        String hostOrderKey = null;
        for (TbWcsPalletBox b : all) {
            if (BoxStatus.fromCode(b.getBoxStatus()) == BoxStatus.VOID) continue;
            if (hostOrderKey == null && ValueUtil.isNotEmpty(b.getHostOrderKey())) {
                hostOrderKey = b.getHostOrderKey();
            }
            ItemLotKey key = new ItemLotKey(b.getItemCode(), b.getLotNo());
            int total = ValueUtil.isEmpty(b.getTotalQty()) ? 0 : b.getTotalQty();
            sumByKey.merge(key, total, Integer::sum);
        }

        // 호스트 주문 식별 불가 — 박스 데이터 정합성 깨짐
        requireFound(hostOrderKey, "HOST_ORDER_NOT_FOUND", "박스의 호스트 주문을 식별할 수 없습니다. (파렛트: " + palletBarcode + ")");

        // 호스트 주문 + 아이템 로드
        TbWcsHostOrder host = hostOrderRepository.findByHostOrderKey(hostOrderKey);
        requireFound(host, "HOST_ORDER_NOT_FOUND", "호스트 주문을 찾을 수 없습니다. (host_order_key: " + hostOrderKey + ")");
        List<TbWcsHostOrderItem> items = hostOrderItemRepository.findByHostOrderKey(
                host.getHostSystemCode(), hostOrderKey);
        if (ValueUtil.isEmpty(items)) {
            throw new ElidomRuntimeException("HOST_ORDER_ITEMS_NOT_FOUND",
                    "호스트 주문 아이템이 없습니다. (host_order_key: " + hostOrderKey + ")");
        }

        // 주문 아이템별 기대 EA 와 박스 합계 비교
        Set<ItemLotKey> expectedKeys = new HashSet<>();
        for (TbWcsHostOrderItem item : items) {
            ExtTbInventoryItemMaster master = itemMasterRepository.findByOwnerAndCode(
                    host.getOwnerCode(), item.getItemCode());
            int expected = palletBoxFactory.toEaQty(master, item);
            ItemLotKey key = new ItemLotKey(item.getItemCode(), item.getLotNo());
            expectedKeys.add(key);
            int actual = sumByKey.getOrDefault(key, 0);
            if (actual != expected) {
                throw new ElidomRuntimeException("BOX_SUM_MISMATCH",
                        String.format("박스 합계 불일치: %s/%s - 현재 %dEA, 주문 %dEA (차이 %dEA)",
                                key.itemCode(), key.lotNo(), actual, expected, expected - actual));
            }
        }

        // 박스에만 있는 (item, lot) — 주문에 없는 품목이 박스로 추가됐을 경우
        for (Map.Entry<ItemLotKey, Integer> e : sumByKey.entrySet()) {
            if (!expectedKeys.contains(e.getKey())) {
                throw new ElidomRuntimeException("BOX_SUM_MISMATCH",
                        String.format("주문에 없는 박스 그룹: %s/%s - 박스 합계 %dEA",
                                e.getKey().itemCode(), e.getKey().lotNo(), e.getValue()));
            }
        }
    }

    /** (itemCode, lotNo) 합계 그룹 키 — null 은 빈 문자열로 정규화. */
    private record ItemLotKey(String itemCode, String lotNo) {
        ItemLotKey(String itemCode, String lotNo) {
            this.itemCode = itemCode == null ? "" : itemCode;
            this.lotNo = lotNo == null ? "" : lotNo;
        }
    }
}
