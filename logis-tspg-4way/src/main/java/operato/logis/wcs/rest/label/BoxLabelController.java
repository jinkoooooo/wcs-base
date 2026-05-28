package operato.logis.wcs.rest.label;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.common.validation.CommentValidator;
import operato.logis.wcs.consts.BoxStatus;
import operato.logis.wcs.dto.BatchLabelReissueRequest;
import operato.logis.wcs.dto.LabelReissueRequest;
import operato.logis.wcs.entity.TbWcsPalletBox;
import operato.logis.wcs.service.impl.label.LabelGenerator;
import operato.logis.wcs.service.impl.pallet.PalletBoxFactory;
import operato.logis.wcs.service.impl.pallet.PalletBoxPrinter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 박스/파렛트 라벨 발행 REST.
 * 라벨 데이터(JSON) / ZPL(Zebra 직접 송신) / PDF(뷰어 출력) 세 형태 + 발행·재발행 처리.
 */
@RestController
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/wcs/labels")
@ServiceDesc(description = "WCS Label API")
public class BoxLabelController {

    private final LabelGenerator labels;
    private final PalletBoxFactory boxes;
    private final PalletBoxPrinter printer;

    @GetMapping(value = "/pallet/{p}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> pallet(@PathVariable("p") String p) {
        return labels.buildPalletLabel(p);
    }

    @GetMapping(value = "/pallet/{p}/boxes", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> all(@PathVariable("p") String p) {
        return labels.buildAllBoxLabels(p);
    }

    @GetMapping(value = "/box/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> box(@PathVariable("id") String id) {
        return labels.buildBoxLabel(id);
    }

    @PostMapping(value = "/pallet/{p}/mark-printed", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> markPallet(@PathVariable("p") String p) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("palletBarcode", p);
        r.put("markedCount", printer.markPalletPrinted(p));
        return r;
    }

    /**
     * 박스 라벨 발행/재발행 (단건).
     * 분기:
     *   - PENDING && printCount == 0 (첫 발행) → COMMENT 면제, markPrinted
     *   - 그 외 모든 상태 (PRINTED/SCANNED/DEPLETED 등) → COMMENT 필수 (2~500자), markReissued
     * SCANNED 박스도 사유만 입력하면 재발행 가능. markReissued 는 상태 안 건드리고 printCount/printedAt 만 갱신.
     */
    @PostMapping(value = "/box/{id}/reissue",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> reissue(@PathVariable("id") String id,
                                       @RequestBody(required = false) LabelReissueRequest body) {
        TbWcsPalletBox current = boxes.findById(id);
        if (ValueUtil.isEmpty(current)) {
            throw new ElidomRuntimeException("BOX_NOT_FOUND",
                    "해당 박스를 찾을 수 없습니다. (박스 ID: " + id + ")");
        }
        int count = current.getPrintCount() == null ? 0 : current.getPrintCount();
        BoxStatus status = BoxStatus.fromCode(current.getBoxStatus());
        String comment = body == null ? null : body.getComment();

        TbWcsPalletBox updated;
        // 첫 발행 케이스 — PENDING 이면서 한번도 인쇄된 적 없는 박스만 사유 면제
        if (count == 0 && status == BoxStatus.PENDING) {
            updated = printer.markPrinted(id);
        } else {
            // 그 외 모든 상태(SCANNED/PRINTED/DEPLETED 등) — 사유 입력 시 재발행 허용
            CommentValidator.requireValid("comment", comment);
            updated = printer.markReissued(id, comment.trim());
        }
        Map<String, Object> pay = labels.buildBoxLabel(id);
        pay.put("boxStatus", updated.getBoxStatus());
        pay.put("printCount", updated.getPrintCount());
        return pay;
    }

    /**
     * 박스 라벨 일괄 재발행 — 다수 박스를 1회 트랜잭션으로 처리.
     * 모두 첫 발행 대상이면 COMMENT 면제, 재발행 대상이 섞이면 COMMENT 필수 (2~500자).
     * 응답으로 각 박스의 라벨 페이로드 N건 반환 — 1회 호출로 통합 인쇄 가능.
     */
    @PostMapping(value = "/boxes/reissue-batch",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> reissueBatch(@RequestBody BatchLabelReissueRequest body) {
        if (body == null || ValueUtil.isEmpty(body.getBoxIds())) {
            throw new ElidomRuntimeException("INVALID_PARAMETER",
                    "박스 ID 목록(boxIds) 이 필요합니다.");
        }
        String comment = body.getComment() == null ? null : body.getComment().trim();

        // markBoxesReissued 내부에서 첫 발행 묶음 여부 판정 후 comment 필수성 검증
        List<String> processedIds = printer.markBoxesReissued(body.getBoxIds(), comment);

        // 라벨 페이로드 N건 — 처리된 박스 순서 보존
        List<Map<String, Object>> labelList = new ArrayList<>(processedIds.size());
        for (String id : processedIds) {
            labelList.add(labels.buildBoxLabel(id));
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("reissuedCount", processedIds.size());
        resp.put("labels", labelList);
        return resp;
    }

    /**
     * 파렛트 라벨 재발행
     * 파렛트 내 박스 중 첫 발행 대상(PENDING && printCount=0)과 폐기된 박스(VOID)를 제외한
     * 모든 박스에 일괄 재발행. COMMENT 필수 (2~500자), 박스 별 감사 로그 N건 적재.
     */
    @PostMapping(value = "/pallet/{p}/reissue",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> reissuePallet(@PathVariable("p") String pallet,
                                             @RequestBody LabelReissueRequest body) {
        String comment = body == null ? null : body.getComment();
        CommentValidator.requireValid("comment", comment);
        int n = printer.markPalletReissued(pallet, comment.trim());
        return Map.of("palletBarcode", pallet, "reissuedBoxes", n);
    }

    // Zebra ZPL — Zebra 프린터에 직접 송신할 수 있는 ZPL 텍스트 반환
    @GetMapping(value = "/pallet/{p}/zpl", produces = "text/plain;charset=UTF-8")
    public String palletZpl(@PathVariable("p") String p) {
        return labels.buildPalletZpl(p);
    }

    @GetMapping(value = "/box/{id}/zpl", produces = "text/plain;charset=UTF-8")
    public String boxZpl(@PathVariable("id") String id) {
        return labels.buildBoxZpl(id);
    }

    // PDF — 일반 프린터/뷰어 출력용. attachment(다운로드) 또는 inline(미리보기) 모드.
    @GetMapping(value = "/pallet/{p}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> palletPdf(@PathVariable("p") String p,
                                            @RequestParam(name = "inline", defaultValue = "true") boolean inline) {
        byte[] pdf = labels.buildPalletPdf(p);
        return pdfResponse(pdf, "pallet-" + p + ".pdf", inline);
    }

    @GetMapping(value = "/box/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> boxPdf(@PathVariable("id") String id,
                                         @RequestParam(name = "inline", defaultValue = "true") boolean inline) {
        byte[] pdf = labels.buildBoxPdf(id);
        return pdfResponse(pdf, "box-" + id + ".pdf", inline);
    }

    /** 박스 1매를 대/중/소/EA 단위로 분해해 페이지별로 출력한 PDF. */
    @GetMapping(value = "/box/{id}/units/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> boxUnitsPdf(@PathVariable("id") String id,
                                              @RequestParam(name = "inline", defaultValue = "true") boolean inline) {
        byte[] pdf = labels.buildBoxUnitPdf(id);
        return pdfResponse(pdf, "box-" + id + "-units.pdf", inline);
    }

    @GetMapping(value = "/pallet/{p}/boxes/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> palletBoxesPdf(@PathVariable("p") String p,
                                                 @RequestParam(name = "inline", defaultValue = "true") boolean inline) {
        byte[] pdf = labels.buildAllBoxesPdf(p);
        return pdfResponse(pdf, "pallet-" + p + "-boxes.pdf", inline);
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] body, String filename, boolean inline) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_PDF);
        h.setContentLength(body.length);
        h.set(HttpHeaders.CONTENT_DISPOSITION,
                (inline ? "inline" : "attachment") + "; filename=\"" + filename + "\"");
        return new ResponseEntity<>(body, h, HttpStatus.OK);
    }
}