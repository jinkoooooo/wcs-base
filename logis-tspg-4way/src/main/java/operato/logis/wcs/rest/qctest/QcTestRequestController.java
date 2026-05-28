package operato.logis.wcs.rest.qctest;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.QcTestRequestStatus;
import operato.logis.wcs.consts.WcsError;
import operato.logis.wcs.dto.qctest.QcRequestBatchSave;
import operato.logis.wcs.entity.ExtTbInventoryItemMaster;
import operato.logis.wcs.entity.TbWcsQcTestRequest;
import operato.logis.wcs.service.impl.qctest.QcRequestService;
import operato.logis.wcs.service.impl.query.qctest.QcTestRequestQueryService;
import operato.logis.wcs.service.repository.InventoryItemMasterRepository;
import operato.logis.wcs.common.util.time.LocalDateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.util.ValueUtil;

import java.time.LocalDate;
import static operato.logis.wcs.common.util.lang.ParseUtils.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * QC 시험 의뢰 REST. 운영자 조회/발행/삭제 + LIMS pull(IF02 발신) 진입점.
 */
@RestController
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/wcs/qc-test/request")
@ServiceDesc(description = "QcTestRequest Master Service API")
public class QcTestRequestController {

    private static final Logger logger = LoggerFactory.getLogger(QcTestRequestController.class);

    private final QcRequestService service;
    private final InventoryItemMasterRepository itemMasterRepository;
    private final QcTestRequestQueryService queryService;

    @RequestMapping(value = "/search", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "QC 시험 의뢰 페이징 검색 - CommonPage")
    public Map<String, Object> search(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "sort",  required = false) String sort,
            @RequestParam(value = "page",  defaultValue = "1")  int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return queryService.search(query, sort, page, limit);
    }

    @RequestMapping(value = "/delete", method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "QC 시험 의뢰 다건 삭제")
    public Map<String, Object> deleteMany(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) body.get("ids");
        int deleted = service.deleteAll(ids);
        Map<String, Object> r = new HashMap<>();
        r.put("success", true);
        r.put("deletedCount", deleted);
        return r;
    }

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "시험 의뢰 조회 - 일자 / 상태 / 인수여부 필터")
    public List<TbWcsQcTestRequest> list(
            @RequestParam(name = "date",    required = false) String date,
            @RequestParam(name = "status",  required = false) String status,
            @RequestParam(name = "fetched", required = false) Boolean fetched) {
        logger.debug("[ Qctest ][ Request ] list - date={}, status={}, fetched={}", date, status, fetched);

        if (ValueUtil.isNotEmpty(status)) {
            QcTestRequestStatus s = QcTestRequestStatus.from(status);
            if (ValueUtil.isEmpty(s)) {
                throw new ElidomRuntimeException(WcsError.INVALID_PARAMETER.codeAsString(),
                        "지원하지 않는 status: " + status);
            }
            return service.findByStatus(s);
        }
        if (Boolean.FALSE.equals(fetched)) {
            return service.findUnfetched();
        }
        return service.findByDate(parseDate(date));
    }

    @RequestMapping(value = "/today", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "금일 시험 의뢰 - 입고 등록 팝업용")
    public List<TbWcsQcTestRequest> today() {
        return service.findToday();
    }

    @RequestMapping(value = "/lookup", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "자동 채움 단건 조회 - 없으면 빈 응답")
    public Map<String, Object> lookup(
            @RequestParam(name = "date") String date,
            @RequestParam(name = "item_code") String itemCode,
            @RequestParam(name = "lot_no", required = false) String lotNo) {
        TbWcsQcTestRequest found = service.lookup(parseDate(date), itemCode, lotNo);
        Map<String, Object> out = new HashMap<>();
        out.put("exists", ValueUtil.isNotEmpty(found));
        out.put("record", found);
        return out;
    }

    @RequestMapping(value = "/unfetched", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "상위 미인수 의뢰 - LIMS pull (호출 즉시 fetched=true)")
    public List<Map<String, Object>> unfetched() {
        List<TbWcsQcTestRequest> rows = service.findUnfetched();
        List<Map<String, Object>> result = new ArrayList<>(rows.size());
        for (TbWcsQcTestRequest r : rows) {
            if (ValueUtil.isEmpty(r.getReportPdfId())) {
                logger.warn("[ Qctest ][ Request ] unfetched skip - no PDF, id={}, reqNo={}",
                        r.getId(), r.getTestRequestNo());
                continue;
            }
            result.add(buildPullPayload(r));
            service.markFetched(r.getId());
        }
        logger.info("[ Qctest ][ Request ] pull returned={} (auto-acked)", result.size());
        return result;
    }

    /**
     * IF02 발신 payload — 의뢰 row 스냅샷을 명세 tf_ 키로 매핑.
     * 날짜는 "yyyy-MM-dd" 문자열로 명시 변환 (LIMS 계약 안정성).
     */
    private Map<String, Object> buildPullPayload(TbWcsQcTestRequest r) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("test_request_no", r.getTestRequestNo());
        out.put("product_code", r.getItemCode());
        out.put("tf_wf_type", r.getTestWfType());
        out.put("tf_req_desc", r.getTestReqDesc());
        out.put("tf_mfr", r.getManufacturer());
        out.put("tf_lot_id", r.getLotNo());

        LocalDate mfd = LocalDateUtils.toLocalDate(r.getManufacturedDate());
        out.put("tf_mfd_on", mfd == null ? null : mfd.toString());
        out.put("tf_mfr_qty", r.getManufacturedQty());
        out.put("tf_mfr_unit", r.getMfrUnit());

        LocalDate exp = LocalDateUtils.toLocalDate(r.getExpiryDate());
        out.put("tf_exp_on", exp == null ? null : exp.toString());
        out.put("tf_incom_cntr_qty", r.getIncomingQty());
        out.put("tf_req_dept", r.getReqDept());
        out.put("submitter_order", r.getSubmitterOrder());

        // 운영 메타 (명세 외 부가)
        out.put("id", r.getId());
        out.put("item_owner", r.getItemOwner());
        out.put("status", r.getStatus());
        out.put("fetched", Boolean.TRUE);
        out.put("created_at", r.getCreatedAt());

        Map<String, Object> pdf = new LinkedHashMap<>();
        pdf.put("file_id", r.getReportPdfId());
        pdf.put("download_url", "/rest/wcs/file-attachment/" + r.getReportPdfId());
        out.put("pdf", pdf);
        return out;
    }

    @PostMapping(value = "/with-pdf-id",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "신규 시험 의뢰 발행 - PDF file_id 동반 (JSON)")
    public TbWcsQcTestRequest createWithPdfId(@RequestBody Map<String, Object> body) {
        return service.createWithPdfId(
                parseDate((String) body.get("inbound_date")),
                (String) body.get("item_code"),
                (String) body.get("lot_no"),
                (String) body.get("file_id"),
                (String) body.get("test_wf_type"),
                (String) body.get("test_req_desc"),
                parseDateToDate((String) body.get("manufactured_date")),
                parseDateToDate((String) body.get("expiry_date")),
                parseIntOrNull(body.get("manufactured_qty")),
                parseIntOrNull(body.get("incoming_qty")),
                (String) body.get("req_dept"),
                (String) body.get("submitter_order"));
    }

    @PostMapping(value = "/batch-save",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "그리드 일괄 저장 - (입고일자,SKU,LOT) 키로 upsert")
    public Map<String, Object> batchSave(@RequestBody QcRequestBatchSave req) {
        return service.batchSave(req.getEntries());
    }

    @PostMapping(value = "/{id}/pdf-id",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "PDF file_id 갱신 (교체)")
    public TbWcsQcTestRequest replacePdfId(@PathVariable("id") String id,
                                           @RequestBody Map<String, Object> body) {
        String newFileId = (String) body.get("file_id");
        return service.replacePdf(id, newFileId);
    }

    @RequestMapping(value = "/{id}/fetched", method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "수동 인수 표시 (보조 경로)")
    public TbWcsQcTestRequest markFetched(@PathVariable("id") String id) {
        return service.markFetched(id);
    }

    @RequestMapping(value = "/{id}/complete", method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "LIMS 결과 콜백 - test_no 기록 + 상태 COMPLETED")
    public TbWcsQcTestRequest complete(@PathVariable("id") String id,
                                       @RequestBody Map<String, Object> body) {
        String testNo = (String) body.get("test_no");
        return service.complete(id, testNo);
    }
}