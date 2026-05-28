package operato.logis.wcs.rest.pallet;

import operato.logis.wcs.consts.BoxStatus;
import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.consts.SubOrderType;
import operato.logis.wcs.consts.UomType;
import operato.logis.wcs.dto.AdminBypassRequest;
import operato.logis.wcs.dto.HostOrderApi;
import operato.logis.wcs.dto.WcsOrderCommand;
import operato.logis.wcs.entity.TbWcsHostOrder;
import operato.logis.wcs.entity.TbWcsPalletBox;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.service.impl.order.intake.OrderIntakeService;
import operato.logis.wcs.service.impl.order.issuer.ReinboundIssuer;
import operato.logis.wcs.service.impl.qctest.SampleDiscardIssuer;
import operato.logis.wcs.service.impl.pallet.OutboundAutoScanner;
import operato.logis.wcs.service.impl.pallet.OutboundFinalizer;
import operato.logis.wcs.service.impl.pallet.PalletBoxScanner;
import operato.logis.wcs.service.impl.pallet.PalletProgressService;
import operato.logis.wcs.service.impl.pallet.PalletReleaseService;
import operato.logis.wcs.service.repository.HostOrderRepository;
import lombok.RequiredArgsConstructor;
import operato.logis.wcs.service.repository.PalletBoxRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/wcs/pallets")
@ServiceDesc(description = "WCS Pallet BCR")
public class PalletBcrController {

    private final PalletBoxScanner palletBoxScanner;
    private final PalletProgressService palletProgressService;
    private final OutboundFinalizer outboundFinalizer;
    private final OutboundAutoScanner outboundAutoScanner;
    private final ReinboundIssuer reinboundIssuer;
    private final SampleDiscardIssuer sampleDiscardIssuer;
    private final OrderIntakeService wcsOrderService;
    private final PalletBoxRepository palletBoxRepository;
    private final HostOrderRepository hostOrderRepository;
    private final PalletReleaseService palletReleaseService;

    // 입고 release - BCR 스캔 완료/관리자 우회 후 host 를 READY_FOR_ALLOC 로 전이. PalletReleaseService 위임.
    @PostMapping(value = "/{p}/release", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> release(@PathVariable("p") String p,
                                       @RequestBody(required = false) AdminBypassRequest body) {
        boolean adminBypass = body != null && body.isAdminBypass();
        return palletReleaseService.release(p, adminBypass);
    }

    // 시험 사이클 - sample-out / sample-taken / reinbound / discard

    /**
     * 시험용 출고 지시 — SAMPLE_OUT 마킹한 OUTBOUND 를 정상 산출 경로로 발행.
     *
     * 박스 잔량(remaining_qty)을 SKU+Lot 별로 집계하여 OUTBOUND items 생성.
     * VOID/DEPLETED 박스 및 잔량 0 박스는 제외.
     */
    @PostMapping(value = "/{p}/sample-out", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> sampleOut(@PathVariable("p") String p) {
        if (ValueUtil.isEmpty(p)) throw new ElidomRuntimeException("INVALID_PARAMETER", "p");

        List<TbWcsPalletBox> boxes = palletBoxRepository.findByPalletBarcode(p);
        if (ValueUtil.isEmpty(boxes))
            throw new ElidomRuntimeException("PALLET_NOT_FOUND",
                    "해당 파렛트를 찾을 수 없습니다. (파렛트 바코드: " + p + ")");

        TbWcsHostOrder h = hostOrderRepository.findByHostOrderKey(boxes.get(0).getHostOrderKey());
        if (ValueUtil.isEmpty(h)) throw new ElidomRuntimeException("ORDER_NOT_FOUND",
                "호스트 주문을 찾을 수 없습니다.");

        // 박스 잔량 → OUTBOUND 아이템 집계 (SKU+Lot)
        Map<String, WcsOrderCommand.Item> agg = new LinkedHashMap<>();
        for (TbWcsPalletBox b : boxes) {
            BoxStatus st = BoxStatus.fromCode(b.getBoxStatus());
            if (st == BoxStatus.VOID || st == BoxStatus.DEPLETED) continue;
            int remaining = b.calcRemainingQty();
            if (remaining <= 0 || ValueUtil.isEmpty(b.getItemCode())) continue;
            String key = b.getItemCode() + "::" + (ValueUtil.isEmpty(b.getLotNo()) ? "" : b.getLotNo());
            WcsOrderCommand.Item existing = agg.get(key);
            if (ValueUtil.isEmpty(existing)) {
                agg.put(key, WcsOrderCommand.Item.builder()
                        .itemCode(b.getItemCode()).lotNo(b.getLotNo())
                        .qty(remaining).uom(UomType.EA.code()).build());
            } else {
                existing.setQty(existing.getQty() + remaining);
            }
        }
        List<WcsOrderCommand.Item> items = new ArrayList<>(agg.values());
        if (ValueUtil.isEmpty(items))
            throw new ElidomRuntimeException("INVALID_PARAMETER", "출고할 박스가 없습니다.");

        WcsOrderCommand command = WcsOrderCommand.builder()
                .orderType(OrderType.OUTBOUND.codeAsString())
                .subOrderType(SubOrderType.SAMPLE_OUT.code())
                .hostOrderKey(h.getHostOrderKey())
                .ownerCode(h.getOwnerCode())
                .eqGroupId(h.getEqGroupId())
                .priority(h.getPriority())
                .barCode(p)
                .items(items)
                .build();

        HostOrderApi.Response resp = wcsOrderService.execute(command);
        if (!resp.isSuccess()) {
            throw new ElidomRuntimeException("SAMPLE_OUT_ALLOC_FAILED",
                    "시험 출고 산출 실패: " + resp.getMessage());
        }

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("palletBarcode", p);
        r.put("outboundOrderKey", resp.getWcsOrderKey());
        r.put("hostOrderKey", h.getHostOrderKey());
        r.put("created", true);
        r.put("userMessage", "시험용 출고가 지시되었습니다.");
        return r;
    }

    /**
     * 박스 채취 수량 입력 — SAMPLE_OUT 완료 후 작업자가 박스별 채취량 입력 시 호출.
     *
     * body: {@code { "boxId": "...", "takenQty": <int> }}.
     * 박스 바코드는 불변이므로 라벨 재발행 불필요.
     */
    @PostMapping(value = "/{p}/sample-taken", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> sampleTaken(@PathVariable("p") String p,
                                           @RequestBody Map<String, Object> body) {
        if (ValueUtil.isEmpty(body)) throw new ElidomRuntimeException("INVALID_PARAMETER", "body");
        String boxId = (String) body.get("boxId");
        Object qtyObj = body.get("takenQty");
        if (ValueUtil.isEmpty(boxId)) throw new ElidomRuntimeException("INVALID_PARAMETER", "boxId");
        if (ValueUtil.isEmpty(qtyObj)) throw new ElidomRuntimeException("INVALID_PARAMETER", "takenQty");
        int taken;
        try { taken = Integer.parseInt(qtyObj.toString()); }
        catch (Exception e) {
            throw new ElidomRuntimeException("INVALID_PARAMETER", "takenQty must be number");
        }

        TbWcsPalletBox updated = palletBoxScanner.reportSampleTaken(boxId, taken);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("palletBarcode", p);
        r.put("boxId", updated.getId());
        r.put("boxBarcode", updated.getBoxBarcode());
        r.put("totalQty", updated.getTotalQty());
        r.put("pickedQty", updated.getPickedQty());
        r.put("remainingQty", updated.calcRemainingQty());
        r.put("boxStatus", updated.getBoxStatus());
        r.put("userMessage", "채취 수량이 반영되었습니다.");
        return r;
    }

    /**
     * 시험 후 재입고 — 일반 INBOUND 발행. parent_order_key = 원 SAMPLE_OUT.
     */
    @PostMapping(value = "/{p}/reinbound", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> reinbound(@PathVariable("p") String p) {
        TbWcsShuttleOrder inbound = reinboundIssuer.issueSampleReinbound(p);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("palletBarcode", p);
        r.put("inboundOrderKey", inbound.getOrderKey());
        r.put("hostOrderKey", inbound.getHostOrderKey());
        r.put("parentOrderKey", inbound.getParentOrderKey());
        r.put("created", true);
        r.put("userMessage", "재입고가 등록되었습니다. 박스 스캔으로 진행해주세요.");
        return r;
    }

    /**
     * 시험 채취 확정 + 재입고 — frontend [확정 미리보기] 모달의 단일 호출.
     *
     * body: {@code { "depleteBoxIds": ["box-uuid-1", "box-uuid-2", ...] }}
     *   depleteBoxIds — 미스캔 박스 + picked>=total (전량 채취) 박스 ID. 모두 DEPLETED 처리.
     *   그 후 {@link ReinboundIssuer#issueSampleReinbound} 호출로 재입고 셔틀 발행.
     */
    @PostMapping(value = "/{p}/sample-finalize-reinbound", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> sampleFinalizeReinbound(@PathVariable("p") String p,
                                                       @RequestBody(required = false)
                                                       Map<String, Object> body) {
        List<String> depleteBoxIds = new ArrayList<>();
        if (body != null && body.get("depleteBoxIds") instanceof List<?> raw) {
            for (Object o : raw) {
                if (o != null) depleteBoxIds.add(String.valueOf(o));
            }
        }
        return reinboundIssuer.finalizeSampleAndReinbound(p, depleteBoxIds);
    }

    /**
     * 시험 부적합 폐기 — SAMPLE_DISCARD + host TEST_FAILED.
     */
    @PostMapping(value = "/{p}/discard", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> discard(@PathVariable("p") String p,
                                       @RequestBody(required = false) Map<String, Object> body) {
        String reason = body == null ? null : (String) body.get("reason");
        TbWcsShuttleOrder discard = sampleDiscardIssuer.issueSampleDiscard(p, reason);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("palletBarcode", p);
        r.put("discardOrderKey", discard.getOrderKey());
        r.put("hostOrderKey", discard.getHostOrderKey());
        r.put("parentOrderKey", discard.getParentOrderKey());
        r.put("discarded", true);
        r.put("userMessage", "폐기 처리되었습니다.");
        return r;
    }

    // 부분 출고 / 잔여 재입고

    /**
     * 부분 출고 — 박스 보유 수량 중 일부만 출고 처리.
     *
     * body: {@code { "boxId": "...", "outboundQty": <int> }}.
     */
    @PostMapping(value = "/box/{id}/partial-outbound", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> partialOutbound(@PathVariable("id") String id,
                                               @RequestBody Map<String, Object> body) {
        if (ValueUtil.isEmpty(body)) throw new ElidomRuntimeException("INVALID_PARAMETER", "body");
        Object qtyObj = body.get("outboundQty");
        if (ValueUtil.isEmpty(qtyObj)) throw new ElidomRuntimeException("INVALID_PARAMETER", "outboundQty");
        int outQty;
        try { outQty = Integer.parseInt(qtyObj.toString()); }
        catch (Exception e) {
            throw new ElidomRuntimeException("INVALID_PARAMETER", "outboundQty must be number");
        }

        TbWcsPalletBox updated = palletBoxScanner.processPartialOutbound(id, outQty);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("boxId", updated.getId());
        r.put("boxBarcode", updated.getBoxBarcode());
        r.put("totalQty", updated.getTotalQty());
        r.put("pickedQty", updated.getPickedQty());
        r.put("remainingQty", updated.calcRemainingQty());
        r.put("boxStatus", updated.getBoxStatus());
        r.put("userMessage", "부분 출고 처리되었습니다.");
        return r;
    }

    /**
     * 잔여 재입고 — 사전 발급된 자동 재입고가 있으면 sync, 없으면 신규 발급.
     */
    @PostMapping(value = "/{p}/remainder-reinbound", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> remainderReinbound(@PathVariable("p") String p) {
        TbWcsShuttleOrder inbound = reinboundIssuer.issueRemainderReinbound(p);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("palletBarcode", p);
        r.put("inboundOrderKey", inbound.getOrderKey());
        r.put("hostOrderKey", inbound.getHostOrderKey());
        r.put("parentOrderKey", inbound.getParentOrderKey());
        r.put("userMessage", "잔여 재입고가 동기화되었습니다.");
        return r;
    }

    // 출고 확정 (파렛트 기준 — 입고 release 와 대칭)

    /**
     * 출고 확정 — 파렛트 바코드 기준.
     *
     * 입고의 {@code /release} 와 대칭. 파렛트의 활성 출고가 정확히 1건일 때 그 출고를 확정한다.
     * autoFinalize 출고(SAMPLE_OUT/DISCARD)는 대상에서 제외된다.
     *
     * body: {@code { "adminBypass": <boolean> }} — true 면 미차감 박스를 일괄 DEPLETED 처리.
     * 단, 부분 출고 박스가 있으면 우회 거부 (전체 출고만 우회 허용).
     */
    @PostMapping(value = "/{p}/finalize-outbound", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> finalizeOutboundByPallet(@PathVariable("p") String p,
                                                        @RequestBody(required = false) AdminBypassRequest body) {
        if (ValueUtil.isEmpty(p)) {
            throw new ElidomRuntimeException("INVALID_PARAMETER", "p");
        }

        TbWcsShuttleOrder outbound = palletProgressService.findActiveOutbound(p);
        boolean adminBypass = body != null && body.isAdminBypass();

        Map<String, Object> result = outboundFinalizer.finalizeOutbound(
                outbound.getOrderKey(), adminBypass);

        // 파렛트 기준 호출이므로 응답에 palletBarcode 도 함께 노출 — release 와 대칭.
        Map<String, Object> wrapped = new LinkedHashMap<>();
        wrapped.put("palletBarcode", p);
        wrapped.putAll(result);
        return wrapped;
    }

    /**
     * 관리자 우회 — 박스 스캔 자동 완료 (출고 확정 미수행).
     *
     * 미스캔 박스의 picked_qty 를 부족분만큼 자동 채워 SCANNED 로 전이. 셔틀 finalize 는 안 함.
     * 사용자 [출고 확정] 명시 클릭으로 진행해야 박스 finalize + 셔틀 90 처리됨.
     *
     * 일반 작업자가 박스 스캔 단계를 건너뛸 뿐 나머지 흐름은 동일.
     */
    @PostMapping(value = "/{p}/auto-scan-outbound-boxes", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> autoScanOutboundBoxes(@PathVariable("p") String p) {
        if (ValueUtil.isEmpty(p)) {
            throw new ElidomRuntimeException("INVALID_PARAMETER", "p");
        }
        TbWcsShuttleOrder outbound = palletProgressService.findActiveOutbound(p);
        Map<String, Object> result = outboundAutoScanner.autoScanOutboundBoxes(outbound.getOrderKey());

        Map<String, Object> wrapped = new LinkedHashMap<>();
        wrapped.put("palletBarcode", p);
        wrapped.putAll(result);
        return wrapped;
    }

    /**
     * 관리자 우회 — 시험 채취 단계 박스 전수 자동 스캔 (채취/확정과 분리).
     *
     * 모든 PRINTED 박스를 SCANNED 로 전이. picked_qty 안 건드림.
     * 사용자가 채취 입력 후 [확정 미리보기] 로 진행해야 박스 finalize + 재입고 발행됨.
     */
    @PostMapping(value = "/{p}/auto-scan-sample-boxes", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> autoScanSampleBoxes(@PathVariable("p") String p) {
        if (ValueUtil.isEmpty(p)) {
            throw new ElidomRuntimeException("INVALID_PARAMETER", "p");
        }
        return outboundAutoScanner.autoScanSampleBoxes(p);
    }
}