package operato.logis.kmat_2026.biz.ecs.sineva.rest;

import operato.logis.kmat_2026.biz.ecs.sineva.service.EcsCommandService;
import operato.logis.kmat_2026.entity.TbWcsOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import operato.logis.kmat_2026.service.impl.TbWcsOrderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import xyz.elidom.util.ValueUtil;

import java.util.HashMap;

import java.util.List;
import java.util.Map;

/**
 * =============================================================================
 * Sineva Interface Test Controller
 * =============================================================================
 *
 * [역할]
 * - Sineva ECS 연동 인터페이스 호출 여부만 빠르게 점검하기 위한 테스트용 REST Controller
 * - DTO 없이 Map<String, Object> 기반으로 Postman 테스트가 가능하도록 구성
 *
 * [테스트 대상 API]
 * - createTask
 * - releaseTask
 * - cancelTask
 * - setTaskPriority
 * - getAgvStatus
 * - setRobotRunningType
 * - skipPoint
 *
 * [주의]
 * - 운영 업무 플로우용이 아니라 인터페이스 단건 호출 확인용
 * - 운영 반영 시 권한/프로파일 제한 권장
 *
 * @author WCS Development Team
 * @since 2026-03-09
 */
@RestController
@RequestMapping("/rest/ecs/sineva/test")
public class SinevaInterfaceTestController {

    private static final Logger logger = LoggerFactory.getLogger(SinevaInterfaceTestController.class);

    @Autowired
    private EcsCommandService ecsCommandService;

    @Autowired
    private TbWcsOrderService tbWcsOrderService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${wcs.api.base-url:http://localhost:9500}")
    private String wcsBaseUrl;

    /**
     * =========================================================================
     * 1. createTask 테스트
     * =========================================================================
     * POST /rest/ecs/sineva/test/task/create
     */
    @PostMapping("/task/create")
    public ResponseEntity<Map<String, Object>> createTask(@RequestBody Map<String, Object> request) {
        logger.info("[SinevaInterfaceTestController] createTask request={}", request);

        try {
            TbWcsOrder task = new TbWcsOrder();
            task.setOrderId(getString(request, "orderId"));
            task.setPriority(getInt(request, "priority", 50));
            task.setEquipType(getString(request, "equipType"));
            task.setEquipId(getString(request, "equipId"));
            task.setFromSide(getString(request, "fromSide"));
            task.setToSide(getString(request, "toSide"));
            task.setTaskType(getString(request, "taskType"));

            validateRequired(task.getOrderId(), "orderId");
            validateRequired(task.getEquipType(), "equipType");
            validateRequired(task.getEquipId(), "equipId");
            validateRequired(task.getFromSide(), "fromSide");
            validateRequired(task.getToSide(), "toSide");
            validateRequired(task.getTaskType(), "taskType");

            Map<String, Object> response = ecsCommandService.sendInitialTaskCommand(task);

            if (ecsCommandService.isSuccess(response)) {
                logger.info("[SinevaInterfaceTestController] createTask success: orderId={}", task.getOrderId());
                return ResponseEntity.ok(response);
            }

            logger.warn("[SinevaInterfaceTestController] createTask fail: orderId={}, response={}", task.getOrderId(), response);
            return ResponseEntity.badRequest().body(response);

        } catch (IllegalArgumentException e) {
            logger.warn("[SinevaInterfaceTestController] createTask validation fail: {}", e.getMessage());
            return ResponseEntity.badRequest().body(error("400", e.getMessage()));
        } catch (Exception e) {
            logger.error("[SinevaInterfaceTestController] createTask unexpected error", e);
            return ResponseEntity.internalServerError().body(error("500", "createTask unexpected error: " + e.getMessage()));
        }
    }

    /**
     * =========================================================================
     * 2. releaseTask 테스트
     * =========================================================================
     * POST /rest/ecs/sineva/test/task/release
     */
    @PostMapping("/task/release")
    public ResponseEntity<Map<String, Object>> releaseTask(@RequestBody Map<String, Object> request) {
        logger.info("[SinevaInterfaceTestController] releaseTask request={}", request);

        try {
            String orderId = getString(request, "orderId");
            validateRequired(orderId, "orderId");

            TbWcsOrder task = new TbWcsOrder();
            task.setOrderId(orderId);

            Map<String, Object> response = ecsCommandService.sendReleaseCommand(task);

            if (ecsCommandService.isSuccess(response)) {
                logger.info("[SinevaInterfaceTestController] releaseTask success: orderId={}", orderId);
                return ResponseEntity.ok(response);
            }

            logger.warn("[SinevaInterfaceTestController] releaseTask fail: orderId={}, response={}", orderId, response);
            return ResponseEntity.badRequest().body(response);

        } catch (IllegalArgumentException e) {
            logger.warn("[SinevaInterfaceTestController] releaseTask validation fail: {}", e.getMessage());
            return ResponseEntity.badRequest().body(error("400", e.getMessage()));
        } catch (Exception e) {
            logger.error("[SinevaInterfaceTestController] releaseTask unexpected error", e);
            return ResponseEntity.internalServerError().body(error("500", "releaseTask unexpected error: " + e.getMessage()));
        }
    }

    /**
     * =========================================================================
     * 3. cancelTask 테스트
     * =========================================================================
     * POST /rest/ecs/sineva/test/task/cancel
     */
    @PostMapping("/task/cancel")
    public ResponseEntity<Map<String, Object>> cancelTask(@RequestBody Map<String, Object> request) {
        logger.info("[SinevaInterfaceTestController] cancelTask request={}", request);

        try {
            String orderId = getString(request, "orderId");
            validateRequired(orderId, "orderId");

            TbWcsOrder task = new TbWcsOrder();
            task.setOrderId(orderId);

            Map<String, Object> response = ecsCommandService.cancelTask(task);

            if (ecsCommandService.isSuccess(response)) {
                logger.info("[SinevaInterfaceTestController] cancelTask success: orderId={}", orderId);
                return ResponseEntity.ok(response);
            }

            logger.warn("[SinevaInterfaceTestController] cancelTask fail: orderId={}, response={}", orderId, response);
            return ResponseEntity.badRequest().body(response);

        } catch (IllegalArgumentException e) {
            logger.warn("[SinevaInterfaceTestController] cancelTask validation fail: {}", e.getMessage());
            return ResponseEntity.badRequest().body(error("400", e.getMessage()));
        } catch (Exception e) {
            logger.error("[SinevaInterfaceTestController] cancelTask unexpected error", e);
            return ResponseEntity.internalServerError().body(error("500", "cancelTask unexpected error: " + e.getMessage()));
        }
    }

    /**
     * =========================================================================
     * 4. setTaskPriority 테스트
     * =========================================================================
     * POST /rest/ecs/sineva/test/task/priority
     */
    @PostMapping("/task/priority")
    public ResponseEntity<Map<String, Object>> setTaskPriority(@RequestBody Map<String, Object> request) {
        logger.info("[SinevaInterfaceTestController] setTaskPriority request={}", request);

        try {
            String orderId = getString(request, "orderId");
            Integer priority = getInt(request, "priority", null);

            validateRequired(orderId, "orderId");
            if (priority == null) {
                throw new IllegalArgumentException("priority is required");
            }

            TbWcsOrder task = new TbWcsOrder();
            task.setOrderId(orderId);

            Map<String, Object> response = ecsCommandService.setTaskPriority(task, priority);

            if (ecsCommandService.isSuccess(response)) {
                logger.info("[SinevaInterfaceTestController] setTaskPriority success: orderId={}, priority={}", orderId, priority);
                return ResponseEntity.ok(response);
            }

            logger.warn("[SinevaInterfaceTestController] setTaskPriority fail: orderId={}, priority={}, response={}",
                    orderId, priority, response);
            return ResponseEntity.badRequest().body(response);

        } catch (IllegalArgumentException e) {
            logger.warn("[SinevaInterfaceTestController] setTaskPriority validation fail: {}", e.getMessage());
            return ResponseEntity.badRequest().body(error("400", e.getMessage()));
        } catch (Exception e) {
            logger.error("[SinevaInterfaceTestController] setTaskPriority unexpected error", e);
            return ResponseEntity.internalServerError().body(error("500", "setTaskPriority unexpected error: " + e.getMessage()));
        }
    }

    /**
     * =========================================================================
     * 5. getAgvStatus 테스트
     * =========================================================================
     * GET /rest/ecs/sineva/test/agv/status
     */
    @GetMapping("/agv/status")
    public ResponseEntity<Map<String, Object>> getAgvStatus() {
        logger.info("[SinevaInterfaceTestController] getAgvStatus request");

        try {
            Map<String, Object> response = ecsCommandService.getAgvStatus();

            if (ecsCommandService.isSuccess(response)) {
                logger.info("[SinevaInterfaceTestController] getAgvStatus success");
                return ResponseEntity.ok(response);
            }

            logger.warn("[SinevaInterfaceTestController] getAgvStatus fail: response={}", response);
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            logger.error("[SinevaInterfaceTestController] getAgvStatus unexpected error", e);
            return ResponseEntity.internalServerError().body(error("500", "getAgvStatus unexpected error: " + e.getMessage()));
        }
    }

    /**
     * =========================================================================
     * 6. setRobotRunningType 테스트
     * =========================================================================
     * POST /rest/ecs/sineva/test/robot/running-type
     */
    @PostMapping("/robot/running-type")
    public ResponseEntity<Map<String, Object>> setRobotRunningType(@RequestBody Map<String, Object> request) {
        logger.info("[SinevaInterfaceTestController] setRobotRunningType request={}", request);

        try {
            Object vehiclesObj = request.get("vehicles");
            if (!(vehiclesObj instanceof List<?> rawList) || rawList.isEmpty()) {
                throw new IllegalArgumentException("vehicles is required and must be a non-empty array");
            }

            List<String> vehicles = rawList.stream()
                    .map(String::valueOf)
                    .toList();

            Map<String, Object> response = ecsCommandService.setRobotRunningType(vehicles);

            if (ecsCommandService.isSuccess(response)) {
                logger.info("[SinevaInterfaceTestController] setRobotRunningType success: vehicles={}", vehicles);
                return ResponseEntity.ok(response);
            }

            logger.warn("[SinevaInterfaceTestController] setRobotRunningType fail: vehicles={}, response={}", vehicles, response);
            return ResponseEntity.badRequest().body(response);

        } catch (IllegalArgumentException e) {
            logger.warn("[SinevaInterfaceTestController] setRobotRunningType validation fail: {}", e.getMessage());
            return ResponseEntity.badRequest().body(error("400", e.getMessage()));
        } catch (Exception e) {
            logger.error("[SinevaInterfaceTestController] setRobotRunningType unexpected error", e);
            return ResponseEntity.internalServerError().body(error("500", "setRobotRunningType unexpected error: " + e.getMessage()));
        }
    }

    /**
     * =========================================================================
     * 7. skipPoint 테스트
     * =========================================================================
     * POST /rest/ecs/sineva/test/point/skip
     */
    @PostMapping("/point/skip")
    public ResponseEntity<Map<String, Object>> skipPoint(@RequestBody Map<String, Object> request) {
        logger.info("[SinevaInterfaceTestController] skipPoint request={}", request);

        try {
            String equipId = getString(request, "equipId");
            validateRequired(equipId, "equipId");

            Map<String, Object> response = ecsCommandService.skipPoint(equipId);

            if (ecsCommandService.isSuccess(response)) {
                logger.info("[SinevaInterfaceTestController] skipPoint success: equipId={}", equipId);
                return ResponseEntity.ok(response);
            }

            logger.warn("[SinevaInterfaceTestController] skipPoint fail: equipId={}, response={}", equipId, response);
            return ResponseEntity.badRequest().body(response);

        } catch (IllegalArgumentException e) {
            logger.warn("[SinevaInterfaceTestController] skipPoint validation fail: {}", e.getMessage());
            return ResponseEntity.badRequest().body(error("400", e.getMessage()));
        } catch (Exception e) {
            logger.error("[SinevaInterfaceTestController] skipPoint unexpected error", e);
            return ResponseEntity.internalServerError().body(error("500", "skipPoint unexpected error: " + e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // internal helper
    // -------------------------------------------------------------------------

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value).trim();
    }

    private Integer getInt(Map<String, Object> map, String key, Integer defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        String str = String.valueOf(value).trim();
        if (str.isEmpty()) {
            return defaultValue;
        }

        return Integer.parseInt(str);
    }

    private void validateRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }

    private Map<String, Object> error(String code, String message) {
        return Map.of(
                "code", code,
                "message", message
        );
    }

    /**
     * =========================================================================
     * 8. AGF 자동 콜백 테스트
     * =========================================================================
     * POST /rest/ecs/sineva/test/task/auto-callback
     *
     * [역할]
     * - taskId 하나만 받아서
     *   1) 작업 시작(status=1)
     *   2) 로딩 완료(status=2)
     *   3) 작업 완료(status=4)
     *   콜백을 순차적으로 전송한다.
     *
     * [요청 예시]
     * {
     *   "taskId": "20260317-000001"
     * }
     */
    @PostMapping("/task/auto-callback")
    public ResponseEntity<Map<String, Object>> autoCallback(@RequestBody Map<String, Object> request) {
        logger.info("[SinevaInterfaceTestController] autoCallback request={}", request);

        try {
            String orderId = getString(request, "orderId");
            validateRequired(orderId, "orderId");

            TbWcsOrder order = tbWcsOrderService.findOrderByOrderId(orderId);
            if (ValueUtil.isEmpty(order)) {
                return ResponseEntity.badRequest().body(error("404", "order not found: " + orderId));
            }

            // 1. 작업 시작
            sendAgfStart(order);
            sleep(1000);

            // 2. 로딩 완료
            sendAgfFromLoadingComplete(order);
            sleep(1000);

            // 3. 작업 완료
            sendAgfComplete(order);

            logger.info("[SinevaInterfaceTestController] autoCallback success: taskId={}", orderId);

            return ResponseEntity.ok(Map.of(
                    "code", "200",
                    "message", "auto callback sent successfully",
                    "orderId", orderId
            ));

        } catch (IllegalArgumentException e) {
            logger.warn("[SinevaInterfaceTestController] autoCallback validation fail: {}", e.getMessage());
            return ResponseEntity.badRequest().body(error("400", e.getMessage()));
        } catch (Exception e) {
            logger.error("[SinevaInterfaceTestController] autoCallback unexpected error", e);
            return ResponseEntity.internalServerError().body(error("500", "autoCallback unexpected error: " + e.getMessage()));
        }
    }

    /**
     * AGF 시작 콜백 전송
     */
    private void sendAgfStart(TbWcsOrder order) {
        sendAgfCallback(
                order,
                "1",
                order.getFromPositionCod(),
                "AGF1"
        );
    }

    /**
     * AGF 로딩 완료 콜백 전송
     */
    private void sendAgfFromLoadingComplete(TbWcsOrder order) {
        sendAgfCallback(
                order,
                "2",
                order.getFromPositionCod(),
                "AGF1"
        );
    }

    /**
     * AGF 완료 콜백 전송
     */
    private void sendAgfComplete(TbWcsOrder order) {
        sendAgfCallback(
                order,
                "4",
                order.getToPositionCod(),
                "AGF1"
        );
    }

    /**
     * AGF 콜백 전송
     */
    private void sendAgfCallback(TbWcsOrder order, String status, String currentPositionCode, String robotCode) {
        Long domainId = extractDomainId(order);
        String url = wcsBaseUrl + "/rest/tbecsamhstask/" + domainId + "/robot/callback";

        Map<String, Object> body = new HashMap<>();
        body.put("taskId", order.getOrderId());
        body.put("status", status);
        body.put("currentPositionCode", currentPositionCode);
        body.put("errorCode", "0");
        body.put("robotCode", robotCode);

        logger.info("[SinevaInterfaceTestController] AGF callback send - taskId={}, status={}, position={}, domainId={}",
                order.getOrderId(), status, currentPositionCode, domainId);

        sendPostRequest(url, body);
    }

    /**
     * domainId 추출
     * - order에 domainId가 있으면 사용
     * - 없으면 기본값 7 사용
     */
    private Long extractDomainId(TbWcsOrder order) {
        try {
            Object domainIdObj = order.getDomainId();
            if (domainIdObj instanceof Number number) {
                return number.longValue();
            }
            if (domainIdObj != null) {
                return Long.parseLong(String.valueOf(domainIdObj));
            }
        } catch (Exception e) {
            logger.warn("[SinevaInterfaceTestController] domainId parse fail. default=7, reason={}", e.getMessage());
        }
        return 7L;
    }

    private void sendPostRequest(String url, Map<String, Object> body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            String response = restTemplate.postForObject(url, request, String.class);

            logger.debug("[SinevaInterfaceTestController] callback response={}", response);
        } catch (Exception e) {
            logger.error("[SinevaInterfaceTestController] callback request fail - url={}", url, e);
            throw e;
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("[SinevaInterfaceTestController] sleep interrupted");
        }
    }
}