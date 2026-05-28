package operato.logis.wcs.service.impl.pallet;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.BoxStatus;
import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.consts.ShuttleOrderStatus;
import operato.logis.wcs.consts.SubOrderType;
import operato.logis.wcs.entity.TbWcsPalletBox;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.service.impl.order.issuer.ReinboundIssuer;
import operato.logis.wcs.service.repository.PalletBoxRepository;
import operato.logis.wcs.service.repository.ShuttleOrderRepository;
import org.springframework.stereotype.Service;
import xyz.elidom.exception.server.ElidomRuntimeException;

import static operato.logis.wcs.common.util.check.Validator.requireNotEmpty;
import xyz.elidom.util.ValueUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 파렛트의 활성 작업 해석 (Evaluator).
 *
 * 파렛트 BCR 스캔 시 현재 어떤 작업을 보여줘야 하는지 결정.
 *   - RUNNING 출고/입고 셔틀 우선
 *   - PENDING 출고/입고 셔틀 차순
 *   - 활성 없으면 PENDING_SAMPLE 또는 POST_OUTBOUND 상태로 응답
 */
@Service
@RequiredArgsConstructor
public class PalletActivityResolver {

    private final ShuttleOrderRepository shuttleOrderRepository;
    private final PalletBoxRepository boxRepository;
    private final ReinboundIssuer reinboundIssuer;
    private final PalletProgressService progressService;

    /**
     * 파렛트의 활성 작업 해석 — 우선순위에 따라 응답 페이로드 생성.
     * 활성 셔틀 / 시험 회수 대기 / 출고 종료 후 상태 순으로 분기.
     */
    public Map<String, Object> resolveActivePallet(String palletBarcode) {
        requireNotEmpty(palletBarcode, "INVALID_PARAMETER", "파렛트 바코드가 입력되지 않았습니다.");

        // 1) 활성 셔틀 분류 — RUNNING/PENDING × OUTBOUND/INBOUND
        Integer runningCode = ShuttleOrderStatus.RUNNING.codeAsIntOrNull();
        List<TbWcsShuttleOrder> all = shuttleOrderRepository.findByBarcode(palletBarcode);

        TbWcsShuttleOrder runningOutbound = null, runningInbound = null;
        TbWcsShuttleOrder pendingOutbound = null, pendingInbound = null;

        for (TbWcsShuttleOrder s : all) {
            Integer st = s.getOrderStatus();
            if (!ShuttleOrderStatus.isActive(st)) continue;

            boolean isOutbound = OrderType.OUTBOUND.matches(s.getOrderType());
            boolean isInbound = OrderType.INBOUND.matches(s.getOrderType());
            boolean isRunning = ValueUtil.isNotEmpty(runningCode) && st >= runningCode;

            if (isOutbound && isRunning && ValueUtil.isEmpty(runningOutbound)) runningOutbound = s;
            else if (isInbound && isRunning && ValueUtil.isEmpty(runningInbound)) runningInbound = s;
            else if (isOutbound && !isRunning && ValueUtil.isEmpty(pendingOutbound)) pendingOutbound = s;
            else if (isInbound && !isRunning && ValueUtil.isEmpty(pendingInbound)) pendingInbound = s;
        }

        // 우선순위 — RUNNING > PENDING, OUTBOUND > INBOUND
        TbWcsShuttleOrder active =
                ValueUtil.isNotEmpty(runningOutbound) ? runningOutbound :
                        ValueUtil.isNotEmpty(runningInbound) ? runningInbound :
                                ValueUtil.isNotEmpty(pendingOutbound) ? pendingOutbound :
                                        pendingInbound;

        if (ValueUtil.isNotEmpty(active)) return buildActiveResponse(active, palletBarcode);

        // 2) 시험 회수 대기 상태 — sample 출고 완료 후 재입고 대기
        TbWcsShuttleOrder pendingSample = reinboundIssuer.findPendingSampleOut(palletBarcode);
        if (ValueUtil.isNotEmpty(pendingSample)) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("palletBarcode", palletBarcode);
            r.put("mode", "PENDING_SAMPLE");
            r.put("sampleOutOrderKey", pendingSample.getOrderKey());
            r.put("hostOrderKey", pendingSample.getHostOrderKey());
            r.put("userMessage", "시험용 출고 완료. 재입고 또는 폐기를 선택하세요.");
            return r;
        }

        // 3) 일반 출고 완료 후 상태 (완전 출고 / 재입고 완료 / 잔량 존재)
        return buildPostOutboundStatus(palletBarcode, all);
    }

    /**
     * 활성 셔틀에 대한 응답 빌드.
     * 출고/입고 모드 + 진행률 + sample/reinbound 플래그.
     */
    private Map<String, Object> buildActiveResponse(TbWcsShuttleOrder active, String palletBarcode) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("palletBarcode", palletBarcode);
        r.put("orderKey", active.getOrderKey());
        r.put("orderType", active.getOrderType());
        r.put("subOrderType", active.getSubOrderType());
        r.put("hostOrderKey", active.getHostOrderKey());
        r.put("orderStatus", active.getOrderStatus());
        r.put("parentOrderKey", active.getParentOrderKey());

        SubOrderType sub = SubOrderType.fromOrNormal(active.getSubOrderType());
        r.put("autoFinalize", sub.isAutoFinalize());
        r.put("sampleFlow", sub.isSampleFlow());

        boolean isOutbound = OrderType.OUTBOUND.matches(active.getOrderType());
        boolean isInbound = OrderType.INBOUND.matches(active.getOrderType());

        // 부모 셔틀이 NORMAL 출고면 재입고 사이클, sample 출고면 sample 사이클로 마킹
        boolean inSampleCycle = sub.isSampleFlow();
        if (isInbound && ValueUtil.isNotEmpty(active.getParentOrderKey())) {
            TbWcsShuttleOrder parent = shuttleOrderRepository.findByOrderKey(active.getParentOrderKey());
            if (ValueUtil.isNotEmpty(parent)) {
                SubOrderType parentSub = SubOrderType.fromOrNormal(parent.getSubOrderType());
                boolean parentIsOutbound = OrderType.OUTBOUND.codeAsString()
                        .equalsIgnoreCase(parent.getOrderType());
                if (parentIsOutbound && parentSub.isAutoReInbound()) {
                    r.put("isReinbound", true);
                    r.put("requiresReinbound", true);
                    r.put("parentOutboundOrderKey", parent.getOrderKey());
                }
                if (parentSub.isSampleFlow()) inSampleCycle = true;
            }
        }
        if (inSampleCycle) r.put("inSampleCycle", true);

        // 출고 모드 — 진행률 + 자식 재입고 정보
        if (isOutbound) {
            r.put("mode", "OUTBOUND");
            r.put("progress", progressService.outboundProgress(active.getOrderKey()));
            for (TbWcsShuttleOrder sib : shuttleOrderRepository.findByParentOrderKey(active.getOrderKey())) {
                if (!OrderType.INBOUND.matches(sib.getOrderType())) continue;
                Integer cs = sib.getOrderStatus();
                if (ValueUtil.isNotEmpty(cs) && ShuttleOrderStatus.isFinalStatus(cs)) continue;
                r.put("reinboundOrderKey", sib.getOrderKey());
                r.put("reinboundStatus", sib.getOrderStatus());
                break;
            }
        } else {
            r.put("mode", "INBOUND");
            r.put("progress", progressService.progress(palletBarcode));
        }
        return r;
    }

    /**
     * 활성 작업 없을 때 — 일반 출고 완료 후 상태 해석.
     * 갈래: 완전 출고(반출 가능) / 재입고 완료(반출 가능) / 잔량 존재(재입고 필요).
     */
    private Map<String, Object> buildPostOutboundStatus(String palletBarcode,
                                                        List<TbWcsShuttleOrder> all) {
        Integer completedCode = ShuttleOrderStatus.COMPLETED.codeAsIntOrNull();

        // 마지막 NORMAL 출고 (autoReInbound 가 켜진 종류) 탐색
        TbWcsShuttleOrder lastNormalOutbound = null;
        for (TbWcsShuttleOrder s : all) {
            if (!completedCode.equals(s.getOrderStatus())) continue;
            if (!OrderType.OUTBOUND.matches(s.getOrderType())) continue;
            SubOrderType sub = SubOrderType.fromOrNormal(s.getSubOrderType());
            if (!sub.isAutoReInbound()) continue;
            lastNormalOutbound = s;
            break;
        }
        if (ValueUtil.isEmpty(lastNormalOutbound)) return null;

        // 이미 재입고 완료된 셔틀이 있는지 — parent_order_key 가 마지막 출고면 재입고가 끝난 것
        // (remaining_qty 만 보면 "필요"로 잘못 판정될 수 있음)
        TbWcsShuttleOrder completedReinbound = null;
        for (TbWcsShuttleOrder s : all) {
            if (!OrderType.INBOUND.matches(s.getOrderType())) continue;
            if (!completedCode.equals(s.getOrderStatus())) continue;
            if (!lastNormalOutbound.getOrderKey().equals(s.getParentOrderKey())) continue;
            completedReinbound = s;
            break;
        }

        // 살아있는 박스 잔량 합 — 재입고 필요 판정용
        int remainingTotal = 0;
        for (TbWcsPalletBox b : boxRepository.findByPalletBarcode(palletBarcode)) {
            BoxStatus bst = BoxStatus.fromCode(b.getBoxStatus());
            if (bst == BoxStatus.VOID || bst == BoxStatus.DEPLETED) continue;
            remainingTotal += b.calcRemainingQty();
        }

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("palletBarcode", palletBarcode);
        r.put("mode", "POST_OUTBOUND");
        r.put("lastOutboundOrderKey", lastNormalOutbound.getOrderKey());
        r.put("hostOrderKey", lastNormalOutbound.getHostOrderKey());
        r.put("orderStatus", lastNormalOutbound.getOrderStatus());
        r.put("remainingQty", remainingTotal);

        // 분기 — 재입고 완료 / 잔량 있음 / 완전 출고
        if (ValueUtil.isNotEmpty(completedReinbound)) {
            r.put("requiresReinbound", false);
            r.put("fullyShipped", false);
            r.put("reinboundCompleted", true);
            r.put("reinboundOrderKey", completedReinbound.getOrderKey());
            r.put("userMessage", "재입고 완료 — 파렛트를 반출하셔도 됩니다.");
        } else if (remainingTotal > 0) {
            r.put("requiresReinbound", true);
            r.put("fullyShipped", false);
            r.put("userMessage", "재입고 필요 — 잔량이 있습니다. [재동기화] 버튼으로 재입고를 진행하세요.");
        } else {
            r.put("requiresReinbound", false);
            r.put("fullyShipped", true);
            r.put("userMessage", "완전 출고 완료 — 파렛트를 반출하셔도 됩니다.");
        }
        return r;
    }
}
