package operato.logis.wcs.rest.qctest;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.dto.QcTestResultRequest;
import operato.logis.wcs.entity.TbWcsHostOrder;
import operato.logis.wcs.service.impl.qctest.QcResultService;
import operato.logis.wcs.service.impl.qctest.QcTestTargetQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

import java.util.List;
import java.util.Map;

/**
 * 시험 결과 입력 / 재입고 지시 REST Controller (INBOUND_TEST_WAIT 상태 host_order 대상).
 *
 * - GET  /rest/wcs/qc-test/target                          : 시험 미종결 host_order 목록
 * - POST /rest/wcs/qc-test/target/{hostOrderKey}/result    : 시험 결과 입력 (PASSED/FAILED)
 * - POST /rest/wcs/qc-test/target/{hostOrderKey}/reinbound : 재입고 지시 (사전 발행 INBOUND shuttle 을 SENDING 으로)
 */
@RestController
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/wcs/qc-test/target")
@ServiceDesc(description = "QcTestTarget Service API")
public class QcTestTargetController {

    private static final Logger logger = LoggerFactory.getLogger(QcTestTargetController.class);

    private final QcTestTargetQueryService qcTestTargetService;
    private final QcResultService testService;

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "시험 미종결 host_order 목록")
    public List<Map<String, Object>> list(
            @RequestParam(name = "eqGroupId", required = false) String eqGroupId) {
        logger.debug("[ Qctest ][ Target ] list - eqGroupId={}", eqGroupId);
        return qcTestTargetService.listInspectionTargets(eqGroupId);
    }

    @RequestMapping(value = "/{hostOrderKey}/result", method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "시험 결과 입력 (PASSED/FAILED)")
    public ResponseEntity<TbWcsHostOrder> applyResult(@PathVariable String hostOrderKey,
                                                       @RequestBody QcTestResultRequest req) {
        logger.info("[ Qctest ][ Target ] result - hostOrderKey={}, result={}",
                hostOrderKey, req == null ? null : req.getResult());
        return ResponseEntity.ok(testService.applyResult(hostOrderKey, req));
    }

    @RequestMapping(value = "/{hostOrderKey}/reinbound", method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "재입고 지시 - 사전 발행 INBOUND shuttle 을 SENDING 으로 전환")
    public Map<String, Object> triggerReinbound(@PathVariable String hostOrderKey) {
        logger.info("[ Qctest ][ Target ] reinbound - hostOrderKey={}", hostOrderKey);
        return qcTestTargetService.triggerReinbound(hostOrderKey);
    }
}
