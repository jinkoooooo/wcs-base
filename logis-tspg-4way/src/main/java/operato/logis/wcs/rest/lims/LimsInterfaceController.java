package operato.logis.wcs.rest.lims;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.QcTestStatus;
import operato.logis.wcs.dto.QcTestResultRequest;
import operato.logis.wcs.dto.lims.*;
import operato.logis.wcs.service.impl.qctest.QcResultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;

/**
 * LIMS → WES 인바운드 인터페이스 통합 컨트롤러.
 *
 * LIMS 가 우리를 호출하는 모든 REST 를 한 곳에 모은다. (우리가 서버, LIMS 가 클라이언트)
 *   - IF01 자재마스터 동기화 : POST /rest/wcs/lims/if01/item-master
 *   - IF02 시험의뢰 (pull)   : POST /rest/wcs/lims/if02/test-request
 *   - IF03 판정결과 콜백      : POST /rest/wcs/lims/if03/test-result
 *
 * 비즈니스 로직은 기존 서비스에 위임. 본 컨트롤러는 매핑 + 응답 포맷팅만.
 * 공통 응답: { status, request_id, message, data }
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/rest/wcs/lims")
public class LimsInterfaceController {

    private static final Logger logger = LoggerFactory.getLogger(LimsInterfaceController.class);

    private final QcResultService qcResultService;
    // private final ItemMasterSyncService itemMasterSyncService;       // IF01 — 실제 서비스명으로 교체
    // private final QcTestRequestService qcTestRequestService;         // IF02 — pull 회신용

    /**
     * IF01 — 자재마스터 동기화. data.items[] 의 type(insert/update/delete) 별로 반영.
     */
    @PostMapping(value = "/if01/item-master",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public LimsResponse<Void> syncItemMaster(@RequestBody If01Request req) {
        logger.info("[ LIMS ][ IF01 ] item-master sync - requestId={}, items={}",
                req == null ? null : req.getRequestId(),
                (req == null || req.getData() == null || req.getData().getItems() == null)
                        ? 0 : req.getData().getItems().size());
        try {
            // TODO: itemMasterSyncService.sync(req.getData().getItems());
            return LimsResponse.success(req.getRequestId(), null);
        } catch (Exception e) {
            logger.error("[ LIMS ][ IF01 ] sync failed - requestId={}", req == null ? null : req.getRequestId(), e);
            return LimsResponse.fail(req == null ? null : req.getRequestId(), e.getMessage());
        }
    }

    /**
     * IF02 — 시험의뢰 pull. LIMS 요청 시 WES 가 미인수 의뢰 목록을 data.requests[] 로 회신.
     */
    @PostMapping(value = "/if02/test-request",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public LimsResponse<If02ResponseData> pullTestRequests(@RequestBody LimsBaseRequest req) {
        logger.info("[ LIMS ][ IF02 ] test-request pull - requestId={}", req == null ? null : req.getRequestId());
        try {
            // TODO: List<...> rows = qcTestRequestService.findUnfetched(); → If02RequestItem 으로 매핑 + markFetched
            If02ResponseData data = new If02ResponseData(new ArrayList<>());
            return LimsResponse.success(req == null ? null : req.getRequestId(), data);
        } catch (Exception e) {
            logger.error("[ LIMS ][ IF02 ] pull failed - requestId={}", req == null ? null : req.getRequestId(), e);
            return LimsResponse.fail(req == null ? null : req.getRequestId(), e.getMessage());
        }
    }

    /**
     * IF03 — 판정결과 콜백. results[] 의 (test_request_no, id_text, tf_decision) 을
     * 기존 QcTestService.applyResult 로 위임. id_text → test_no, tf_decision → PASSED/FAILED 매핑.
     */
    @PostMapping(value = "/if03/test-result",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public LimsResponse<Void> applyTestResult(@RequestBody If03Request req) {
        logger.info("[ LIMS ][ IF03 ] test-result - requestId={}, results={}",
                req == null ? null : req.getRequestId(),
                (req == null || req.getResults() == null) ? 0 : req.getResults().size());
        try {
            if (req == null || ValueUtil.isEmpty(req.getResults())) {
                return LimsResponse.fail(req == null ? null : req.getRequestId(), "results is empty");
            }
            for (If03Result r : req.getResults()) {
                QcTestStatus mapped = mapDecision(r.getTfDecision());
                if (mapped == null) {
                    // C(취소) 등 — 정책 미정. 우선 skip + 로그.
                    logger.warn("[ LIMS ][ IF03 ] skip non-PF decision - reqNo={}, decision={}",
                            r.getTestRequestNo(), r.getTfDecision());
                    continue;
                }
                QcTestResultRequest dto = new QcTestResultRequest();
                dto.setResult(mapped.code());
                dto.setTestNo(r.getIdText());        // LIMS 시험번호 = 우리 test_no
                // dto.setTesterId(...); dto.setReason(...);  // IF03 에 해당 필드 있으면 매핑
                qcResultService.applyResult(r.getTestRequestNo(), dto);
            }
            return LimsResponse.success(req.getRequestId(), null);
        } catch (Exception e) {
            logger.error("[ LIMS ][ IF03 ] apply failed - requestId={}", req == null ? null : req.getRequestId(), e);
            return LimsResponse.fail(req == null ? null : req.getRequestId(), e.getMessage());
        }
    }

    // tf_decision(P/F/C) → QcTestStatus. C 등은 null 반환(미처리).
    private QcTestStatus mapDecision(String tfDecision) {
        if ("P".equalsIgnoreCase(tfDecision)) return QcTestStatus.PASSED;
        if ("F".equalsIgnoreCase(tfDecision)) return QcTestStatus.FAILED;
        return null;
    }
}