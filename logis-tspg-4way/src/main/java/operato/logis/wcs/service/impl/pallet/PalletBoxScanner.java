package operato.logis.wcs.service.impl.pallet;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.BoxStatus;
import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.consts.ShuttleOrderStatus;
import operato.logis.wcs.consts.SubOrderType;
import operato.logis.wcs.entity.TbWcsPalletBox;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.service.impl.order.state.ShuttleOrderGuard;
import operato.logis.wcs.service.repository.PalletBoxRepository;
import operato.logis.wcs.service.repository.ShuttleOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.util.ValueUtil;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static operato.logis.wcs.common.util.lang.CommonUtils.nz;
import static operato.logis.wcs.common.util.lang.CommonUtils.toInt;
import static operato.logis.wcs.common.util.check.Validator.requireFound;
import static operato.logis.wcs.common.util.check.Validator.requireNonNegative;
import static operato.logis.wcs.common.util.check.Validator.requireNotEmpty;
import static operato.logis.wcs.common.util.check.Validator.requirePositive;

/**
 * 파렛트 박스 단위의 상태/수량 변경 서비스.
 *
 * 동작 요약:
 *   - scan / scanOut    : 입출고 BCR 스캔 처리 (picked_qty 누적, remaining_qty 그대로)
 *   - reportSampleTaken : 시험 채취 (remaining_qty 즉시 차감)
 *   - adjustQty         : 수량 보정 (remaining_qty 직접 변경)
 *   - processPartialOutbound : 부분 출고 (사용자 지정 수량 picked 누적)
 *   - voidBox           : 박스 폐기
 *
 * 재고(stock) 차감은 출고 확정 시점에 OutboundFinalizer 가 담당. 본 서비스는 박스 레코드만 변경.
 */
@Service
@RequiredArgsConstructor
public class PalletBoxScanner {

    private static final Logger logger = LoggerFactory.getLogger(PalletBoxScanner.class);

    private final PalletBoxRepository boxRepository;
    private final ShuttleOrderRepository shuttleOrderRepository;
    private final PalletProgressService progressService;

    /**
     * 입고 BCR 스캔 — PRINTED → SCANNED 전이, 수량 변경 없음.
     * 진행률 결과를 반환해 UI 가 즉시 상태를 갱신.
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> scan(String palletBarcode, String boxBarcode) {
        requireNotEmpty(boxBarcode, "INVALID_PARAMETER", "박스 바코드가 입력되지 않았습니다.");

        // box_barcode 로 직접 조회 — PENDING/PRINTED 상태만 허용
        TbWcsPalletBox box = boxRepository.findByBoxBarcodeAndStatus(boxBarcode, List.of(
                BoxStatus.PENDING.code(),
                BoxStatus.PRINTED.code()
        ));
        requireFound(box, "BOX_NOT_FOUND",
                "스캔한 박스와 일치하는 인쇄 완료 상태의 박스를 찾을 수 없습니다. (박스: " + boxBarcode + ")");

        // 파렛트 일치 검증
        if (!Objects.equals(palletBarcode, box.getPalletBarcode())) {
            throw new ElidomRuntimeException("PALLET_MISMATCH",
                    "박스 바코드의 파렛트가 일치하지 않습니다. " +
                            "(스캔한 파렛트: " + palletBarcode + ", 박스의 파렛트: " + box.getPalletBarcode() + ")");
        }

        // 상태 전이 + 스캔 시각 기록
        PalletBoxStatusTransition.transition(box, BoxStatus.SCANNED, "scan");
        box.setScannedAt(new Date());
        boxRepository.update(box, "boxStatus", "scannedAt");

        logger.info("[ Pallet ][ Scan ] inbound - pallet={}, boxId={}, seq={}, sku={}, lot={}, remaining={}",
                palletBarcode, box.getId(), box.getBoxSeq(), box.getItemCode(), box.getLotNo(),
                box.getRemainingQty());

        return progressService.progress(palletBarcode);
    }

    /**
     * 출고 BCR 스캔 — picked_qty 만 누적.
     *
     * 처리 규칙:
     *   - 박스 잔량 ≤ 출고 잔량 → picked_qty += boxAvailable (박스 통째로)
     *   - 박스 잔량 > 출고 잔량 → picked_qty += outboundRem (부분 출고)
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> scanOut(String palletBarcode, String boxBarcode, String outboundOrderKey) {
        requireNotEmpty(outboundOrderKey, "INVALID_PARAMETER", "출고 주문번호가 지정되지 않았습니다.");
        requireNotEmpty(boxBarcode, "INVALID_PARAMETER", "박스 바코드가 입력되지 않았습니다.");

        // 박스 바코드로 직접 조회
        TbWcsPalletBox box = boxRepository.findByBoxBarcode(boxBarcode);
        requireFound(box, "BOX_NOT_FOUND_FOR_OUTBOUND",
                "출고 가능한 박스를 찾을 수 없습니다. (박스: " + boxBarcode + ")");

        // 파렛트 일치 검증
        if (!Objects.equals(palletBarcode, box.getPalletBarcode())) {
            throw new ElidomRuntimeException("PALLET_MISMATCH",
                    "박스 바코드의 파렛트가 일치하지 않습니다. " +
                            "(스캔한 파렛트: " + palletBarcode + ", 박스의 파렛트: " + box.getPalletBarcode() + ")");
        }

        // 다른 출고에 이미 잡혀있는 박스 차단
        if (ValueUtil.isNotEmpty(box.getOutboundOrderKey())
                && !Objects.equals(box.getOutboundOrderKey(), outboundOrderKey)
                && box.getPickedQtyOrZero() > 0) {
            throw new ElidomRuntimeException("BOX_LOCKED_BY_OTHER_OUTBOUND",
                    "다른 출고에 잡혀있는 박스입니다. (다른 출고: " + box.getOutboundOrderKey() + ")");
        }

        // 출고 잔량 계산 — 이미 전량 스캔된 출고는 차단
        Map<String, Object> prog = progressService.outboundProgress(outboundOrderKey);
        int expectedQty = toInt(prog.get("expectedQty"));
        int outboundRem = Math.max(0, expectedQty - toInt(prog.get("pickedQty")));
        if (outboundRem <= 0) {
            throw new ElidomRuntimeException("OUTBOUND_ALREADY_FULL",
                    "이미 요청 수량(" + expectedQty + ")만큼 스캔되었습니다.");
        }

        requireActiveOutbound(outboundOrderKey);

        // 박스 잔량 — DEPLETED 박스는 차단
        int boxRemaining = box.calcRemainingQty();
        if (boxRemaining <= 0) {
            throw new ElidomRuntimeException("BOX_ALREADY_DEPLETED",
                    "박스 잔량이 없습니다. (박스: " + box.getBoxBarcode() + ")");
        }

        // 이번 출고에서 추가로 잡을 수 있는 양 = remaining - (이미 picked)
        int alreadyPicked = box.getPickedQtyOrZero();
        int boxAvailable = boxRemaining - alreadyPicked;
        if (boxAvailable <= 0) {
            throw new ElidomRuntimeException("BOX_FULLY_PICKED_IN_THIS_OUTBOUND",
                    "이미 이번 출고에 박스 전량이 잡혀있습니다. (박스: " + box.getBoxBarcode() + ")");
        }

        // 부분/전체 결정 후 picked 누적
        boolean partial = boxAvailable > outboundRem;
        int pickQty = partial ? outboundRem : boxAvailable;
        applyPick(box, pickQty, outboundOrderKey);

        logger.info("[ Pallet ][ ScanOut ] {} - pallet={}, boxId={}, sku={}, lot={}, pickedDelta={}, pickedTotal={}, remaining={}, orderKey={}",
                partial ? "partial" : "full",
                box.getPalletBarcode(), box.getId(), box.getItemCode(), box.getLotNo(),
                pickQty, box.getPickedQty(), box.getRemainingQty(), outboundOrderKey);

        // 응답 — 진행률 + partial 정보
        Map<String, Object> result = new LinkedHashMap<>(progressService.outboundProgress(outboundOrderKey));
        result.put("partialOutbound", partial);
        if (partial) {
            result.put("partialInfo", buildPartialInfo(box, alreadyPicked, pickQty));
            result.put("userMessage", "박스에서 " + pickQty + " EA 출고, 박스 잔량 " + (boxAvailable - pickQty) + " EA.");
        } else {
            result.put("userMessage", "박스 " + pickQty + " EA 출고 처리됨.");
        }
        return result;
    }

    /** 부분 출고 응답에 포함할 박스 스냅샷. */
    private Map<String, Object> buildPartialInfo(TbWcsPalletBox box, int pickedBefore, int pickQty) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("boxId", box.getId());
        info.put("boxBarcode", box.getBoxBarcode());
        info.put("itemCode", box.getItemCode());
        info.put("lotNo", box.getLotNo());
        info.put("uom", ValueUtil.isEmpty(box.getUom()) ? "EA" : box.getUom());
        info.put("totalQty", box.getTotalQty());
        info.put("remainingQty", box.getRemainingQty());
        info.put("pickedQtyBefore", pickedBefore);
        info.put("pickedQtyAfter", box.getPickedQty());
        info.put("outboundQty", pickQty);
        return info;
    }

    /**
     * 박스 수량 보정 — remaining_qty 직접 변경.
     * picked_qty 가 newRemaining 보다 크면 자동 축소.
     */
    @Transactional(rollbackFor = Exception.class)
    public TbWcsPalletBox adjustQty(String boxId, int newRemaining) {
        requireNonNegative(newRemaining, "수량은 0 이상이어야 합니다.");

        TbWcsPalletBox box = requireBox(boxId);
        ensureModifiable(box);

        // 전체 수량 초과 차단
        int total = nz(box.getTotalQty());
        if (newRemaining > total) {
            throw new ElidomRuntimeException("INVALID_QTY",
                    "조정 수량(" + newRemaining + ")이 박스 전체 수량(" + total + ")을 초과합니다.");
        }

        // picked > newRemaining 이면 picked 도 같이 축소
        int currentPicked = box.getPickedQtyOrZero();
        if (currentPicked > newRemaining) {
            box.setPickedQty(newRemaining);
        }

        int beforeRemaining = box.calcRemainingQty();
        box.setRemainingQty(newRemaining);
        PalletBoxStatusTransition.autoDepleteIfEmpty(box, "adjustQty");

        logger.info("[ Pallet ][ AdjustQty ] boxId={}, total={}, remainingFrom={}, remainingTo={}, picked={}",
                boxId, total, beforeRemaining, newRemaining, box.getPickedQty());

        return boxRepository.update(box, "remainingQty", "pickedQty", "boxStatus");
    }

    /**
     * 시험 채취 — picked_qty 누적 + remaining_qty 차감.
     * 박스 DEPLETED 전이는 하지 않음. sample-finalize-reinbound 시점에 일괄 처리.
     */
    @Transactional(rollbackFor = Exception.class)
    public TbWcsPalletBox reportSampleTaken(String boxId, int takenQty) {
        requirePositive(takenQty, "채취 수량은 1 이상이어야 합니다.");

        TbWcsPalletBox box = requireBox(boxId);
        ensureModifiable(box);

        // 보유 수량 초과 차단
        int remaining = box.calcRemainingQty();
        if (takenQty > remaining) {
            throw new ElidomRuntimeException("INVALID_QTY",
                    "채취 수량(" + takenQty + ")이 박스 보유 수량(" + remaining + ")을 초과합니다.");
        }

        // picked 누적 + remaining 차감
        int pickedBefore = box.getPickedQtyOrZero();
        box.setPickedQty(pickedBefore + takenQty);
        box.setRemainingQty(remaining - takenQty);

        logger.info("[ Pallet ][ SampleTaken ] boxId={}, sku={}, lot={}, taken={}, pickedFrom={}, pickedTo={}, remainingFrom={}, remainingTo={}",
                boxId, box.getItemCode(), box.getLotNo(), takenQty,
                pickedBefore, box.getPickedQty(), remaining, box.getRemainingQty());

        return boxRepository.update(box, "pickedQty", "remainingQty");
    }

    /**
     * 부분 출고 (UI 버튼) — 사용자가 지정한 수량만큼 picked_qty 누적(applyPick 공유).
     * 활성 NORMAL 출고가 ECS 운송 완료(ARRIVED) 이후일 때만 허용.
     */
    @Transactional(rollbackFor = Exception.class)
    public TbWcsPalletBox processPartialOutbound(String boxId, int outboundQty) {
        requirePositive(outboundQty, "부분 출고 수량은 1 이상이어야 합니다.");

        TbWcsPalletBox box = requireBox(boxId);
        ensureModifiable(box);

        // 잡힘 가능 수량 검증
        int boxAvailable = box.availableToPickQty();
        if (outboundQty > boxAvailable) {
            throw new ElidomRuntimeException("INVALID_QTY",
                    "부분 출고 수량(" + outboundQty + ")이 박스 잡힘 가능 수량(" + boxAvailable + ")을 초과합니다.");
        }

        // 활성 NORMAL 출고 결정 + ARRIVED 게이트
        TbWcsShuttleOrder outbound = findActiveNormalOutbound(box.getPalletBarcode());
        if (ValueUtil.isEmpty(outbound)) {
            throw new ElidomRuntimeException("NO_ACTIVE_OUTBOUND", "진행 중인 출고 주문이 없습니다.");
        }
        ShuttleOrderGuard.requireArrived(outbound, "부분 출고가");

        applyPick(box, outboundQty, outbound.getOrderKey());

        logger.info("[ Pallet ][ PartialOut ] boxId={}, pallet={}, sku={}, pickedDelta={}, pickedTotal={}, remaining={}, orderKey={}",
                box.getId(), box.getPalletBarcode(), box.getItemCode(),
                outboundQty, box.getPickedQty(), box.getRemainingQty(), outbound.getOrderKey());

        return box;
    }

    /** 박스 폐기 — VOID 전이. */
    @Transactional(rollbackFor = Exception.class)
    public TbWcsPalletBox voidBox(String boxId, String reason) {
        TbWcsPalletBox box = requireBox(boxId);
        String tag = "voidBox" + (ValueUtil.isEmpty(reason) ? "" : ":" + reason);
        PalletBoxStatusTransition.transition(box, BoxStatus.VOID, tag);
        logger.info("[ Pallet ][ Void ] boxId={}, pallet={}, reason={}",
                boxId, box.getPalletBarcode(), ValueUtil.isEmpty(reason) ? "-" : reason);
        return boxRepository.update(box, "boxStatus");
    }

    /**
     * picked_qty 만 누적, remaining_qty 는 건드리지 않음.
     * 상태 전이도 없음 — DEPLETED 는 출고 확정 시점에 발생.
     */
    private void applyPick(TbWcsPalletBox box, int pickQty, String outboundOrderKey) {
        box.setPickedQty(box.getPickedQtyOrZero() + pickQty);
        box.setOutboundOrderKey(outboundOrderKey);
        box.setPickedAt(new Date());
        boxRepository.update(box, "pickedQty", "outboundOrderKey", "pickedAt");
    }

    /** 출고 주문 활성 상태 검증 — 출고 타입·미종료·stockId·ARRIVED 게이트. */
    private TbWcsShuttleOrder requireActiveOutbound(String outboundOrderKey) {
        TbWcsShuttleOrder outbound = shuttleOrderRepository.findByOrderKey(outboundOrderKey);
        if (ValueUtil.isEmpty(outbound) || !OrderType.OUTBOUND.matches(outbound.getOrderType())) {
            throw new ElidomRuntimeException("NOT_AN_OUTBOUND", "유효한 출고 주문이 아닙니다.");
        }
        Integer st = outbound.getOrderStatus();
        if (ValueUtil.isNotEmpty(st) && ShuttleOrderStatus.isFinalStatus(st)) {
            throw new ElidomRuntimeException("OUTBOUND_FINALIZED", "이미 종료된 출고 주문입니다.");
        }
        if (ValueUtil.isEmpty(outbound.getCarryingStockId())) {
            throw new ElidomRuntimeException("STOCK_ID_MISSING",
                    "출고 주문에 carryingStockId 가 없어 재고 차감이 불가능합니다.");
        }
        // ECS 운송 완료(ARRIVED) 이후에만 박스 스캔 허용
        ShuttleOrderGuard.requireArrived(outbound, "출고 스캔이");
        return outbound;
    }

    /** 파렛트의 활성 NORMAL 출고 1건 탐색. */
    private TbWcsShuttleOrder findActiveNormalOutbound(String palletBarcode) {
        return shuttleOrderRepository.findByBarcode(palletBarcode).stream()
                .filter(s -> OrderType.OUTBOUND.matches(s.getOrderType()))
                .filter(s -> SubOrderType.fromOrNormal(s.getSubOrderType()) == SubOrderType.NORMAL)
                .filter(s -> ShuttleOrderStatus.isActive(s.getOrderStatus()))
                .findFirst()
                .orElse(null);
    }

    private TbWcsPalletBox requireBox(String id) {
        TbWcsPalletBox box = boxRepository.findById(id);
        requireFound(box, "BOX_NOT_FOUND", "해당 박스를 찾을 수 없습니다. (박스 ID: " + id + ")");
        return box;
    }

    /** DEPLETED/VOID 박스는 변경 불가. */
    private void ensureModifiable(TbWcsPalletBox box) {
        BoxStatus status = BoxStatus.fromCode(box.getBoxStatus());
        if (status == BoxStatus.DEPLETED) {
            throw new ElidomRuntimeException("BOX_ALREADY_DEPLETED", "이미 출고 완료된 박스는 변경할 수 없습니다.");
        }
        if (status == BoxStatus.VOID) {
            throw new ElidomRuntimeException("BOX_VOIDED", "폐기된 박스는 변경할 수 없습니다.");
        }
    }

}
