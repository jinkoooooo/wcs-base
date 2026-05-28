package operato.logis.wcs.rest.box;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.entity.TbWcsPalletBox;
import operato.logis.wcs.service.impl.pallet.OutboundFinalizer;
import operato.logis.wcs.service.impl.pallet.PalletActivityResolver;
import operato.logis.wcs.service.impl.pallet.PalletBoxEditor;
import operato.logis.wcs.service.impl.pallet.PalletBoxFinalizer;
import operato.logis.wcs.service.impl.pallet.PalletBoxScanner;
import operato.logis.wcs.service.impl.pallet.PalletProgressService;
import operato.logis.wcs.service.impl.pallet.ReinboundCanceller;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.util.ValueUtil;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 박스 관리 전용 컨트롤러.
 * 박스 조회 / 스캔(입고·출고) / 진행률 / 출고 확정 / 수량 조정 / 폐기 / 활성 작업 조회.
 * 시험 사이클(시험 출고 지시·채취 입력·재입고·폐기) 엔드포인트는 PalletBcrController 가 담당.
 * 박스 분할(splitBox) 은 새 박스 모델에서 제거됨. 부분 출고는 picked_qty 누적으로 표현.
 */
@RestController
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/wcs/boxes")
@ServiceDesc(description = "WCS Box API")
public class BoxScanController {

    private final PalletBoxScanner palletBoxScanner;
    private final PalletProgressService palletProgressService;
    private final PalletActivityResolver palletActivityResolver;
    private final OutboundFinalizer outboundFinalizer;
    private final ReinboundCanceller reinboundCanceller;
    private final PalletBoxEditor palletBoxEditor;
    private final PalletBoxFinalizer palletBoxFinalizer;

    // 조회
    @GetMapping(value = "/pallet/{p}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TbWcsPalletBox> byPallet(@PathVariable("p") String p) {
        return palletProgressService.listByPallet(p);
    }

    @GetMapping(value = "/host-order/{h}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TbWcsPalletBox> byHost(@PathVariable("h") String h) {
        return palletProgressService.listByHostOrder(h);
    }

    @GetMapping(value = "/pallet/{p}/progress", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> progress(@PathVariable("p") String p) {
        return palletProgressService.progress(p);
    }

    @GetMapping(value = "/pallet/{p}/active", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> active(@PathVariable("p") String p) {
        return palletActivityResolver.resolveActivePallet(p);
    }

    // 박스 스캔
    @PostMapping(value = "/scan", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> scan(@RequestBody Map<String, Object> body) {
        return palletBoxScanner.scan(str(body.get("palletBarcode")), str(body.get("boxBarcode")));
    }

    @PostMapping(value = "/scan-out", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> scanOut(@RequestBody Map<String, Object> body) {
        return palletBoxScanner.scanOut(
                str(body.get("palletBarcode")),
                str(body.get("boxBarcode")),
                str(body.get("outboundOrderKey")));
    }

    // 출고 진행 / 확정
    @GetMapping(value = "/outbound/{orderKey}/progress", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> outboundProgress(@PathVariable("orderKey") String orderKey) {
        return palletProgressService.outboundProgress(orderKey);
    }

    @PostMapping(value = "/outbound/{orderKey}/finalize", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> finalizeOutbound(@PathVariable("orderKey") String orderKey) {
        return outboundFinalizer.finalizeOutbound(orderKey);
    }

    @PostMapping(value = "/reinbound/{orderKey}/cancel", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> cancelReinbound(@PathVariable("orderKey") String orderKey,
                                               @RequestBody(required = false) Map<String, Object> body) {
        String reason = body == null ? null : str(body.get("reason"));
        return reinboundCanceller.cancelReinbound(orderKey, reason);
    }

    // 수량 조정 / 폐기
    /**
     * 박스의 현재 잔량을 newQty 로 변경 (실사 보정 등).
     * 내부적으로 picked_qty 갱신 — total_qty 는 불변.
     * body: { "newQty": int } — newQty 는 조정 후 박스 잔량 (= total_qty - picked_qty).
     */
    @PostMapping(value = "/{id}/adjust", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> adjust(@PathVariable("id") String id, @RequestBody Map<String, Object> body) {
        TbWcsPalletBox b = palletBoxScanner.adjustQty(id, intOf(body.get("newQty")));
        return boxSummary(b);
    }

    /**
     * 박스 폐기 — VOID 로 전이. 재고 차감 없음.
     */
    @PostMapping(value = "/{id}/void", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> voidBox(@PathVariable("id") String id,
                                       @RequestBody(required = false) Map<String, Object> body) {
        String reason = body == null ? null : str(body.get("reason"));
        TbWcsPalletBox b = palletBoxScanner.voidBox(id, reason);
        return boxSummary(b);
    }

    // 입고 전 박스 편집 — total_qty 수정 / 박스 추가 / PENDING 폐기.
    // 변경 후 (item, lot) 그룹 합계가 host_order_item EA 수량과 일치해야 함 (불일치 시 롤백).
    /**
     * PENDING 박스의 total_qty 변경. remaining_qty 도 같은 값으로 동기화.
     * body: {@code { "totalQty": <int> }}.
     */
    @PostMapping(value = "/{id}/edit-total", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> editTotal(@PathVariable("id") String id,
                                         @RequestBody Map<String, Object> body) {
        TbWcsPalletBox b = palletBoxEditor.editTotalQty(id, intOf(body.get("totalQty")));
        return boxSummary(b);
    }

    /**
     * 파렛트에 새 박스 추가. host_order_item 에 존재하는 (item_code, lot_no) 만 허용.
     * body: {@code { "itemCode", "lotNo", "totalQty", "produceDate"?, "expiryDate"? }}.
     */
    @PostMapping(value = "/pallet/{p}/add", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> addBox(@PathVariable("p") String palletBarcode,
                                      @RequestBody Map<String, Object> body) {
        TbWcsPalletBox b = palletBoxEditor.addBox(
                palletBarcode,
                str(body.get("itemCode")),
                str(body.get("lotNo")),
                intOf(body.get("totalQty")),
                dateOf(body.get("produceDate")),
                dateOf(body.get("expiryDate")));
        return boxSummary(b);
    }

    /**
     * PENDING 박스 폐기. 라벨 인쇄된 박스는 거부.
     * body: {@code { "reason"? }}.
     */
    @PostMapping(value = "/{id}/void-pending", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> voidPending(@PathVariable("id") String id,
                                           @RequestBody(required = false) Map<String, Object> body) {
        String reason = body == null ? null : str(body.get("reason"));
        TbWcsPalletBox b = palletBoxEditor.voidPendingBox(id, reason);
        return boxSummary(b);
    }

    /**
     * "박스 추가" 모달 드롭다운 — 해당 파렛트의 host_order_item 목록을 EA 수량과 함께 반환.
     */
    @GetMapping(value = "/pallet/{p}/host-items", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Map<String, Object>> hostItems(@PathVariable("p") String palletBarcode) {
        return palletBoxEditor.listHostItemsForPallet(palletBarcode);
    }

    /**
     * 배치 편집 — UI draft 의 edits / additions / deletions 를 한 번에 반영.
     * 합계 검증은 마지막에 한 번만 수행 → 정합성 어긋난 중간 상태 허용.
     * body: { "edits":[{boxId,totalQty}], "additions":[{itemCode,lotNo,totalQty,produceDate?,expiryDate?}], "deletions":[{boxId,reason?}] }
     */
    @PostMapping(value = "/pallet/{p}/edit-batch", produces = MediaType.APPLICATION_JSON_VALUE)
    @SuppressWarnings("unchecked")
    public Map<String, Object> editBatch(@PathVariable("p") String palletBarcode,
                                         @RequestBody Map<String, Object> body) {
        List<Map<String, Object>> edits     = (List<Map<String, Object>>) body.getOrDefault("edits",     List.of());
        List<Map<String, Object>> additions = (List<Map<String, Object>>) body.getOrDefault("additions", List.of());
        List<Map<String, Object>> deletions = (List<Map<String, Object>>) body.getOrDefault("deletions", List.of());
        return palletBoxEditor.applyEditBatch(palletBarcode, edits, additions, deletions);
    }

    /**
     * 박스 일련번호 [확정] — (item, lot, 입고일자) 그룹 단위로 box_seq 와 box_barcode 를 부여.
     * 이미 확정된 박스는 skip (idempotent). 합계 검증 통과 + pre-inbound 상태 한정.
     */
    @PostMapping(value = "/pallet/{p}/finalize", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> finalizeBoxes(@PathVariable("p") String palletBarcode) {
        List<TbWcsPalletBox> finalized = palletBoxFinalizer.finalize(palletBarcode);
        List<Map<String, Object>> boxes = new java.util.ArrayList<>(finalized.size());
        for (TbWcsPalletBox b : finalized) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("boxId", b.getId());
            row.put("boxSeq", b.getBoxSeq());
            row.put("boxBarcode", b.getBoxBarcode());
            boxes.add(row);
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("palletBarcode", palletBarcode);
        r.put("finalizedCount", finalized.size());
        r.put("boxes", boxes);
        return r;
    }

    // helpers

    /** 박스 요약 응답 — 새 수량 모델 (totalQty / pickedQty / remainingQty) 모두 노출. */
    private static Map<String, Object> boxSummary(TbWcsPalletBox b) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("boxId", b.getId());
        r.put("boxBarcode", b.getBoxBarcode());
        r.put("totalQty", b.getTotalQty());
        r.put("pickedQty", b.getPickedQty());
        r.put("remainingQty", b.calcRemainingQty());
        r.put("boxStatus", b.getBoxStatus());
        return r;
    }

    private static String str(Object v) { return v == null ? null : v.toString().trim(); }

    private static int intOf(Object v) {
        if (v instanceof Number n) return n.intValue();
        try { return ValueUtil.isEmpty(v) ? 0 : Integer.parseInt(v.toString().trim()); }
        catch (Exception e) { return 0; }
    }

    private static Date dateOf(Object v) {
        if (v == null) return null;
        if (v instanceof Date d) return d;
        if (v instanceof Number n) return new Date(n.longValue());
        String s = v.toString().trim();
        if (s.isEmpty()) return null;
        try {
            // ISO 8601 (yyyy-MM-ddTHH:mm:ss[Z|±hh:mm]) 우선 — Vue 에서 Date.toJSON() 또는 datetime-local
            return Date.from(java.time.OffsetDateTime.parse(s).toInstant());
        } catch (Exception ignore) { /* fallthrough */ }
        try {
            return Date.from(java.time.LocalDateTime.parse(s).atZone(java.time.ZoneId.systemDefault()).toInstant());
        } catch (Exception ignore) { /* fallthrough */ }
        try {
            return Date.from(java.time.LocalDate.parse(s).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        } catch (Exception ignore) { return null; }
    }
}
