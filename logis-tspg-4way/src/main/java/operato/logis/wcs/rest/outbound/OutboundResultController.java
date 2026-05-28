package operato.logis.wcs.rest.outbound;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.service.impl.query.outbound.OutboundResultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

import java.util.Map;

/**
 * 출고실적현황 REST Controller
 *
 * - GET /rest/wcs/outbound/result                  : Master — 출고실적 목록 조회
 * - GET /rest/wcs/outbound/result/shuttle-orders   : Detail — 특정 Host Order의 ShuttleOrder 상세
 */
@RestController
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/wcs/outbound/result")
@ServiceDesc(description = "OutboundResult Service API")
public class OutboundResultController {

    private static final Logger logger = LoggerFactory.getLogger(OutboundResultController.class);

    private final OutboundResultService outboundResultService;

    /**
     * Master — 출고실적 목록 조회 (페이징)
     */
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "출고실적현황 조회 (페이징)")
    public Map<String, Object> search(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "50") int limit) {

        logger.debug("[ Outbound ][ Result ] search - page={}, limit={}, query={}, sort={}", page, limit, query, sort);
        return outboundResultService.search(query, sort, page, limit);
    }

    /**
     * Detail — 특정 Host Order에 연결된 ShuttleOrder + Item 상세 조회
     */
    @RequestMapping(value = "/shuttle-orders", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "출고 ShuttleOrder 상세 조회")
    public Map<String, Object> searchShuttleOrders(
            @RequestParam(name = "host_order_key") String hostOrderKey) {

        logger.debug("[ Outbound ][ Result ] shuttle orders - hostOrderKey={}", hostOrderKey);
        return outboundResultService.searchShuttleOrders(hostOrderKey);
    }
}
