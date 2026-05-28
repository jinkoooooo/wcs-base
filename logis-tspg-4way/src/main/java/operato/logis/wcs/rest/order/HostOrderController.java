package operato.logis.wcs.rest.order;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.dto.HostOrderApi;
import operato.logis.wcs.entity.TbWcsHostOrder;
import operato.logis.wcs.facade.Tspg4WayShuttleWcsFacade;
import operato.logis.wcs.service.impl.order.host.HostOrderCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * HOST 주문 인테이크 REST (수신/생성/상태).
 *
 * 외부 수신은 Tspg4WayShuttleWcsFacade, WCS 내부 생성은 HostOrderCreator 로 위임.
 * 응답: 200 수신 성공/중복(멱등), 400 검증 실패, 429 처리율 초과, 500 내부 오류.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/rest/wcs/host-order")
public class HostOrderController {

    private static final Logger logger = LoggerFactory.getLogger(HostOrderController.class);

    private final Tspg4WayShuttleWcsFacade wcsFacade;
    private final HostOrderCreator hostOrderCreator;

    // HOST 주문 수신 - 성공/중복은 200, 검증 실패는 400, 내부 오류는 500.
    @PostMapping("/receive")
    public ResponseEntity<HostOrderApi.Response> receiveOrder(@RequestBody HostOrderApi.Request request) {
        logger.info("[ Order ][ Host ] receive - hostSystemCode={}, hostOrderKey={}, orderType={}",
                request.getHostSystemCode(), request.getHostOrderKey(), request.getOrderType());

        try {
            HostOrderApi.Response response = wcsFacade.receiveHostOrder(request);

            // 성공이면 200, 비즈니스 거부면 400
            if (response.isSuccess()) {
                logger.info("[ Order ][ Host ] received - wcsOrderKey={}", response.getWcsOrderKey());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("[ Order ][ Host ] rejected - errorCode={}, errorDesc={}",
                        response.getErrorCode(), response.getErrorDesc());
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            logger.error("[ Order ][ Host ] receive failed - hostOrderKey={}", request.getHostOrderKey(), e);
            HostOrderApi.Response errorResponse = HostOrderApi.Response.fail(
                    request.getHostOrderKey(),
                    "ERR_INTERNAL",
                    "Internal server error: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // WCS 내부(UI/배치) 주문 생성 - 외부 수신과 별개 경로. 산출 진입은 스케줄러가 처리.
    @PostMapping("/create")
    public ResponseEntity<TbWcsHostOrder> create(@RequestBody HostOrderApi.Request req) {
        logger.info("[ Order ][ Host ] create - hostOrderKey={}, type={}, eqGroupId={}",
                req.getHostOrderKey(), req.getOrderType(), req.getEqGroupId());
        TbWcsHostOrder saved = hostOrderCreator.create(req);
        return ResponseEntity.ok(saved);
    }

    // 주문 상태 조회 - 설계 미확정으로 현재는 placeholder 응답.
    @GetMapping("/status/{hostSystemCode}/{hostOrderKey}")
    public ResponseEntity<HostOrderApi.Response> getOrderStatus(
            @PathVariable String hostSystemCode,
            @PathVariable String hostOrderKey) {
        logger.debug("[ Order ][ Host ] status query - hostSystemCode={}, hostOrderKey={}", hostSystemCode, hostOrderKey);
        return ResponseEntity.ok(HostOrderApi.Response.success(null, "Status query not implemented"));
    }
}
