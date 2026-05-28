package operato.logis.wcs.service.impl.order.issuer;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.BoxStatus;
import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.consts.ShuttleOrderStatus;
import operato.logis.wcs.consts.SubOrderType;
import operato.logis.wcs.consts.UomType;
import operato.logis.wcs.dto.HostOrderApi;
import operato.logis.wcs.dto.WcsOrderCommand;
import operato.logis.wcs.entity.TbWcsHostOrder;
import operato.logis.wcs.entity.TbWcsPalletBox;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.service.impl.order.intake.OrderIntakeService;
import operato.logis.wcs.service.impl.order.lookup.OrderLookupUtils;
import operato.logis.wcs.service.impl.order.state.ShuttleOrderStateWriter;
import operato.logis.wcs.service.impl.pallet.PalletBoxDepleter;
import operato.logis.wcs.service.impl.pallet.PalletBoxStatusTransition;
import operato.logis.wcs.service.repository.PalletBoxRepository;
import operato.logis.wcs.service.repository.ShuttleOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.exception.server.ElidomRuntimeException;

import static operato.logis.wcs.common.util.check.Validator.requireFound;
import static operato.logis.wcs.common.util.check.Validator.requireNotEmpty;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 재입고 발급 — 시험 사이클 / 잔여 사이클.
 *
 * 두 경로 모두 진입점은 OrderIntakeService.execute.
 *   - issueSampleReinbound     : 시험 출고 완료 파렛트의 재입고 (운영자 [재입고] 버튼)
 *   - issueRemainderReinbound  : 부분 출고 후 잔여 박스 재입고 (운영자 [재동기화] 버튼)
 *   - findPendingSampleOut     : 외부 노출 — PalletProgressService 의 PENDING_SAMPLE 감지에 사용
 *
 * 박스 바코드 불변 — 재입고 시 박스 상태만 PRINTED 로 복원한다.
 */
@Service
@RequiredArgsConstructor
public class ReinboundIssuer {

    private static final Logger logger = LoggerFactory.getLogger(ReinboundIssuer.class);

    private final ShuttleOrderRepository shuttleOrderRepository;
    private final PalletBoxRepository palletBoxRepository;
    private final ShuttleOrderStateWriter shuttleOrderStateWriter;
    private final OrderLookupUtils orderLookup;
    private final PalletBoxDepleter palletBoxDepleter;

    @Lazy @Autowired private OrderIntakeService orderIntakeService;

    /**
     * 시험 채취 확정 + 재입고 발행 — frontend [확정 미리보기] 모달의 단일 트랜잭션.
     *
     * 1) depleteBoxIds 박스를 DEPLETED 처리 (미스캔 + 전량 채취 박스).
     * 2) issueSampleReinbound 호출 → 재입고 셔틀 발행.
     *
     * DEPLETED 박스는 재입고 items 집계에서 자동 제외되므로 정합성 유지.
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> finalizeSampleAndReinbound(String palletBarcode, List<String> depleteBoxIds) {
        requireNotEmpty(palletBarcode, "INVALID_PARAMETER", "파렛트 바코드가 입력되지 않았습니다.");

        // 1) 박스 DEPLETED 처리
        List<String> depleted = palletBoxDepleter.markBoxesDepleted(
                depleteBoxIds == null ? new ArrayList<>() : depleteBoxIds,
                "sample-finalize");

        // 2) 재입고 셔틀 발행
        TbWcsShuttleOrder inbound = issueSampleReinbound(palletBarcode);

        // 응답 빌드 — 미리보기 모달이 표시할 요약 정보
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("palletBarcode", palletBarcode);
        r.put("depletedCount", depleted.size());
        r.put("inboundOrderKey", inbound.getOrderKey());
        r.put("parentOrderKey", inbound.getParentOrderKey());
        r.put("hostOrderKey", inbound.getHostOrderKey());
        r.put("userMessage", "시험 채취 확정 — 소진 " + depleted.size() + "건, 재입고 셔틀 발행됨.");

        logger.info("[ Order ][ Issuer ] sample finalize - pallet={}, depleted={}, inbound={}",
                palletBarcode, depleted.size(), inbound.getOrderKey());
        return r;
    }

    /**
     * 시험 출고 재입고 발행.
     * 파렛트의 PENDING SAMPLE_OUT 셔틀을 parent 로 INBOUND 셔틀을 산출한다.
     */
    @Transactional(rollbackFor = Exception.class)
    public TbWcsShuttleOrder issueSampleReinbound(String palletBarcode) {
        requireNotEmpty(palletBarcode, "INVALID_PARAMETER", "파렛트 바코드가 입력되지 않았습니다.");

        // 박스 + parent SAMPLE_OUT 조회
        List<TbWcsPalletBox> boxes = palletBoxRepository.findByPalletBarcode(palletBarcode);
        requireFound(boxes, "PALLET_NOT_FOUND", "해당 파렛트를 찾을 수 없습니다.");
        TbWcsShuttleOrder parentSampleOut = findPendingSampleOut(palletBarcode);
        if (ValueUtil.isEmpty(parentSampleOut)) {
            throw new ElidomRuntimeException("NO_PENDING_SAMPLE_OUT",
                    "재입고 대상 시험용 출고가 없습니다. 시험용 출고 완료 후에만 재입고가 가능합니다.");
        }

        // host 주문 확보 + 중복 입고 차단
        TbWcsHostOrder h = requireHost(boxes.get(0).getHostOrderKey());
        rejectIfActiveInbound(palletBarcode);

        // 박스 잔량 → 재입고 items
        List<WcsOrderCommand.Item> orderItems = aggregateBoxesToCommandItems(boxes);
        requireFound(orderItems, "INVALID_PARAMETER", "재입고할 박스 수량이 없습니다.");

        // INBOUND command 빌드 + 산출
        WcsOrderCommand command = WcsOrderCommand.builder()
                .orderType(OrderType.INBOUND.codeAsString())
                .hostOrderKey(h.getHostOrderKey())
                .parentOrderKey(parentSampleOut.getOrderKey())
                .ownerCode(h.getOwnerCode())
                .eqGroupId(h.getEqGroupId())
                .priority(h.getPriority())
                .barCode(palletBarcode)
                .items(orderItems)
                .build();

        HostOrderApi.Response resp = orderIntakeService.execute(command);
        if (!resp.isSuccess()) {
            throw new ElidomRuntimeException("REINBOUND_ALLOC_FAILED", "재입고 산출 실패: " + resp.getMessage());
        }

        // parentOrderKey 보정 (intake 가 안 세팅하면 직접 set)
        TbWcsShuttleOrder inbound = shuttleOrderRepository.findByOrderKey(resp.getWcsOrderKey());
        if (ValueUtil.isEmpty(inbound.getParentOrderKey())) {
            inbound.setParentOrderKey(parentSampleOut.getOrderKey());
            shuttleOrderRepository.update(inbound, "parentOrderKey");
        }

        // 박스 PRINTED 복원
        restoreBoxesToPrinted(boxes);

        logger.info("[ Order ][ Issuer ] sample reinbound issued - pallet={}, parent={}, inbound={}, host={}",
                palletBarcode, parentSampleOut.getOrderKey(), inbound.getOrderKey(), h.getHostOrderKey());
        return inbound;
    }

    /**
     * 잔여 재입고 발행 — 운영자 [재동기화] 트리거.
     * 부분 출고된 OUTBOUND 셔틀을 parent 로 INBOUND 셔틀을 산출한다.
     */
    @Transactional(rollbackFor = Exception.class)
    public TbWcsShuttleOrder issueRemainderReinbound(String palletBarcode) {
        requireNotEmpty(palletBarcode, "INVALID_PARAMETER", "파렛트 바코드가 입력되지 않았습니다.");

        // 박스 + parent PARTIAL_OUT 조회
        List<TbWcsPalletBox> boxes = palletBoxRepository.findByPalletBarcode(palletBarcode);
        requireFound(boxes, "PALLET_NOT_FOUND", "해당 파렛트를 찾을 수 없습니다. (파렛트 바코드: " + palletBarcode + ")");
        TbWcsShuttleOrder parentOutbound = findReinboundablePartialOutbound(palletBarcode);
        if (ValueUtil.isEmpty(parentOutbound)) {
            throw new ElidomRuntimeException("NO_PARTIAL_OUTBOUND", "잔여 재입고 대상 출고 주문이 없습니다.");
        }

        // 과거 데이터 호환 — 자동 PRE 발급 시절 placeholder 가 남아 있으면 CANCEL 후 신규 산출
        TbWcsShuttleOrder stalePreIssued = findNonCancelledChildInbound(parentOutbound.getOrderKey());
        if (ValueUtil.isNotEmpty(stalePreIssued)) {
            shuttleOrderStateWriter.markCancelled(stalePreIssued);
            logger.info("[ Order ][ Issuer ] remainder supersede stale - parent={}, stale={}",
                    parentOutbound.getOrderKey(), stalePreIssued.getOrderKey());
        }

        // 중복 입고 차단 + 잔량 집계
        rejectIfActiveInbound(palletBarcode);
        List<WcsOrderCommand.Item> orderItems = aggregateBoxesToCommandItems(boxes);
        requireFound(orderItems, "INVALID_PARAMETER", "재입고할 잔여 박스 수량이 없습니다.");

        // INBOUND command 빌드 + 산출
        TbWcsHostOrder h = requireHost(parentOutbound.getHostOrderKey());
        WcsOrderCommand command = WcsOrderCommand.builder()
                .orderType(OrderType.INBOUND.codeAsString())
                .hostOrderKey(h.getHostOrderKey())
                .parentOrderKey(parentOutbound.getOrderKey())
                .ownerCode(h.getOwnerCode())
                .eqGroupId(h.getEqGroupId())
                .priority(h.getPriority())
                .barCode(palletBarcode)
                .items(orderItems)
                .build();

        HostOrderApi.Response resp = orderIntakeService.execute(command);
        if (!resp.isSuccess()) {
            throw new ElidomRuntimeException("REMAINDER_REINBOUND_FAILED",
                    "잔여 재입고 산출 실패: " + resp.getMessage());
        }

        // parentOrderKey 보정
        TbWcsShuttleOrder inbound = shuttleOrderRepository.findByOrderKey(resp.getWcsOrderKey());
        if (ValueUtil.isEmpty(inbound.getParentOrderKey())) {
            inbound.setParentOrderKey(parentOutbound.getOrderKey());
            shuttleOrderRepository.update(inbound, "parentOrderKey");
        }

        // 박스 PRINTED 복원
        restoreBoxesToPrinted(boxes);

        logger.info("[ Order ][ Issuer ] remainder reinbound issued - pallet={}, parent={}, inbound={}, host={}",
                palletBarcode, parentOutbound.getOrderKey(), inbound.getOrderKey(), h.getHostOrderKey());
        return inbound;
    }

    /**
     * 파렛트의 PENDING SAMPLE_OUT 셔틀 검색.
     * PalletProgressService 가 PENDING_SAMPLE 상태 감지에 사용.
     */
    public TbWcsShuttleOrder findPendingSampleOut(String palletBarcode) {
        if (ValueUtil.isEmpty(palletBarcode)) return null;
        Integer cancelledCode = ShuttleOrderStatus.CANCELLED.codeAsIntOrNull();

        for (TbWcsShuttleOrder s : shuttleOrderRepository.findByBarcode(palletBarcode)) {
            // OUTBOUND + SAMPLE_OUT + 종결 상태만 후보
            if (!OrderType.OUTBOUND.matches(s.getOrderType())) continue;
            if (SubOrderType.fromOrNormal(s.getSubOrderType()) != SubOrderType.SAMPLE_OUT) continue;
            Integer st = s.getOrderStatus();
            if (ValueUtil.isEmpty(st) || !ShuttleOrderStatus.isFinalStatus(st)) continue;

            // 자식 INBOUND 가 모두 CANCELLED 면 재입고 가능
            boolean blocked = false;
            for (TbWcsShuttleOrder child : shuttleOrderRepository.findByParentOrderKey(s.getOrderKey())) {
                if (!OrderType.INBOUND.matches(child.getOrderType())) continue;
                Integer cs = child.getOrderStatus();
                if (ValueUtil.isEmpty(cs) || !cs.equals(cancelledCode)) { blocked = true; break; }
            }
            if (!blocked) return s;
        }
        return null;
    }

    /**
     * 잔여 재입고 가능한 PARTIAL_OUT OUTBOUND 셔틀 검색 — 가장 최근 생성된 것 선택.
     */
    private TbWcsShuttleOrder findReinboundablePartialOutbound(String palletBarcode) {
        if (ValueUtil.isEmpty(palletBarcode)) return null;
        TbWcsShuttleOrder candidate = null;
        for (TbWcsShuttleOrder s : shuttleOrderRepository.findByBarcode(palletBarcode)) {
            if (!OrderType.OUTBOUND.matches(s.getOrderType())) continue;
            if (SubOrderType.fromOrNormal(s.getSubOrderType()) != SubOrderType.PARTIAL_OUT) continue;
            // 더 최근 생성된 것 선택
            if (ValueUtil.isEmpty(candidate)
                    || (ValueUtil.isNotEmpty(s.getCreatedAt())
                    && ValueUtil.isNotEmpty(candidate.getCreatedAt())
                    && s.getCreatedAt().after(candidate.getCreatedAt()))) {
                candidate = s;
            }
        }
        return candidate;
    }

    /**
     * parent 의 CANCEL 아닌 자식 INBOUND 검색 — stale placeholder 감지용.
     */
    private TbWcsShuttleOrder findNonCancelledChildInbound(String parentOrderKey) {
        Integer cancelledCode = ShuttleOrderStatus.CANCELLED.codeAsIntOrNull();
        for (TbWcsShuttleOrder c : shuttleOrderRepository.findByParentOrderKey(parentOrderKey)) {
            if (!OrderType.INBOUND.matches(c.getOrderType())) continue;
            Integer st = c.getOrderStatus();
            if (ValueUtil.isEmpty(st) || !st.equals(cancelledCode)) return c;
        }
        return null;
    }

    /**
     * 파렛트에 진행 중 INBOUND 가 있으면 중복 발행 차단.
     */
    private void rejectIfActiveInbound(String palletBarcode) {
        for (TbWcsShuttleOrder s : shuttleOrderRepository.findByBarcode(palletBarcode)) {
            if (!OrderType.INBOUND.matches(s.getOrderType())) continue;
            Integer st = s.getOrderStatus();
            if (ShuttleOrderStatus.isActive(st)) {
                throw new ElidomRuntimeException("INBOUND_ALREADY_ACTIVE",
                        "이 파렛트에 이미 진행 중인 입고가 있습니다. (입고 주문: " + s.getOrderKey() + ")");
            }
        }
    }

    /**
     * 박스를 재입고 가능한 상태(PRINTED) 로 복원한다.
     *
     * SCANNED 인데 picked_qty > 0 (부분 출고된 박스) 또는 PENDING 박스만 복원.
     * DEPLETED / VOID / SCANNED+picked=0 은 그대로 둠. 박스 바코드 불변.
     */
    private void restoreBoxesToPrinted(List<TbWcsPalletBox> boxes) {
        Date now = new Date();
        for (TbWcsPalletBox b : boxes) {
            BoxStatus cur = BoxStatus.fromCode(b.getBoxStatus());
            int picked = ValueUtil.isEmpty(b.getPickedQty()) ? 0 : b.getPickedQty();

            boolean needRestore = cur == BoxStatus.PENDING || (cur == BoxStatus.SCANNED && picked > 0);
            if (!needRestore) continue;

            PalletBoxStatusTransition.restore(b, BoxStatus.PRINTED, "reinbound-restore");
            b.setPrintedAt(now);
            b.setScannedAt(null);
            // 시험 부분 채취 박스의 누적 picked_qty 정리 — 재입고 시점에 잔량만 의미 있음
            b.setPickedQty(0);
            palletBoxRepository.update(b, "boxStatus", "printedAt", "scannedAt", "pickedQty");
        }
    }

    /**
     * 박스 잔량을 SKU+Lot 별로 집계해 재입고 items 생성. VOID/DEPLETED + 잔량 0 제외.
     */
    private List<WcsOrderCommand.Item> aggregateBoxesToCommandItems(List<TbWcsPalletBox> boxes) {
        Map<String, WcsOrderCommand.Item> agg = new LinkedHashMap<>();
        for (TbWcsPalletBox b : boxes) {
            BoxStatus st = BoxStatus.fromCode(b.getBoxStatus());
            if (st == BoxStatus.VOID || st == BoxStatus.DEPLETED) continue;
            int remaining = b.calcRemainingQty();
            if (remaining <= 0 || ValueUtil.isEmpty(b.getItemCode())) continue;

            String key = itemKey(b.getItemCode(), b.getLotNo());
            WcsOrderCommand.Item existing = agg.get(key);
            if (ValueUtil.isEmpty(existing)) {
                agg.put(key, WcsOrderCommand.Item.builder()
                        .itemCode(b.getItemCode())
                        .lotNo(b.getLotNo())
                        .qty(remaining)
                        .uom(UomType.EA.code())
                        .build());
            } else {
                existing.setQty(existing.getQty() + remaining);
            }
        }
        return new ArrayList<>(agg.values());
    }

    /**
     * host 주문 확보 — 없으면 ElidomRuntimeException.
     */
    private TbWcsHostOrder requireHost(String hostOrderKey) {
        return orderLookup.getHostOrderOrThrow(hostOrderKey);
    }

    /**
     * SKU + LOT 결합 키 생성.
     */
    private static String itemKey(String sku, String lot) {
        return (ValueUtil.isEmpty(sku) ? "" : sku) + "::" + (ValueUtil.isEmpty(lot) ? "" : lot);
    }
}
