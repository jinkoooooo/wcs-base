package operato.logis.wcs.service.impl.pallet;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.BoxStatus;
import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.consts.SubOrderType;
import operato.logis.wcs.entity.TbWcsPalletBox;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.service.impl.order.lookup.OrderLookupUtils;
import operato.logis.wcs.service.repository.PalletBoxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.exception.server.ElidomRuntimeException;

import static operato.logis.wcs.common.util.check.Validator.requireNotEmpty;
import xyz.elidom.util.ValueUtil;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static operato.logis.wcs.common.util.lang.CommonUtils.toInt;

/**
 * 관리자 우회 자동 스캔 (StateWriter).
 *
 * 사용자 [출고 확정] 과 분리된 흐름. 부족분 자동 채움 + SCANNED 전이까지만.
 * 셔틀 90 / 박스 finalize / 재고 차감은 하지 않음 — 사용자가 [출고 확정] 으로 별도 진행.
 *
 *   - autoScanAliveBoxesForOutbound : 출고 부족분만큼 미스캔 박스 자동 채움
 *   - autoScanOutboundBoxes         : entry 메서드 (부분 출고 박스가 있으면 거부)
 *   - autoScanSampleBoxes           : 시험 채취 단계 (PENDING_SAMPLE) 박스 전수 자동 스캔
 */
@Service
@RequiredArgsConstructor
public class OutboundAutoScanner {

    private static final Logger logger = LoggerFactory.getLogger(OutboundAutoScanner.class);

    private final PalletBoxRepository boxRepository;
    private final OrderLookupUtils orderLookup;
    private final PalletProgressService progressService;

    /**
     * 관리자 우회 — 미스캔 박스의 picked_qty 를 부족분만큼 자동 채워 SCANNED 로 전이.
     * 부분 출고 박스는 호출자가 사전 차단해야 함.
     *
     * @return [bypassedBoxCount, bypassedQty]
     */
    @Transactional(rollbackFor = Exception.class)
    public int[] autoScanAliveBoxesForOutbound(TbWcsShuttleOrder shuttle, Map<String, Object> progress) {
        String outboundOrderKey = shuttle.getOrderKey();
        String palletBarcode = shuttle.getBarcode();
        int expectedQty = toInt(progress.get("expectedQty"));
        int currentPicked = toInt(progress.get("pickedQty"));
        int shortage = Math.max(0, expectedQty - currentPicked);

        int bypassedBoxCount = 0;
        int bypassedQty = 0;
        Date now = new Date();

        // 부족분이 있으면 살아있는 박스 순회하며 채움
        if (shortage > 0) {
            for (TbWcsPalletBox box : boxRepository.findByPalletBarcode(palletBarcode)) {
                if (shortage <= 0) break;

                BoxStatus st = BoxStatus.fromCode(box.getBoxStatus());
                if (st == BoxStatus.VOID || st == BoxStatus.DEPLETED) continue;

                // 이 박스에서 추가로 잡을 수 있는 양
                int rem = box.calcRemainingQty();
                int picked = box.getPickedQtyOrZero();
                int boxAvailable = rem - picked;
                if (boxAvailable <= 0) continue;

                int addQty = Math.min(boxAvailable, shortage);
                box.setPickedQty(picked + addQty);
                box.setOutboundOrderKey(outboundOrderKey);

                // PRINTED → SCANNED 전이 (이미 SCANNED 면 상태 유지)
                if (st == BoxStatus.PRINTED) {
                    PalletBoxStatusTransition.transition(box, BoxStatus.SCANNED, "adminAutoScan");
                    box.setScannedAt(now);
                    boxRepository.update(box, "pickedQty", "outboundOrderKey", "boxStatus", "scannedAt");
                } else {
                    boxRepository.update(box, "pickedQty", "outboundOrderKey");
                }

                bypassedBoxCount++;
                bypassedQty += addQty;
                shortage -= addQty;
            }
        }
        logger.warn("[ Pallet ][ AdminAutoScan ] outbound - orderKey={}, pallet={}, bypassedBoxes={}, bypassedQty={}, unfilled={}",
                outboundOrderKey, palletBarcode, bypassedBoxCount, bypassedQty, shortage);
        return new int[]{bypassedBoxCount, bypassedQty};
    }

    /**
     * 관리자 우회 [박스 스캔 자동 완료] entry.
     * 부분 출고 박스 존재 시 거부 — 전체 출고만 우회 가능.
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> autoScanOutboundBoxes(String outboundOrderKey) {
        requireNotEmpty(outboundOrderKey, "INVALID_PARAMETER", "출고 주문번호가 입력되지 않았습니다.");

        TbWcsShuttleOrder shuttle = orderLookup.getShuttleOrderOrThrow(
                outboundOrderKey, OrderType.OUTBOUND, "출고");
        SubOrderType sub = SubOrderType.fromOrNormal(shuttle.getSubOrderType());

        // autoFinalize 종은 박스 스캔 대상 아님
        if (sub.isAutoFinalize())
            throw new ElidomRuntimeException("INVALID_SUB_ORDER_TYPE",
                    "자동 출고(SAMPLE/DISCARD/DISPOSAL/RETURN) 는 박스 자동 스캔 대상이 아닙니다.");

        // 부분 출고된 박스가 있으면 거부
        if (progressService.hasPartialPickedBox(shuttle.getBarcode())) {
            throw new ElidomRuntimeException("PARTIAL_OUTBOUND_BLOCKS_BYPASS",
                    "부분 출고된 박스가 있어 박스 스캔 자동 완료가 불가합니다. 전체 출고만 우회 가능합니다.");
        }

        Map<String, Object> progress = progressService.outboundProgress(outboundOrderKey);
        int[] r = autoScanAliveBoxesForOutbound(shuttle, progress);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("outboundOrderKey", outboundOrderKey);
        result.put("bypassedBoxCount", r[0]);
        result.put("bypassedQty", r[1]);
        result.put("userMessage", "박스 스캔 자동 완료 (" + r[0] + "박스, " + r[1] + " EA). [출고 확정] 으로 진행하세요.");
        return result;
    }

    /**
     * 관리자 우회 — 시험 채취 단계 (PENDING_SAMPLE) 의 박스 전수 자동 스캔.
     *
     * 파렛트의 모든 PRINTED 박스를 SCANNED 로 전이 (회수 확인 자동 완료).
     * picked_qty 는 건드리지 않음 — 채취 입력은 사용자가 박스별로 명시.
     * 셔틀 / 출고 확정 / 재입고 발행 등 다른 처리는 하지 않음.
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> autoScanSampleBoxes(String palletBarcode) {
        requireNotEmpty(palletBarcode, "INVALID_PARAMETER", "파렛트 바코드가 입력되지 않았습니다.");

        Date now = new Date();
        int scannedCount = 0;

        // 모든 PRINTED 박스를 SCANNED 로 전이
        for (TbWcsPalletBox box : boxRepository.findByPalletBarcode(palletBarcode)) {
            BoxStatus st = BoxStatus.fromCode(box.getBoxStatus());
            if (st != BoxStatus.PRINTED) continue;
            PalletBoxStatusTransition.transition(box, BoxStatus.SCANNED, "adminAutoScanSample");
            box.setScannedAt(now);
            boxRepository.update(box, "boxStatus", "scannedAt");
            scannedCount++;
        }
        logger.info("[ Pallet ][ AdminAutoScan ] sample - pallet={}, scannedBoxes={}", palletBarcode, scannedCount);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("palletBarcode", palletBarcode);
        result.put("scannedBoxCount", scannedCount);
        result.put("userMessage",
                "박스 전수 자동 스캔 완료 (" + scannedCount + "박스). 채취 수량을 입력하고 [확정 미리보기] 로 진행하세요.");
        return result;
    }
}
