package operato.logis.kmat_2026.biz.ecs.sineva.rest;

import operato.logis.kmat_2026.biz.ecs.sineva.SinevaEcsFacade;
import operato.logis.kmat_2026.biz.ecs.sineva.service.OrderCommandService;
import operato.logis.kmat_2026.entity.TbWcsOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.util.ValueUtil;

import java.util.Map;

/**
 * =============================================================================
 * Sineva ECS Facade Test Controller
 * =============================================================================
 *
 * [역할]
 * - SinevaEcsFacade 테스트용 REST Controller
 * - Postman 등으로 facade 진입 로직을 단건 호출/검증하기 위한 용도
 *
 * [테스트 대상]
 * - ShuttleInbound execute
 * - ShuttleInbound callback
 * - Cancel task execute
 *
 * [주의]
 * - DTO 없이 Map<String, Object> 기반으로 테스트 가능하게 구성
 * - 운영 반영 시 보안/권한/프로파일 제한 권장
 */
@RestController
@RequestMapping("/rest/ecs/sineva/facade-test")
public class SinevaEcsFacadeTestController {

    private static final Logger logger = LoggerFactory.getLogger(SinevaEcsFacadeTestController.class);

    @Autowired
    private SinevaEcsFacade sinevaEcsFacade;

    @Autowired
    private OrderCommandService orderCommandService;

    /**
     * =========================================================================
     * 1. Shuttle Inbound Execute
     * =========================================================================
     * POST /rest/ecs/sineva/facade-test/shuttle-inbound/execute
     */
    @PostMapping("/shuttle-inbound/execute")
    public ResponseEntity<Map<String, Object>> shuttleInboundExecute(@RequestBody Map<String, Object> request) {
        logger.info("[SinevaEcsFacadeTestController] shuttleInboundExecute request={}", request);

        try {
            String fromLocationCd = getString(request, "fromLocationCd");
            validateRequired(fromLocationCd, "fromLocationCd");

            TbWcsOrder order = sinevaEcsFacade.handleTspgConveyorInboundExecute(fromLocationCd);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Shuttle inbound execute success",
                    "data", order
            ));
        } catch (IllegalArgumentException e) {
            logger.warn("[SinevaEcsFacadeTestController] shuttleInboundExecute validation fail: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("[SinevaEcsFacadeTestController] shuttleInboundExecute unexpected error", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }


    /**
     * =========================================================================
     * 1. Shuttle Inbound Execute
     * =========================================================================
     * POST /rest/ecs/sineva/facade-test/shuttle-inbound/execute
     */
    @PostMapping("/shuttle-outbound/execute")
    public ResponseEntity<Map<String, Object>> shuttleOutboundExecute(@RequestBody Map<String, Object> request) {
        logger.info("[SinevaEcsFacadeTestController] shuttleOutboundExecute request={}", request);

        try {
            String fromLocationCd = getString(request, "fromLocationCd");
            validateRequired(fromLocationCd, "fromLocationCd");

            TbWcsOrder order = sinevaEcsFacade.handleTspgConveyorOutboundExecute(fromLocationCd);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Shuttle inbound execute success",
                    "data", order
            ));
        } catch (IllegalArgumentException e) {
            logger.warn("[SinevaEcsFacadeTestController] shuttleOutboundExecute validation fail: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("[SinevaEcsFacadeTestController] shuttleOutboundExecute unexpected error", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * =========================================================================
     * 2. Shuttle Inbound Callback
     * =========================================================================
     * POST /rest/ecs/sineva/facade-test/shuttle-inbound/callback
     *
     * [설명]
     * - callback 테스트용으로 최소 TbWcsOrder 객체를 만들어 facade callback 진입
     * - 실제 callback 전체 정책은 별도 callback/event 흐름이 주체지만,
     *   facade callback 연결 확인용으로 사용 가능
     */
    @PostMapping("/shuttle-inbound/callback")
    public ResponseEntity<Map<String, Object>> shuttleInboundCallback(@RequestBody Map<String, Object> request) {
        logger.info("[SinevaEcsFacadeTestController] shuttleInboundCallback request={}", request);

        try {
            String orderId = getString(request, "orderId");
            String taskId = getString(request, "taskId");
            String cbkStatus = getString(request, "cbkStatus");

            validateRequired(orderId, "orderId");

            TbWcsOrder order = new TbWcsOrder();
            order.setOrderId(orderId);
            order.setTaskId(ValueUtil.isEmpty(taskId) ? orderId : taskId);
            order.setCbkStatus(cbkStatus);

            sinevaEcsFacade.handleTspgConveyorInboundCallback(order);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Shuttle inbound callback success",
                    "data", order
            ));
        } catch (IllegalArgumentException e) {
            logger.warn("[SinevaEcsFacadeTestController] shuttleInboundCallback validation fail: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("[SinevaEcsFacadeTestController] shuttleInboundCallback unexpected error", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * =========================================================================
     * 3. Cancel Task Execute
     * =========================================================================
     * POST /rest/ecs/sineva/facade-test/cancel/execute
     */
    @PostMapping("/cancel/execute")
    public ResponseEntity<Map<String, Object>> cancelTaskExecute(@RequestBody Map<String, Object> request) {
        logger.info("[SinevaEcsFacadeTestController] cancelTaskExecute request={}", request);

        try {
            String orderId = getString(request, "orderId");
            validateRequired(orderId, "orderId");

            TbWcsOrder order = sinevaEcsFacade.handleCancelTaskExecute(orderId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cancel task execute success",
                    "data", order
            ));
        } catch (IllegalArgumentException e) {
            logger.warn("[SinevaEcsFacadeTestController] cancelTaskExecute validation fail: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("[SinevaEcsFacadeTestController] cancelTaskExecute unexpected error", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // -------------------------------------------------------------------------
    // helper
    // -------------------------------------------------------------------------

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value).trim();
    }

    private void validateRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }


    /**
     * =========================================================================
     * 0. WCS Order 직접 생성 + ECS 초기 지시 전송
     * =========================================================================
     * POST /rest/ecs/sineva/facade-test/order/create-initial
     *
     * [필수]
     * - fromSide
     * - toSide
     * - commandType
     * - equipType
     *
     * [선택]
     * - orderId
     * - taskId
     * - taskType
     * - currentStep
     * - priority
     * - remark
     * - statusList
     * - equipId
     * - podCd
     * - attribute1 ~ attribute4
     */
    @PostMapping("/order/create-initial")
    public ResponseEntity<Map<String, Object>> createInitialOrder(@RequestBody Map<String, Object> request) {
        logger.info("[SinevaEcsFacadeTestController] createInitialOrder request={}", request);

        try {
            String fromSide = getString(request, "fromSide");
            String toSide = getString(request, "toSide");
            String commandType = getString(request, "commandType");
            String equipType = getString(request, "equipType");

            validateRequired(fromSide, "fromSide");
            validateRequired(toSide, "toSide");
            validateRequired(commandType, "commandType");
            validateRequired(equipType, "equipType");

            TbWcsOrder order = new TbWcsOrder();

            // -----------------------------------------------------------------
            // 필수 필드
            // -----------------------------------------------------------------
            order.setFromSide(fromSide);
            order.setToSide(toSide);
            order.setCommandType(commandType);
            order.setEquipType(equipType);

            // -----------------------------------------------------------------
            // 선택 필드
            // -----------------------------------------------------------------
            order.setOrderId(getString(request, "orderId"));
            order.setTaskId(getString(request, "taskId"));
            order.setTaskType(getString(request, "taskType"));
            order.setRemark(getString(request, "remark"));
            order.setStatusList(getString(request, "statusList"));
            order.setEquipId(getString(request, "equipId"));
            order.setPodCd(getString(request, "podCd"));

            order.setAttribute1(getString(request, "attribute1"));
            order.setAttribute2(getString(request, "attribute2"));
            order.setAttribute3(getString(request, "attribute3"));
            order.setAttribute4(getString(request, "attribute4"));

            Integer currentStep = getInteger(request, "currentStep");
            if (currentStep != null) {
                order.setCurrentStep(currentStep);
            }

            Integer priority = getInteger(request, "priority");
            if (priority != null) {
                order.setPriority(priority);
            }

            TbWcsOrder createdOrder = orderCommandService.createAndSendInitialWcsOrder(order);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Initial WCS order created and ECS command sent successfully",
                    "data", createdOrder
            ));
        } catch (IllegalArgumentException e) {
            logger.warn("[SinevaEcsFacadeTestController] createInitialOrder validation fail: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("[SinevaEcsFacadeTestController] createInitialOrder unexpected error", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // -------------------------------------------------------------------------
    // helper
    // -------------------------------------------------------------------------

    private Integer getInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }

        if (value instanceof Integer) {
            return (Integer) value;
        }

        String text = String.valueOf(value).trim();
        if (text.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " must be integer");
        }
    }
}