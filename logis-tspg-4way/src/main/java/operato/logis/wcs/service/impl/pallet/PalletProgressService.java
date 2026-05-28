package operato.logis.wcs.service.impl.pallet;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.BoxStatus;
import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.consts.ShuttleOrderStatus;
import operato.logis.wcs.consts.SubOrderType;
import operato.logis.wcs.entity.TbWcsPalletBox;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.entity.TbWcsShuttleOrderItem;
import operato.logis.wcs.service.impl.order.lookup.OrderLookupUtils;
import operato.logis.wcs.service.repository.PalletBoxRepository;
import operato.logis.wcs.service.repository.ShuttleOrderItemRepository;
import operato.logis.wcs.service.repository.ShuttleOrderRepository;
import org.springframework.stereotype.Service;
import xyz.elidom.exception.server.ElidomRuntimeException;

import static operato.logis.wcs.common.util.check.Validator.requireNotEmpty;
import xyz.elidom.util.ValueUtil;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 박스/출고 진행률 조회 + 파렛트 박스 목록 조회 (Lookup).
 *
 * 출고 진행률은 박스 picked_qty 직접 합산으로 계산. 차분 공식은 사용하지 않는다.
 * 상태 변경·재고 차감은 OutboundFinalizer 가, 활성 작업 해석은 PalletActivityResolver 가 담당.
 */
@Service
@RequiredArgsConstructor
public class PalletProgressService {

    private final PalletBoxRepository boxRepository;
    private final ShuttleOrderRepository shuttleOrderRepository;
    private final ShuttleOrderItemRepository shuttleOrderItemRepository;
    private final OrderLookupUtils orderLookup;

    /**
     * 입고 진행률 — VOID/DEPLETED 제외 박스의 SCANNED 비율과 잔량 합계.
     */
    public Map<String, Object> progress(String palletBarcode) {
        List<TbWcsPalletBox> boxes = boxRepository.findByPalletBarcode(palletBarcode);
        int totalBoxes = 0, scannedBoxes = 0;
        int totalQty = 0, scannedQty = 0;

        // 살아있는 박스만 집계
        for (TbWcsPalletBox b : boxes) {
            BoxStatus st = BoxStatus.fromCode(b.getBoxStatus());
            if (st == BoxStatus.VOID || st == BoxStatus.DEPLETED) continue;

            int remaining = b.calcRemainingQty();
            totalBoxes++;
            totalQty += remaining;

            if (st == BoxStatus.SCANNED) {
                scannedBoxes++;
                scannedQty += remaining;
            }
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("palletBarcode", palletBarcode);
        m.put("totalBoxes", totalBoxes);
        m.put("scannedBoxes", scannedBoxes);
        m.put("totalQty", totalQty);
        m.put("scannedQty", scannedQty);
        m.put("completed", scannedBoxes == totalBoxes && totalBoxes > 0);
        return m;
    }

    /**
     * 출고 진행률 — 박스 picked_qty 합산.
     * autoFinalize 종(SAMPLE_OUT/DISCARD/DISPOSAL/RETURN) 은 즉시 완료 응답.
     */
    public Map<String, Object> outboundProgress(String outboundOrderKey) {
        TbWcsShuttleOrder shuttle = orderLookup.getShuttleOrderOrThrow(outboundOrderKey, "출고");

        // 기대 수량 — 출고 아이템 합계
        int expectedQty = 0;
        for (TbWcsShuttleOrderItem it : shuttleOrderItemRepository.findByOrderKey(outboundOrderKey))
            expectedQty += it.getQty();

        SubOrderType sub = SubOrderType.fromOrNormal(shuttle.getSubOrderType());

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("outboundOrderKey", outboundOrderKey);
        m.put("hostOrderKey", shuttle.getHostOrderKey());
        m.put("subOrderType", shuttle.getSubOrderType());
        m.put("expectedQty", expectedQty);

        // autoFinalize — 즉시 완료 응답
        if (sub.isAutoFinalize()) {
            m.put("pickedQty", expectedQty);
            m.put("pickedBoxes", 0);
            m.put("remainingQty", 0);
            m.put("completed", expectedQty > 0);
            m.put("autoFinalize", true);
            m.put("nextAction", "AUTO_FINALIZE");
            m.put("userMessage", sub == SubOrderType.SAMPLE_OUT
                    ? "시험용 출고입니다. 도착 시 자동 완료됩니다."
                    : "시험 부적합 폐기 출고입니다.");
            return m;
        }

        // NORMAL/PARTIAL_OUT — 이번 출고에 잡힌 박스 picked_qty 직접 합산
        int pickedQty = 0;
        int pickedBoxes = 0;
        for (TbWcsPalletBox b : boxRepository.findActivePickedByOutbound(outboundOrderKey)) {
            pickedQty += b.getPickedQtyOrZero();
            pickedBoxes++;
        }

        // 결과 상태 분기 — 다음 액션 안내
        boolean completed = expectedQty > 0 && pickedQty >= expectedQty;
        m.put("pickedQty", pickedQty);
        m.put("pickedBoxes", pickedBoxes);
        m.put("remainingQty", Math.max(0, expectedQty - pickedQty));
        m.put("completed", completed);
        m.put("autoFinalize", false);
        if (completed) {
            m.put("nextAction", "FINALIZE");
            m.put("userMessage", "출고 수량이 일치합니다. 출고 확정하시겠습니까?");
        } else if (pickedQty > expectedQty) {
            m.put("nextAction", "REVIEW");
            m.put("userMessage", "스캔 수량이 출고 수량을 초과했습니다.");
        } else {
            m.put("nextAction", "SCAN");
            m.put("userMessage", "다음 박스를 스캔해주세요. (잔여 " + (expectedQty - pickedQty) + ")");
        }
        return m;
    }

    /** 파렛트 입고 스캔 완료 여부. */
    public boolean isPalletScanCompleted(String pallet) {
        return Boolean.TRUE.equals(progress(pallet).get("completed"));
    }

    /** 출고 확정 가능 여부 — autoFinalize 또는 박스 picked 합이 기대 수량 도달. */
    public boolean isOutboundFinalized(String outboundOrderKey) {
        TbWcsShuttleOrder s = shuttleOrderRepository.findByOrderKey(outboundOrderKey);
        if (ValueUtil.isEmpty(s)) return false;
        SubOrderType sub = SubOrderType.fromOrNormal(s.getSubOrderType());
        if (sub.isAutoFinalize()) return true;
        return Boolean.TRUE.equals(outboundProgress(outboundOrderKey).get("completed"));
    }

    /** 파렛트의 박스 목록. */
    public List<TbWcsPalletBox> listByPallet(String palletBarcode) {
        return boxRepository.findByPalletBarcode(palletBarcode);
    }

    /** 호스트 주문의 박스 목록. */
    public List<TbWcsPalletBox> listByHostOrder(String hostOrderKey) {
        return boxRepository.findByHostOrderKey(hostOrderKey);
    }

    /**
     * 파렛트 라이프사이클 — 셔틀 오더 전체 이력(최신순).
     * findByBarcode 가 createdAt DESC 정렬을 보장하므로 그대로 슬림 DTO 매핑.
     */
    public List<Map<String, Object>> listLifecycle(String palletBarcode) {
        requireNotEmpty(palletBarcode, "INVALID_PARAMETER", "파렛트 바코드가 입력되지 않았습니다.");

        List<TbWcsShuttleOrder> all = shuttleOrderRepository.findByBarcode(palletBarcode);
        List<Map<String, Object>> result = new java.util.ArrayList<>(all.size());
        for (TbWcsShuttleOrder s : all) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("orderKey", s.getOrderKey());
            m.put("orderType", s.getOrderType());
            m.put("subOrderType", s.getSubOrderType());
            m.put("orderStatus", s.getOrderStatus());
            m.put("fromLocCode", s.getFromLocCode());
            m.put("toLocCode", s.getToLocCode());
            m.put("parentOrderKey", s.getParentOrderKey());
            m.put("hostOrderKey", s.getHostOrderKey());
            m.put("carryingStockId", s.getCarryingStockId());
            m.put("createdAt", isoDateTime(s.getCreatedAt()));
            m.put("updatedAt", isoDateTime(s.getUpdatedAt()));
            result.add(m);
        }
        return result;
    }

    private static final DateTimeFormatter LIFECYCLE_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // Date → 시각 포함 ISO 문자열. elidom 직렬화가 Date 를 date-only 로 깎으므로 직접 포맷.
    private String isoDateTime(Date d) {
        if (d == null) return null;
        return LIFECYCLE_TS.format(d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
    }

    /**
     * 파렛트의 활성 출고 주문 — RUNNING 이상이 우선, 없으면 PENDING.
     * 활성이 0개 또는 2개 이상이면 예외.
     */
    public TbWcsShuttleOrder findActiveOutbound(String palletBarcode) {
        requireNotEmpty(palletBarcode, "INVALID_PARAMETER", "palletBarcode");
        Integer runningCode = ShuttleOrderStatus.RUNNING.codeAsIntOrNull();
        List<TbWcsShuttleOrder> all = shuttleOrderRepository.findByBarcode(palletBarcode);

        // 활성 출고 분류 — running 우선, pending 차순
        TbWcsShuttleOrder running = null, pending = null;
        int activeCount = 0;
        for (TbWcsShuttleOrder s : all) {
            Integer st = s.getOrderStatus();
            if (!ShuttleOrderStatus.isActive(st)) continue;
            if (!OrderType.OUTBOUND.matches(s.getOrderType())) continue;

            activeCount++;
            boolean isRunning = ValueUtil.isNotEmpty(runningCode) && st >= runningCode;
            if (isRunning && ValueUtil.isEmpty(running)) running = s;
            else if (!isRunning && ValueUtil.isEmpty(pending)) pending = s;
        }
        if (activeCount == 0) {
            throw new ElidomRuntimeException("NO_ACTIVE_OUTBOUND",
                    "이 파렛트에 활성 출고가 없습니다. (palletBarcode=" + palletBarcode + ")");
        }
        if (activeCount > 1) {
            throw new ElidomRuntimeException("MULTIPLE_ACTIVE_OUTBOUND",
                    "이 파렛트에 활성 출고가 둘 이상입니다. orderKey 로 직접 호출하세요.");
        }
        return ValueUtil.isNotEmpty(running) ? running : pending;
    }

    /**
     * 이번 출고에 일부만 잡힌 박스 존재 여부.
     * 관리자 우회 차단 기준 — OutboundFinalizer / OutboundAutoScanner 가 사용.
     */
    public boolean hasPartialPickedBox(String palletBarcode) {
        for (TbWcsPalletBox box : boxRepository.findByPalletBarcode(palletBarcode)) {
            BoxStatus st = BoxStatus.fromCode(box.getBoxStatus());
            if (st != BoxStatus.SCANNED) continue;
            int picked = box.getPickedQtyOrZero();
            int rem = box.calcRemainingQty();
            if (picked > 0 && picked < rem) return true;
        }
        return false;
    }
}
