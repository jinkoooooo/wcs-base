package operato.logis.wcs.rest.ecs;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import operato.logis.wcs.dto.EcsCallbackApi;
import operato.logis.wcs.service.impl.ecs.EcsCommandSender;
import operato.logis.wcs.service.impl.ecs.InternalEcsCallbackHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.elidom.util.ValueUtil;

/**
 * Internal ECS Callback REST.
 * InternalEcsCallbackHandler 를 REST 로 노출 (테스트/수동 시뮬레이션).
 * Endpoints: started / in-progress / complete / error / cancelled / conveyor-arrived / inbound-bcr-scan.
 * Request body: { "orderKey": "MOV-..." }. error 추가: errorCode, message.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/rest/wcs/ecs-callback")
public class EcsCallbackController {

    private static final Logger logger = LoggerFactory.getLogger(EcsCallbackController.class);

    private final InternalEcsCallbackHandler internalEcsCallbackService;

    private final EcsCommandSender ecsCommandService;

    // 작업 시작 콜백
    @PostMapping("/started")
    public ResponseEntity<EcsCallbackApi.Response> started(@RequestBody EcsCallbackApi.InternalRequest request) {
        logger.info("[ Ecs ][ Callback ] started start - orderKey={}", request.getOrderKey());
        try {
            validateOrderKey(request.getOrderKey());
            EcsCallbackApi.Response response = internalEcsCallbackService.started(request.getOrderKey());
            logger.info("[ Ecs ][ Callback ] started completed - orderKey={}", request.getOrderKey());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("[ Ecs ][ Callback ] started rejected - orderKey={}, reason={}", request.getOrderKey(), e.getMessage());
            return ResponseEntity.badRequest().body(failResponse(request.getOrderKey(), e.getMessage()));
        } catch (Exception e) {
            logger.error("[ Ecs ][ Callback ] started failed - orderKey={}", request.getOrderKey(), e);
            return ResponseEntity.internalServerError()
                    .body(failResponse(request.getOrderKey(), "Internal server error: " + e.getMessage()));
        }
    }

    // 작업 진행중 콜백
    @PostMapping("/in-progress")
    public ResponseEntity<EcsCallbackApi.Response> inProgress(@RequestBody EcsCallbackApi.InternalRequest request) {
        logger.info("[ Ecs ][ Callback ] in-progress start - orderKey={}", request.getOrderKey());
        try {
            validateOrderKey(request.getOrderKey());
            EcsCallbackApi.Response response = internalEcsCallbackService.inProgress(request.getOrderKey());
            logger.info("[ Ecs ][ Callback ] in-progress completed - orderKey={}", request.getOrderKey());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("[ Ecs ][ Callback ] in-progress rejected - orderKey={}, reason={}", request.getOrderKey(), e.getMessage());
            return ResponseEntity.badRequest().body(failResponse(request.getOrderKey(), e.getMessage()));
        } catch (Exception e) {
            logger.error("[ Ecs ][ Callback ] in-progress failed - orderKey={}", request.getOrderKey(), e);
            return ResponseEntity.internalServerError()
                    .body(failResponse(request.getOrderKey(), "Internal server error: " + e.getMessage()));
        }
    }

    // 출발지 로딩 완료 콜백
    @PostMapping("/from-loading-complete")
    public ResponseEntity<EcsCallbackApi.Response> fromLoadingComplete(@RequestBody EcsCallbackApi.InternalRequest request) {
        logger.info("[ Ecs ][ Callback ] from-loading-complete start - orderKey={}", request.getOrderKey());
        try {
            validateOrderKey(request.getOrderKey());
            EcsCallbackApi.Response response = internalEcsCallbackService.fromLoadingComplete(request.getOrderKey());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("[ Ecs ][ Callback ] from-loading-complete rejected - orderKey={}, reason={}", request.getOrderKey(), e.getMessage());
            return ResponseEntity.badRequest().body(failResponse(request.getOrderKey(), e.getMessage()));
        } catch (Exception e) {
            logger.error("[ Ecs ][ Callback ] from-loading-complete failed - orderKey={}", request.getOrderKey(), e);
            return ResponseEntity.internalServerError().body(failResponse(request.getOrderKey(), "Internal server error: " + e.getMessage()));
        }
    }

    // 목적지 언로딩 완료 콜백
    @PostMapping("/to-unloading-complete")
    public ResponseEntity<EcsCallbackApi.Response> toUnloadingComplete(@RequestBody EcsCallbackApi.InternalRequest request) {
        logger.info("[ Ecs ][ Callback ] to-unloading-complete start - orderKey={}", request.getOrderKey());
        try {
            validateOrderKey(request.getOrderKey());
            EcsCallbackApi.Response response = internalEcsCallbackService.toUnloadingComplete(request.getOrderKey());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("[ Ecs ][ Callback ] to-unloading-complete rejected - orderKey={}, reason={}", request.getOrderKey(), e.getMessage());
            return ResponseEntity.badRequest().body(failResponse(request.getOrderKey(), e.getMessage()));
        } catch (Exception e) {
            logger.error("[ Ecs ][ Callback ] to-unloading-complete failed - orderKey={}", request.getOrderKey(), e);
            return ResponseEntity.internalServerError().body(failResponse(request.getOrderKey(), "Internal server error: " + e.getMessage()));
        }
    }

    // 작업 완료 콜백
    @PostMapping("/complete")
    public ResponseEntity<EcsCallbackApi.Response> complete(@RequestBody EcsCallbackApi.InternalRequest request) {
        logger.info("[ Ecs ][ Callback ] complete start - orderKey={}", request.getOrderKey());
        try {
            validateOrderKey(request.getOrderKey());
            EcsCallbackApi.Response response = internalEcsCallbackService.complete(request.getOrderKey());
            logger.info("[ Ecs ][ Callback ] complete completed - orderKey={}", request.getOrderKey());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("[ Ecs ][ Callback ] complete rejected - orderKey={}, reason={}", request.getOrderKey(), e.getMessage());
            return ResponseEntity.badRequest().body(failResponse(request.getOrderKey(), e.getMessage()));
        } catch (Exception e) {
            logger.error("[ Ecs ][ Callback ] complete failed - orderKey={}", request.getOrderKey(), e);
            return ResponseEntity.internalServerError()
                    .body(failResponse(request.getOrderKey(), "Internal server error: " + e.getMessage()));
        }
    }

    // 작업 실패 콜백
    @PostMapping("/error")
    public ResponseEntity<EcsCallbackApi.Response> error(@RequestBody EcsCallbackApi.InternalRequest request) {
        logger.info("[ Ecs ][ Callback ] error start - orderKey={}, errorCode={}, message={}",
                request.getOrderKey(), request.getErrorCode(), request.getMessage());
        try {
            validateOrderKey(request.getOrderKey());
            EcsCallbackApi.Response response = internalEcsCallbackService.error(
                    request.getOrderKey(),
                    request.getErrorCode(),
                    request.getMessage()
            );
            logger.info("[ Ecs ][ Callback ] error completed - orderKey={}", request.getOrderKey());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("[ Ecs ][ Callback ] error rejected - orderKey={}, reason={}", request.getOrderKey(), e.getMessage());
            return ResponseEntity.badRequest().body(failResponse(request.getOrderKey(), e.getMessage()));
        } catch (Exception e) {
            logger.error("[ Ecs ][ Callback ] error failed - orderKey={}", request.getOrderKey(), e);
            return ResponseEntity.internalServerError()
                    .body(failResponse(request.getOrderKey(), "Internal server error: " + e.getMessage()));
        }
    }

    // 작업 취소 콜백
    @PostMapping("/cancelled")
    public ResponseEntity<EcsCallbackApi.Response> cancelled(@RequestBody EcsCallbackApi.InternalRequest request) {
        logger.info("[ Ecs ][ Callback ] cancelled start - orderKey={}", request.getOrderKey());
        try {
            validateOrderKey(request.getOrderKey());
            EcsCallbackApi.Response response = internalEcsCallbackService.cancelled(request.getOrderKey());
            logger.info("[ Ecs ][ Callback ] cancelled completed - orderKey={}", request.getOrderKey());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("[ Ecs ][ Callback ] cancelled rejected - orderKey={}, reason={}", request.getOrderKey(), e.getMessage());
            return ResponseEntity.badRequest().body(failResponse(request.getOrderKey(), e.getMessage()));
        } catch (Exception e) {
            logger.error("[ Ecs ][ Callback ] cancelled failed - orderKey={}", request.getOrderKey(), e);
            return ResponseEntity.internalServerError()
                    .body(failResponse(request.getOrderKey(), "Internal server error: " + e.getMessage()));
        }
    }

    // 렉단 컨베이어 도착 완료 콜백 - 입고 명령 전송 + AGF 입고 리필 트리거
    @PostMapping("/conveyor-arrived")
    public ResponseEntity<EcsCallbackApi.Response> conveyorArrived(@RequestBody EcsCallbackApi.InternalRequest request) {
        logger.info("[ Ecs ][ Callback ] conveyor-arrived start - orderKey={}", request.getOrderKey());
        try {
            validateOrderKey(request.getOrderKey());
            EcsCallbackApi.Response response = internalEcsCallbackService.conveyorArrived(request.getOrderKey());
            logger.info("[ Ecs ][ Callback ] conveyor-arrived completed - orderKey={}", request.getOrderKey());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("[ Ecs ][ Callback ] conveyor-arrived rejected - orderKey={}, reason={}", request.getOrderKey(), e.getMessage());
            return ResponseEntity.badRequest().body(failResponse(request.getOrderKey(), e.getMessage()));
        } catch (Exception e) {
            logger.error("[ Ecs ][ Callback ] conveyor-arrived failed - orderKey={}", request.getOrderKey(), e);
            return ResponseEntity.internalServerError()
                    .body(failResponse(request.getOrderKey(), "Internal server error: " + e.getMessage()));
        }
    }

    // 입고 BCR 스캔 통합 진입점 - (eqGroupId, barcode, scanPortCode) 로 입고 toLocation 확정 + ECS 가동 신호 송출.
    // 입고는 OrderScheduler 자동 송신 없음 - 이 엔드포인트가 유일한 송신 트리거.
    @PostMapping("/inbound-bcr-scan")
    public ResponseEntity<EcsCallbackApi.Response> inboundBcrScan(@RequestBody InboundBcrScanRequest request) {
        // 필수 파라미터 검증
        if (ValueUtil.isEmpty(request)
                || ValueUtil.isEmpty(request.getEqGroupId())
                || ValueUtil.isEmpty(request.getBarcode())) {
            return ResponseEntity.badRequest().body(EcsCallbackApi.Response.builder()
                    .success(false)
                    .message("eqGroupId, barcode are required")
                    .build());
        }

        try {
            boolean ok = ecsCommandService.processInboundBcrScanByBarcode(
                    request.getEqGroupId(), request.getBarcode(), request.getScanPortCode());
            return ResponseEntity.ok(EcsCallbackApi.Response.builder()
                    .success(ok)
                    .message(ok ? "OK" : "no match or dispatch failed")
                    .build());
        } catch (Exception e) {
            logger.error("[ Ecs ][ Callback ] inbound-bcr-scan failed - eqGroupId={}, barcode={}",
                    request.getEqGroupId(), request.getBarcode(), e);
            return ResponseEntity.internalServerError().body(EcsCallbackApi.Response.builder()
                    .success(false)
                    .message("Internal server error: " + e.getMessage())
                    .build());
        }
    }

    @Getter
    @Setter
    public static class InboundBcrScanRequest {
        @JsonProperty("eqGroupId")
        private String eqGroupId;

        @JsonProperty("barcode")
        private String barcode;

        @JsonProperty("scanPortCode")
        private String scanPortCode;
    }

    // orderKey 필수 검증
    private void validateOrderKey(String orderKey) {
        if (ValueUtil.isEmpty(orderKey) || orderKey.trim().isEmpty()) {
            throw new IllegalArgumentException("orderKey is required");
        }
    }

    // 실패 응답 생성
    private EcsCallbackApi.Response failResponse(String orderKey, String message) {
        return EcsCallbackApi.Response.builder()
                .orderKey(orderKey)
                .success(false)
                .message(message)
                .build();
    }
}
