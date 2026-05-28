package operato.logis.wcs.rest.inbound;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.service.impl.query.inbound.InboundPlanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

import java.util.List;
import java.util.Map;

/**
 * 입고 예정 REST Controller
 *
 * - GET  /rest/wcs/inbound/plan        : 입고 예정 조회 (query/sort/page/limit)
 * - POST /rest/wcs/inbound/plan/save   : 입고 예정 다건 저장 (QC 대상이면 의뢰 자동 생성)
 * - POST /rest/wcs/inbound/plan/delete : 입고 예정 다건 삭제
 */
@RestController
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/wcs/inbound/plan")
@ServiceDesc(description = "InboundPlan Service API")
public class InboundPlanController {

    private static final Logger logger = LoggerFactory.getLogger(InboundPlanController.class);

    private final InboundPlanService inboundPlanService;

    /** 입고 예정 조회 (페이징) */
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "입고 예정 조회")
    public Map<String, Object> search(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "50") int limit) {

        logger.debug("[ Inbound ][ Plan ] search - page={}, limit={}, query={}, sort={}", page, limit, query, sort);
        return inboundPlanService.search(query, sort, page, limit);
    }

    /** 입고 예정 다건 저장 — body: { rows: [{ planDate, itemCode, lotNo, ownerCode, plannedQty, uom, produceDate, expiryDate, testRequired, niaRequired }] } */
    @RequestMapping(value = "/save", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "입고 예정 저장")
    public Map<String, Object> save(@RequestBody SaveRequest requestData) {
        logger.info("[ Inbound ][ Plan ] save - rows={}", requestData.getRows() != null ? requestData.getRows().size() : 0);
        return inboundPlanService.save(requestData.getRows());
    }

    /** 입고 예정 다건 삭제 — body: { ids: [...] } */
    @RequestMapping(value = "/delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "입고 예정 삭제")
    public Map<String, Object> delete(@RequestBody DeleteRequest requestData) {
        logger.info("[ Inbound ][ Plan ] delete - {}", requestData.getIds());
        return inboundPlanService.deleteByIds(requestData.getIds());
    }

    /** 입고 예정 필수 여부 조회 — 프론트 "추가" 버튼 게이팅용. { required: true|false } */
    @RequestMapping(value = "/required", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "입고 예정 필수 여부 조회")
    public Map<String, Object> required() {
        boolean required = inboundPlanService.isPlanRequired();
        logger.debug("[ Inbound ][ Plan ] plan-required setting - {}", required);
        return Map.of("required", required);
    }

    /** 저장 요청 바디. */
    public static class SaveRequest {
        private List<Map<String, Object>> rows;

        public List<Map<String, Object>> getRows() { return rows; }
        public void setRows(List<Map<String, Object>> rows) { this.rows = rows; }
    }

    /** 삭제 요청 바디. */
    public static class DeleteRequest {
        private List<String> ids;

        public List<String> getIds() { return ids; }
        public void setIds(List<String> ids) { this.ids = ids; }
    }
}
