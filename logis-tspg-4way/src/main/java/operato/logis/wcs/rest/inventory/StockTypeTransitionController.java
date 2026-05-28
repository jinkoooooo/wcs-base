package operato.logis.wcs.rest.inventory;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.common.validation.CommentValidator;
import operato.logis.wcs.dto.StockTypeTransitionRequest;
import operato.logis.wcs.service.impl.query.inventory.StockTypeTransitionService;
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
 * 재고 카테고리(stock_type) 전이 REST.
 *
 * 운영자 권한 전용 ({@code /rest/admin/wcs/...} prefix, WcsAdminController 와 동일).
 * comment 필수. testRequestNo 는 to=NIA_PENDING 일 때만 의미.
 */
@RestController
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/admin/wcs/stock")
@ServiceDesc(description = "Stock Type Transition API (admin)")
public class StockTypeTransitionController {

    private static final Logger logger = LoggerFactory.getLogger(StockTypeTransitionController.class);

    private final StockTypeTransitionService service;

    /**
     * stock_type 전이.
     * body.to: DISPOSAL | RETURN | NORMAL | NIA_PENDING | NIA_FAIL | QC_PENDING | QC_FAIL
     * body.comment 필수(2~500자), body.testRequestNo 선택(to=NIA_PENDING 일 때).
     */
    @PostMapping(value = "/{stockId}/transition",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "재고 카테고리 전이")
    public ResponseEntity<Map<String, Object>> transition(@PathVariable String stockId,
                                                          @RequestBody StockTypeTransitionRequest req) {
        if (ValueUtil.isEmpty(req)) {
            throw new ElidomRuntimeException("요청 파라미터가 비어있습니다");
        }
        CommentValidator.requireValid("comment", req.getComment());
        logger.warn("[ Inventory ][ State ] transition - stockId={}, to={}, testRequestNo={}",
                stockId, req.getTo(), req.getTestRequestNo());
        try {
            return ResponseEntity.ok(service.transition(stockId, req.getTo(),
                    req.getComment(), req.getTestRequestNo()));
        } catch (Exception e) {
            logger.error("[ Inventory ][ State ] failed - stockId={}, to={}", stockId, req.getTo(), e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}
