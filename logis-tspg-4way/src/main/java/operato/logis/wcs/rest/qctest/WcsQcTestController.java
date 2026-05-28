package operato.logis.wcs.rest.qctest;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.common.validation.CommentValidator;
import operato.logis.wcs.service.repository.HostOrderRepository;
import operato.logis.wcs.consts.WcsError;
import operato.logis.wcs.dto.QcTestResultRequest;
import operato.logis.wcs.entity.TbWcsHostOrder;
import operato.logis.wcs.service.impl.qctest.QcCorrectionService;
import operato.logis.wcs.service.impl.qctest.QcResultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.util.ValueUtil;

import java.util.Map;

/**
 * QC 시험(QcTest) REST 컨트롤러.
 *
 * 한 곳에서 QC 시험의 송수신 양방향을 다룬다:
 *   1) 의뢰(request) : WCS → 외부 LIMS 송신 트리거 (수동 재의뢰 경로)
 *   2) 결과(result)  : 외부 LIMS → WCS 콜백 수신
 *
 * 키 단위:
 *   - 의뢰 : hostOrderKey (주문 1개의 시험 대상 item 일괄 의뢰)
 *   - 결과 : testReqNo (시험 의뢰번호 — 한 LIMS 의뢰가 여러 item 에 걸칠 수 있음)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/rest/wcs/qc-test")
public class WcsQcTestController {

    private static final Logger logger = LoggerFactory.getLogger(WcsQcTestController.class);

    private final QcResultService qcResultService;
    private final QcCorrectionService qcCorrectionService;
    private final HostOrderRepository hostOrderRepository;

    // 시험 의뢰 — WCS → 외부 LIMS. 자동 호출은 HostOrderCreator 가 담당.
    // 외부 송신 실패 등으로 수동 재의뢰가 필요할 때 이 엔드포인트 사용.
    // 시험 대상 0건이면 자동 PASSED → READY_FOR_ALLOC.
    @PostMapping("/request/{hostOrderKey}")
    public ResponseEntity<TbWcsHostOrder> requestTest(@PathVariable String hostOrderKey) {
        logger.info("[ Qctest ] request - hostOrderKey={}", hostOrderKey);

        TbWcsHostOrder h = hostOrderRepository.findByHostOrderKey(hostOrderKey);
        if (ValueUtil.isEmpty(h)) {
            throw new ElidomRuntimeException(WcsError.ORDER_NOT_FOUND.codeAsString(),
                    "host_order 없음: " + hostOrderKey);
        }
        qcResultService.requestTest(h);
        return ResponseEntity.ok(h);
    }

    // 시험 결과 수신 — 외부 LIMS → WCS. testReqNo 단위 결과(PASSED/FAILED) 일괄 적용 후 host 별 헤더 재집계.
    @PostMapping("/result/{testReqNo}")
    public ResponseEntity<TbWcsHostOrder> applyResult(@PathVariable String testReqNo,
                                                      @RequestBody QcTestResultRequest req) {
        logger.info("[ Qctest ] result - testReqNo={}, result={}, testerId={}",
                testReqNo,
                req == null ? null : req.getResult(),
                req == null ? null : req.getTesterId());
        return ResponseEntity.ok(qcResultService.applyResult(testReqNo, req));
    }

    // QC 누락(오기입) 보정 — NORMAL 재고를 QC 대기로(재고+item+의뢰마스터+주문 재오픈 일괄).
    // body: { eqGroupId, comment } (manual-outbound 와 동일 전달 방식)
    @PostMapping("/correction/stocks/{stockId}/to-qc-pending")
    public ResponseEntity<Map<String, Object>> correctToQcPending(
            @PathVariable String stockId,
            @RequestBody(required = false) Map<String, Object> body) {

        // eqGroupId·comment 추출 + comment 필수 검증 (admin 보정 사유)
        String eqGroupId = (body == null) ? null : (String) body.get("eqGroupId");
        String comment = (body == null) ? null : (String) body.get("comment");
        CommentValidator.requireValid("comment", comment);

        logger.warn("[ Qctest ][ Correction ] request - stockId={}, eqGroupId={}", stockId, eqGroupId);
        return ResponseEntity.ok(qcCorrectionService.correctToQcPending(eqGroupId, stockId, comment));
    }
}