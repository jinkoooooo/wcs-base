package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.rest;

import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.HostOrderReceiveRequest;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.HostOrderReceiveResponse;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.facade.Tspg4WayShuttleWcsFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ====================================================================
 * HOST 주문 수신 REST Controller
 * ====================================================================
 *
 * [역할]
 * HOST 시스템(WMS, ERP 등)으로부터 주문 요청을 수신하는 REST API 엔드포인트
 *
 * [API 엔드포인트]
 * - POST /api/wcs/tspg4way/host-order/receive : 주문 수신
 * - GET  /api/wcs/tspg4way/host-order/status/{hostSystemCode}/{hostOrderKey} : 상태 조회
 *
 * [요청/응답 흐름]
 * HOST System → HostOrderController → Tspg4WayShuttleWcsFacade → Response
 *
 * [응답 코드]
 * - 200 OK: 주문 수신 성공 또는 중복 주문 (멱등성)
 * - 400 Bad Request: 검증 실패 (필수 필드 누락, 잘못된 주문 유형 등)
 * - 500 Internal Server Error: 내부 오류
 *
 * @author WCS Development Team
 * @since 2026-03-04
 */
@RestController
@RequestMapping("/rest/wcs/tspg4way/host-order")
public class HostOrderController {

    private static final Logger logger = LoggerFactory.getLogger(HostOrderController.class);

    @Autowired
    private Tspg4WayShuttleWcsFacade wcsFacade;

    /**
     * HOST 주문 수신
     * POST /api/wcs/tspg4way/host-order/receive
     */
    @PostMapping("/receive")
    public ResponseEntity<HostOrderReceiveResponse> receiveOrder(@RequestBody HostOrderReceiveRequest request) {
        logger.info("Received host order request: hostSystemCode={}, hostOrderKey={}, orderType={}",
                request.getHostSystemCode(), request.getHostOrderKey(), request.getOrderType());

        try {
            HostOrderReceiveResponse response = wcsFacade.receiveHostOrder(request);

            if (response.isSuccess()) {
                logger.info("Host order processed successfully: wcsOrderKey={}", response.getWcsOrderKey());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Host order processing failed: errorCode={}, errorDesc={}",
                        response.getErrorCode(), response.getErrorDesc());
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            logger.error("Unexpected error processing host order", e);
            HostOrderReceiveResponse errorResponse = HostOrderReceiveResponse.fail(
                    request.getHostOrderKey(),
                    "ERR_INTERNAL",
                    "Internal server error: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 주문 상태 조회
     * GET /api/wcs/tspg4way/host-order/status/{hostSystemCode}/{hostOrderKey}
     */
    @GetMapping("/status/{hostSystemCode}/{hostOrderKey}")
    public ResponseEntity<HostOrderReceiveResponse> getOrderStatus(
            @PathVariable String hostSystemCode,
            @PathVariable String hostOrderKey) {
        logger.info("Querying order status: hostSystemCode={}, hostOrderKey={}", hostSystemCode, hostOrderKey);

        // TODO: 상태 조회 로직 구현
        return ResponseEntity.ok(HostOrderReceiveResponse.success(null, "Status query not implemented"));
    }
}
