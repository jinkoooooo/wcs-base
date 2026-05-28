package operato.logis.wcs.rest.inventory;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.common.validation.CommentValidator;
import operato.logis.wcs.dto.ManualOutboundRequest;
import operato.logis.wcs.service.impl.order.issuer.ManualOutboundIssuer;
import operato.logis.wcs.service.impl.order.issuer.OperatorSampleOutboundIssuer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.util.ValueUtil;

import java.util.Map;

/**
 * Dashboard2D 셀 액션 - 단일 stock 기준 수동 출고 지시 REST.
 *
 * 운영자 권한 전용 ({@code /rest/admin/wcs/...} prefix).
 * 작업 생성은 {@link ManualOutboundIssuer} 로 위임. comment 필수.
 */
@RestController
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/admin/wcs/stock")
@ServiceDesc(description = "Manual Outbound API (admin)")
public class ManualOutboundController {

    private static final Logger logger = LoggerFactory.getLogger(ManualOutboundController.class);

    private final ManualOutboundIssuer manualOutboundIssuer;
    private final OperatorSampleOutboundIssuer operatorSampleOutboundIssuer;

    /**
     * 단일 stock 수동 출고.
     * body: { eqGroupId, portCode(선택, 비우면 자동 배정), comment(필수 2~500자) }
     */
    @PostMapping(value = "/{stockId}/manual-outbound",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "단일 stock 수동 출고")
    public ResponseEntity<Map<String, Object>> issue(@PathVariable String stockId,
                                                     @RequestBody ManualOutboundRequest req) {
        if (ValueUtil.isEmpty(req)) {
            throw new ElidomRuntimeException("요청 파라미터가 비어있습니다");
        }
        CommentValidator.requireValid("comment", req.getComment());
        logger.warn("[ Outbound ][ Manual ] issue - stockId={}, eqGroupId={}, portCode={}",
                stockId, req.getEqGroupId(), req.getPortCode());
        try {
            return ResponseEntity.ok(manualOutboundIssuer.issueManualOutbound(
                    req.getEqGroupId(), stockId, req.getPortCode(), req.getComment()));
        } catch (Exception e) {
            logger.error("[ Outbound ][ Manual ] failed - stockId={}", stockId, e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * 운영자 샘플 출고 (SAMPLE_OUT).
     *
     * 수량을 사후 확정하는 출고. 채취 후 남은 양은 자동 재입고.
     * 포트 lock 없음. follow_up_since 는 shuttle COMPLETED 시점에 자동 set.
     * body: { eqGroupId, portCode(선택, 비우면 자동 배정), comment(필수 2~500자) }
     */
    @PostMapping(value = "/{stockId}/sample-outbound",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "단일 stock 샘플 출고 (채취 / 시험 등 수량 사후 확정 출고)")
    public ResponseEntity<Map<String, Object>> issueSample(@PathVariable String stockId,
                                                           @RequestBody ManualOutboundRequest req) {
        if (ValueUtil.isEmpty(req)) {
            throw new ElidomRuntimeException("요청 파라미터가 비어있습니다");
        }
        CommentValidator.requireValid("comment", req.getComment());
        logger.warn("[ Outbound ][ Sample ] issue - stockId={}, eqGroupId={}, portCode={}",
                stockId, req.getEqGroupId(), req.getPortCode());
        try {
            return ResponseEntity.ok(operatorSampleOutboundIssuer.issue(
                    req.getEqGroupId(), stockId, req.getPortCode(), req.getComment()));
        } catch (Exception e) {
            logger.error("[ Outbound ][ Sample ] failed - stockId={}", stockId, e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}
