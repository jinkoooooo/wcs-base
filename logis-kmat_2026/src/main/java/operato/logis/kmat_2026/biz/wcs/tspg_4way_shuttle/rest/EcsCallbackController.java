package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.rest;

import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.EcsCallbackResponse;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.InternalEcsCallbackRequest;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.service.InternalEcsCallbackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ====================================================================
 * Internal ECS Callback REST Controller
 * ====================================================================
 *
 * [역할]
 * - 같은 시스템 내부에서 사용하던 InternalEcsCallbackService 를
 *   REST API 형태로 노출하여 테스트/수동 시뮬레이션 가능하게 한다.
 *
 * [API 엔드포인트]
 * - POST /rest/wcs/tspg4way/internal-ecs-callback/started
 * - POST /rest/wcs/tspg4way/internal-ecs-callback/in-progress
 * - POST /rest/wcs/tspg4way/internal-ecs-callback/complete
 * - POST /rest/wcs/tspg4way/internal-ecs-callback/error
 * - POST /rest/wcs/tspg4way/internal-ecs-callback/cancelled
 *
 * [요청 예시]
 * {
 *   "orderKey": "MOV-20260313212803-5f"
 * }
 *
 * [error 요청 예시]
 * {
 *   "orderKey": "MOV-20260313212803-5f",
 *   "errorCode": "ECS_ERR",
 *   "message": "equipment failure"
 * }
 */
@RestController
@RequestMapping("/rest/wcs/tspg4way/ecs-callback")
public class EcsCallbackController {

    private static final Logger logger = LoggerFactory.getLogger(EcsCallbackController.class);

    @Autowired
    private InternalEcsCallbackService internalEcsCallbackService;

    /**
     * 작업 시작 콜백
     * POST /rest/wcs/tspg4way/internal-ecs-callback/started
     */
    @PostMapping("/started")
    public ResponseEntity<EcsCallbackResponse> started(@RequestBody InternalEcsCallbackRequest request) {
        logger.info("Received internal ECS started callback request: orderKey={}", request.getOrderKey());

        try {
            validateOrderKey(request.getOrderKey());

            EcsCallbackResponse response = internalEcsCallbackService.started(request.getOrderKey());
            logger.info("Internal ECS started callback processed: orderKey={}", request.getOrderKey());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid internal ECS started callback request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(failResponse(request.getOrderKey(), e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error processing internal ECS started callback", e);
            return ResponseEntity.internalServerError()
                    .body(failResponse(request.getOrderKey(), "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * 작업 진행중 콜백
     * POST /rest/wcs/tspg4way/internal-ecs-callback/in-progress
     */
    @PostMapping("/in-progress")
    public ResponseEntity<EcsCallbackResponse> inProgress(@RequestBody InternalEcsCallbackRequest request) {
        logger.info("Received internal ECS in-progress callback request: orderKey={}", request.getOrderKey());

        try {
            validateOrderKey(request.getOrderKey());

            EcsCallbackResponse response = internalEcsCallbackService.inProgress(request.getOrderKey());
            logger.info("Internal ECS in-progress callback processed: orderKey={}", request.getOrderKey());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid internal ECS in-progress callback request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(failResponse(request.getOrderKey(), e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error processing internal ECS in-progress callback", e);
            return ResponseEntity.internalServerError()
                    .body(failResponse(request.getOrderKey(), "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * 작업 완료 콜백
     * POST /rest/wcs/tspg4way/internal-ecs-callback/complete
     */
    @PostMapping("/complete")
    public ResponseEntity<EcsCallbackResponse> complete(@RequestBody InternalEcsCallbackRequest request) {
        logger.info("Received internal ECS complete callback request: orderKey={}", request.getOrderKey());

        try {
            validateOrderKey(request.getOrderKey());

            EcsCallbackResponse response = internalEcsCallbackService.complete(request.getOrderKey());
            logger.info("Internal ECS complete callback processed: orderKey={}", request.getOrderKey());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid internal ECS complete callback request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(failResponse(request.getOrderKey(), e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error processing internal ECS complete callback", e);
            return ResponseEntity.internalServerError()
                    .body(failResponse(request.getOrderKey(), "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * 작업 실패 콜백
     * POST /rest/wcs/tspg4way/internal-ecs-callback/error
     */
    @PostMapping("/error")
    public ResponseEntity<EcsCallbackResponse> error(@RequestBody InternalEcsCallbackRequest request) {
        logger.info("Received internal ECS error callback request: orderKey={}, errorCode={}, message={}",
                request.getOrderKey(), request.getErrorCode(), request.getMessage());

        try {
            validateOrderKey(request.getOrderKey());

            EcsCallbackResponse response = internalEcsCallbackService.error(
                    request.getOrderKey(),
                    request.getErrorCode(),
                    request.getMessage()
            );
            logger.info("Internal ECS error callback processed: orderKey={}", request.getOrderKey());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid internal ECS error callback request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(failResponse(request.getOrderKey(), e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error processing internal ECS error callback", e);
            return ResponseEntity.internalServerError()
                    .body(failResponse(request.getOrderKey(), "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * 작업 취소 콜백
     * POST /rest/wcs/tspg4way/internal-ecs-callback/cancelled
     */
    @PostMapping("/cancelled")
    public ResponseEntity<EcsCallbackResponse> cancelled(@RequestBody InternalEcsCallbackRequest request) {
        logger.info("Received internal ECS cancelled callback request: orderKey={}", request.getOrderKey());

        try {
            validateOrderKey(request.getOrderKey());

            EcsCallbackResponse response = internalEcsCallbackService.cancelled(request.getOrderKey());
            logger.info("Internal ECS cancelled callback processed: orderKey={}", request.getOrderKey());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid internal ECS cancelled callback request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(failResponse(request.getOrderKey(), e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error processing internal ECS cancelled callback", e);
            return ResponseEntity.internalServerError()
                    .body(failResponse(request.getOrderKey(), "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * 렉단 컨베이어 도착 완료 콜백
     * POST /rest/wcs/tspg4way/ecs-callback/conveyor-arrived
     *
     * ECS에서 렉단 컨베이어에 파렛트가 도착 완료되면 호출
     * → 입고 명령 전송 + AGF 입고 리필 트리거
     */
    @PostMapping("/conveyor-arrived")
    public ResponseEntity<EcsCallbackResponse> conveyorArrived(@RequestBody InternalEcsCallbackRequest request) {
        logger.info("Received ECS conveyor-arrived callback: orderKey={}", request.getOrderKey());

        try {
            validateOrderKey(request.getOrderKey());

            EcsCallbackResponse response = internalEcsCallbackService.conveyorArrived(request.getOrderKey());
            logger.info("ECS conveyor-arrived callback processed: orderKey={}", request.getOrderKey());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid ECS conveyor-arrived callback request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(failResponse(request.getOrderKey(), e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error processing ECS conveyor-arrived callback", e);
            return ResponseEntity.internalServerError()
                    .body(failResponse(request.getOrderKey(), "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * orderKey 필수 검증
     */
    private void validateOrderKey(String orderKey) {
        if (orderKey == null || orderKey.trim().isEmpty()) {
            throw new IllegalArgumentException("orderKey is required");
        }
    }

    /**
     * 에러 응답 생성
     *
     * ※ EcsCallbackResponse 의 실제 생성 메서드/빌더에 맞게 조정 필요
     */
    private EcsCallbackResponse failResponse(String orderKey, String message) {
        return EcsCallbackResponse.builder()
                .orderKey(orderKey)
                .success(false)
                .message(message)
                .build();
    }
}