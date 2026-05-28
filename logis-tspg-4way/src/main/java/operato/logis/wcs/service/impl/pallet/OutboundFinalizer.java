package operato.logis.wcs.service.impl.pallet;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.BoxStatus;
import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.consts.ShuttleOrderStatus;
import operato.logis.wcs.consts.SubOrderType;
import operato.logis.wcs.entity.ExtTbInventoryStock;
import operato.logis.wcs.entity.TbWcsPalletBox;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.service.impl.ecs.EcsCallbackProcessor;
import operato.logis.wcs.service.impl.order.lookup.OrderLookupUtils;
import operato.logis.wcs.service.impl.order.state.ShuttleOrderGuard;
import operato.logis.wcs.service.repository.InventoryStockRepository;
import operato.logis.wcs.service.repository.PalletBoxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.util.ValueUtil;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static operato.logis.wcs.common.util.lang.CommonUtils.nullToEmpty;
import static operato.logis.wcs.common.util.lang.CommonUtils.toInt;

/**
 * 출고 확정 + 박스 finalize + 재고 차감 (StateWriter).
 *
 * 핵심 처리:
 *   - finalizeOutbound       : 사용자 [출고 확정] 진입점. ARRIVED 게이트, 우회 분기, ECS 완료 트리거
 *   - finalizeBoxesForOutbound : SubOrderType 별 박스 finalize (셔틀 COMPLETED 와 같은 시점)
 *
 * SubOrderType 별 분기:
 *   - SAMPLE_OUT  : 박스를 PRINTED 로 reset (회수 후 재스캔 가능 상태)
 *   - autoFinalize : 전 박스 전량 DEPLETED + 재고 차감
 *   - NORMAL/PARTIAL_OUT : picked 채워진 박스만 차감 + reset
 *
 * idempotent — 이미 DEPLETED 인 박스는 자동 skip. 두 번 호출돼도 안전.
 */
@Service
@RequiredArgsConstructor
public class OutboundFinalizer {

    private static final Logger logger = LoggerFactory.getLogger(OutboundFinalizer.class);

    private final PalletBoxRepository boxRepository;
    private final InventoryStockRepository stockRepository;
    private final OrderLookupUtils orderLookup;
    private final PalletProgressService progressService;
    private final OutboundAutoScanner autoScanner;
    @Lazy @Autowired private EcsCallbackProcessor ecsCallbackProcessor;

    /** 사용자 [출고 확정] — 우회 없음. */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> finalizeOutbound(String outboundOrderKey) {
        return finalizeOutbound(outboundOrderKey, false);
    }

    /**
     * 출고 확정 처리.
     * 각 박스에 대해:
     *   1) remaining_qty -= picked_qty
     *   2) picked_qty = 0
     *   3) remaining_qty == 0 → DEPLETED, 아니면 SCANNED 유지
     *   4) 재고 차감
     *   5) outbound_order_key 는 이력용 유지
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> finalizeOutbound(String outboundOrderKey, boolean adminBypass) {
        TbWcsShuttleOrder shuttle = orderLookup.getShuttleOrderOrThrow(
                outboundOrderKey, OrderType.OUTBOUND, "출고");
        SubOrderType sub = SubOrderType.fromOrNormal(shuttle.getSubOrderType());

        // ARRIVED 게이트 — autoFinalize 종도 동일. 정상 ECS 콜백은 이 메서드를 거치지 않음.
        ShuttleOrderGuard.requireArrived(shuttle, "출고 확정이");

        Map<String, Object> progress = progressService.outboundProgress(outboundOrderKey);
        boolean completed = Boolean.TRUE.equals(progress.get("completed"));

        // 미완료 + 우회 미허용 → 거부 / 우회 + 부분 출고 박스 존재 → 거부
        if (!sub.isAutoFinalize() && !completed) {
            if (!adminBypass) {
                throw new ElidomRuntimeException("OUTBOUND_QTY_MISMATCH",
                        "출고 수량이 일치하지 않습니다. (요청: %s, 스캔: %s)"
                                .formatted(progress.get("expectedQty"), progress.get("pickedQty")));
            }
            String palletBarcode = shuttle.getBarcode();
            if (progressService.hasPartialPickedBox(palletBarcode)) {
                throw new ElidomRuntimeException("PARTIAL_OUTBOUND_BLOCKS_BYPASS",
                        "부분 출고된 박스가 있어 관리자 우회로 확정할 수 없습니다. 전체 출고만 우회 가능합니다.");
            }
        }

        // 관리자 우회: 미스캔 박스 picked_qty 를 부족분만큼 자동 채움
        int[] bypassRes = (adminBypass && !sub.isAutoFinalize() && !completed)
                ? autoScanner.autoScanAliveBoxesForOutbound(shuttle, progress) : new int[]{0, 0};
        int bypassedBoxCount = bypassRes[0];
        int bypassedQty = bypassRes[1];

        // 핵심 — 박스 finalize + 재고 차감
        int[] r = finalizeBoxesForOutbound(shuttle);
        int finalizedBoxCount = r[0];
        int finalizedQty = r[1];

        // ARRIVED 면 완료 트리거
        boolean triggered = false;
        if (ShuttleOrderStatus.isArrived(shuttle.getOrderStatus())) {
            ecsCallbackProcessor.completeShuttleAfterFinalize(shuttle);
            triggered = true;
        }

        // 응답
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("outboundOrderKey", outboundOrderKey);
        result.put("expectedQty", progress.get("expectedQty"));
        result.put("finalizedQty", finalizedQty);
        result.put("finalizedBoxCount", finalizedBoxCount);
        result.put("finalized", true);
        result.put("completionTriggered", triggered);
        result.put("nextAction", triggered ? "DONE" : "WAIT_ECS_ARRIVAL");
        result.put("userMessage", triggered ? "출고 확정 완료." : "ECS 운송 완료(ARRIVED) 후 자동 확정됩니다.");
        if (bypassedBoxCount > 0) {
            result.put("adminBypass", true);
            result.put("bypassedBoxCount", bypassedBoxCount);
            result.put("bypassedQty", bypassedQty);
        }
        return result;
    }

    /**
     * SubOrderType 별 박스 finalize 분기.
     *
     * @return [finalizedBoxCount, finalizedQty]
     */
    @Transactional(rollbackFor = Exception.class)
    public int[] finalizeBoxesForOutbound(TbWcsShuttleOrder shuttle) {
        SubOrderType sub = SubOrderType.fromOrNormal(shuttle.getSubOrderType());

        return switch (sub) {
            // SAMPLE_OUT — 박스 PRINTED 로 reset, 채취/재고 차감은 모달 [확정 미리보기] 단계
            case SAMPLE_OUT -> resetBoxesToPrintedForSample(shuttle);
            // SAMPLE_DISCARD / DISPOSAL_OUT / RETURN_OUT — 전량 소진 + 재고 차감
            case SAMPLE_DISCARD, DISPOSAL_OUT, RETURN_OUT -> depleteAllAliveBoxesForOutbound(shuttle);
            // NORMAL / PARTIAL_OUT — picked 채워진 박스만 처리
            default -> finalizePickedBoxesForOutbound(shuttle);
        };
    }

    /** NORMAL/PARTIAL_OUT 출고 — picked_qty 채워진 박스만 처리. */
    private int[] finalizePickedBoxesForOutbound(TbWcsShuttleOrder shuttle) {
        String outboundOrderKey = shuttle.getOrderKey();
        int finalizedBoxCount = 0;
        int finalizedQty = 0;

        for (TbWcsPalletBox box : boxRepository.findActivePickedByOutbound(outboundOrderKey)) {
            int picked = box.getPickedQtyOrZero();
            if (picked <= 0) continue;

            int rem = box.calcRemainingQty();
            int newRemaining = Math.max(0, rem - picked);

            // 재고 차감 (확정 시점에 한 번만)
            deductStock(box, shuttle, picked);

            // 잔량/픽업 정리 + 0 이면 DEPLETED
            box.setRemainingQty(newRemaining);
            box.setPickedQty(0);
            if (newRemaining == 0) {
                PalletBoxStatusTransition.transition(box, BoxStatus.DEPLETED, "finalizeOutbound");
            }
            boxRepository.update(box, "remainingQty", "pickedQty", "boxStatus");

            finalizedBoxCount++;
            finalizedQty += picked;

            logger.info("[ Pallet ][ Finalize ] box - boxId={}, picked={}, remainingFrom={}, remainingTo={}, status={}",
                    box.getId(), picked, rem, newRemaining, box.getBoxStatus());
        }
        if (finalizedBoxCount > 0) {
            logger.info("[ Pallet ][ Finalize ] outbound completed - orderKey={}, boxes={}, qty={}",
                    outboundOrderKey, finalizedBoxCount, finalizedQty);
        }
        return new int[]{finalizedBoxCount, finalizedQty};
    }

    /**
     * 시험 출고 자동 finalize 시 박스 reset — SCANNED → PRINTED.
     *
     * 입고 단계에서 사용자가 박스를 이미 스캔하여 SCANNED. 시험 출고 후 PENDING_SAMPLE 단계에서
     * 회수된 박스를 재스캔해야 하는데, 정상 scan 은 PRINTED 박스만 허용하므로 미리 PRINTED 로 되돌린다.
     *
     * 이후 흐름:
     *   - 사용자 박스 스캔 → PalletBoxScanner.scan 이 PRINTED → SCANNED 정상 전이
     *   - 채취 입력 → reportSampleTaken 이 picked_qty 누적
     *   - 모달 [확정 미리보기] → 미스캔/전량 채취 박스 DEPLETED
     */
    private int[] resetBoxesToPrintedForSample(TbWcsShuttleOrder shuttle) {
        int resetCount = 0;
        for (TbWcsPalletBox box : boxRepository.findByPalletBarcode(shuttle.getBarcode())) {
            BoxStatus st = BoxStatus.fromCode(box.getBoxStatus());
            if (st != BoxStatus.SCANNED) continue;
            PalletBoxStatusTransition.restore(box, BoxStatus.PRINTED, "sampleOut-reset");
            box.setScannedAt(null);
            boxRepository.update(box, "boxStatus", "scannedAt");
            resetCount++;
        }
        if (resetCount > 0) {
            logger.info("[ Pallet ][ Sample ] reset - shuttle={}, pallet={}, resetBoxes={}",
                    shuttle.getOrderKey(), shuttle.getBarcode(), resetCount);
        }
        return new int[]{resetCount, 0};
    }

    /** 자동 출고 (SAMPLE_DISCARD/DISPOSAL_OUT/RETURN_OUT) — 전 박스 전량 소진 + 재고 차감. */
    private int[] depleteAllAliveBoxesForOutbound(TbWcsShuttleOrder shuttle) {
        int finalizedBoxCount = 0;
        int finalizedQty = 0;
        for (TbWcsPalletBox box : boxRepository.findByPalletBarcode(shuttle.getBarcode())) {
            BoxStatus st = BoxStatus.fromCode(box.getBoxStatus());
            if (st == BoxStatus.VOID || st == BoxStatus.DEPLETED) continue;

            int remaining = box.calcRemainingQty();
            if (remaining > 0) {
                deductStock(box, shuttle, remaining);
            }
            box.setRemainingQty(0);
            box.setPickedQty(0);
            PalletBoxStatusTransition.transition(box, BoxStatus.DEPLETED, "autoFinalizeOutbound");
            box.setOutboundOrderKey(shuttle.getOrderKey());
            boxRepository.update(box, "remainingQty", "pickedQty", "boxStatus", "outboundOrderKey");

            finalizedBoxCount++;
            finalizedQty += remaining;

            logger.info("[ Pallet ][ AutoFinalize ] box depleted - boxId={}, remaining={}, sub={}",
                    box.getId(), remaining, shuttle.getSubOrderType());
        }
        if (finalizedBoxCount > 0) {
            logger.info("[ Pallet ][ AutoFinalize ] outbound completed - orderKey={}, sub={}, boxes={}, qty={}",
                    shuttle.getOrderKey(), shuttle.getSubOrderType(),
                    finalizedBoxCount, finalizedQty);
        }
        return new int[]{finalizedBoxCount, finalizedQty};
    }

    /** 재고 차감 — 출고 확정 시점에 호출. */
    private void deductStock(TbWcsPalletBox box, TbWcsShuttleOrder outbound, int pickQty) {
        ExtTbInventoryStock target = findStockMatching(
                box.getEqGroupId(), outbound.getCarryingStockId(), box.getItemCode(), box.getLotNo());
        if (ValueUtil.isEmpty(target)) {
            logger.error("[ Pallet ][ Stock ] row not found - stockId={}, sku={}, lot={}",
                    outbound.getCarryingStockId(), box.getItemCode(), box.getLotNo());
            throw new ElidomRuntimeException("STOCK_NOT_FOUND",
                    "재고 행을 찾을 수 없습니다. (stockId=%s, sku=%s, lot=%s)"
                            .formatted(outbound.getCarryingStockId(), box.getItemCode(), box.getLotNo()));
        }
        int stockQty = ValueUtil.isEmpty(target.getItemQty()) ? 0 : target.getItemQty();
        if (pickQty > stockQty) {
            logger.error("[ Pallet ][ Stock ] insufficient - stockId={}, sku={}, stock={}, request={}",
                    outbound.getCarryingStockId(), box.getItemCode(), stockQty, pickQty);
            throw new ElidomRuntimeException("STOCK_INSUFFICIENT",
                    "재고 수량(" + stockQty + ")이 출고 수량(" + pickQty + ") 미만입니다.");
        }
        target.setItemQty(stockQty - pickQty);
        stockRepository.update(target);
    }

    private ExtTbInventoryStock findStockMatching(String eqGroupId, String stockId, String sku, String lotNo) {
        String targetLot = nullToEmpty(lotNo);
        return stockRepository.findByEqGroupIdAndStockId(eqGroupId, stockId).stream()
                .filter(s -> Objects.equals(s.getSku(), sku))
                .filter(s -> nullToEmpty(s.getLotNo()).equals(targetLot))
                .findFirst()
                .orElse(null);
    }
}
