package operato.logis.wcs.rest.outbound;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.service.impl.query.outbound.SelectOutboundService;
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
 * 선택출고 REST Controller
 *
 * - GET  /rest/wcs/outbound/select            : 재고현황 조회 (페이징)
 * - POST /rest/wcs/outbound/select/issue      : 선택 재고 출고 지시
 * - GET  /rest/wcs/outbound/select/ports      : 출고대(포트) 목록 — 화면 드롭다운용
 */
@RestController
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/wcs/outbound/select")
@ServiceDesc(description = "SelectOutbound Service API")
public class SelectOutboundController {

    private static final Logger logger = LoggerFactory.getLogger(SelectOutboundController.class);

    private final SelectOutboundService selectOutboundService;

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "선택출고 재고현황 조회 (페이징)")
    public Map<String, Object> search(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "50") int limit,
            @RequestParam(name = "stockType", defaultValue = "NORMAL") String stockType) {

        logger.debug("[ Outbound ][ Select ] search - page={}, limit={}, stockType={}, query={}, sort={}",
                page, limit, stockType, query, sort);
        return selectOutboundService.search(query, sort, page, limit, stockType);
    }

    /**
     * 선택 재고 출고 지시
     * body: { eqGroupId, portCode, stocks: [{ stockId, itemCode, lotNo, qty, ownerCode }] }
     */
    @RequestMapping(value = "/issue", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "선택 재고 출고 지시")
    public Map<String, Object> issue(@RequestBody Map<String, Object> requestData) {
        logger.info("[ Outbound ][ Select ] issue - {}", requestData);
        return selectOutboundService.issueOutbound(requestData);
    }

    /**
     * 출고대(포트) 목록 — 화면 드롭다운 용
     */
    @RequestMapping(value = "/ports", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "출고대(포트) 목록 조회")
    public List<Map<String, Object>> ports(
            @RequestParam(name = "eqGroupId", required = false) String eqGroupId) {
        return selectOutboundService.listOutboundPorts(eqGroupId);
    }
}
