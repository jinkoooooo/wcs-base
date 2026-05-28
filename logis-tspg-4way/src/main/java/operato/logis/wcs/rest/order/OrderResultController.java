package operato.logis.wcs.rest.order;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.service.impl.query.common.orderResult.OrderResultService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

import java.util.Map;

/**
 * 입출고 실적현황 REST Controller.
 *
 * GET /rest/wcs/order-result                : Master — 주문 목록 (order_type 필터는 query로)
 * GET /rest/wcs/order-result/shuttle-orders : Detail — host_order의 shuttle_order 상세
 */
@RestController
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/wcs/order-result")
@ServiceDesc(description = "OrderResult Service API")
public class OrderResultController {

    private final OrderResultService orderResultService;

    /** Master — 입출고 주문 목록 조회 (페이징). */
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "입출고 실적현황 조회 (페이징)")
    public Map<String, Object> search(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "50") int limit) {

        return orderResultService.search(query, sort, page, limit);
    }

    /** Detail — host_order_key에 연결된 shuttle_order 상세. */
    @RequestMapping(value = "/shuttle-orders", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "ShuttleOrder 상세 조회")
    public Map<String, Object> searchShuttleOrders(
            @RequestParam(name = "host_order_key") String hostOrderKey) {

        return orderResultService.searchShuttleOrders(hostOrderKey);
    }
}