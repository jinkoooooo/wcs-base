package operato.logis.kmat_2026.biz.wcs.kmat_2026.rest;

import operato.logis.kmat_2026.biz.wcs.kmat_2026.dto.KMat2026ScenarioContext;
import operato.logis.kmat_2026.biz.wcs.kmat_2026.service.KMat2026ScenarioTestService;
import operato.logis.kmat_2026.biz.wcs.kmat_2026.service.KMat2026WcsFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/rest/kmat2026/test")
public class KMat2026ScenarioTestController {

    private static final Logger logger = LoggerFactory.getLogger(KMat2026ScenarioTestController.class);

    @Autowired
    private KMat2026ScenarioTestService testService;

    @Autowired
    private KMat2026WcsFacade wcsFacade;

    @PostMapping("/auto-callbacks")
    public ResponseEntity<Map<String, Object>> runAutoCallbacks(
            @RequestParam(defaultValue = "3000") long delay) {

        logger.info("[TestController] 자동 콜백 실행 요청 - delay={}ms", delay);

        if (testService.isRunning()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "테스트가 이미 실행 중입니다"
            ));
        }

        KMat2026ScenarioContext ctx = wcsFacade.getContext();
        if (ctx == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "시나리오가 시작되지 않았습니다. /rest/kmat2026/scenario/start 먼저 호출하세요."
            ));
        }

        testService.runAutoCallbacks(delay);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "기본 자동 콜백 실행 시작됨 (비동기)",
                "mode", "BASIC",
                "delay", delay,
                "currentStep", ctx.getCurrentStep().toString()
        ));
    }

    @PostMapping("/auto-callbacks-varied")
    public ResponseEntity<Map<String, Object>> runVariedCallbacks(
            @RequestParam(defaultValue = "3000") long delay) {

        logger.info("[TestController] 다양한 자동 콜백 실행 요청 - delay={}ms", delay);

        if (testService.isRunning()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "테스트가 이미 실행 중입니다"
            ));
        }

        KMat2026ScenarioContext ctx = wcsFacade.getContext();
        if (ctx == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "시나리오가 시작되지 않았습니다. /rest/kmat2026/scenario/start 먼저 호출하세요."
            ));
        }

        testService.runVariedCallbacks(delay);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "다양한 자동 콜백 실행 시작됨 (비동기)",
                "mode", "VARIED",
                "description", "사이클마다 outbound 완료 순서 / 렉단 컨베이어 도착 시점을 달리하여 테스트",
                "delay", delay,
                "currentStep", ctx.getCurrentStep().toString()
        ));
    }

    @PostMapping("/reverse-order")
    public ResponseEntity<Map<String, Object>> runReverseOrderTest(
            @RequestParam(defaultValue = "3000") long delay) {

        logger.info("[TestController] 역순 테스트 실행 요청 - delay={}ms", delay);

        if (testService.isRunning()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "테스트가 이미 실행 중입니다"
            ));
        }

        KMat2026ScenarioContext ctx = wcsFacade.getContext();
        if (ctx == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "시나리오가 시작되지 않았습니다. /rest/kmat2026/scenario/start 먼저 호출하세요."
            ));
        }

        testService.runReverseOrderTest(delay);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "역순 테스트 실행 시작됨 (비동기)",
                "mode", "REVERSE",
                "delay", delay,
                "currentStep", ctx.getCurrentStep().toString()
        ));
    }

    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopTest() {
        logger.info("[TestController] 테스트 중지 요청");

        testService.stop();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "테스트 중지 요청됨"
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        KMat2026ScenarioContext ctx = wcsFacade.getContext();

        Map<String, Object> result = new HashMap<>();
        result.put("testRunning", testService.isRunning());
        result.put("testMode", testService.getCurrentMode());
        result.put("currentVariant", testService.getCurrentVariantName());
        result.put("lastExecutedCycleNo", testService.getLastExecutedCycleNo());

        if (ctx != null) {
            result.put("scenarioActive", true);
            result.put("currentStep", ctx.getCurrentStep());
            result.put("outbound1OrderKey", ctx.getOutbound1OrderKey());
            result.put("outbound2OrderKey", ctx.getOutbound2OrderKey());
            result.put("move1OrderKey", ctx.getMove1OrderKey());
            result.put("move2OrderKey", ctx.getMove2OrderKey());
            result.put("inbound1OrderKey", ctx.getInbound1OrderKey());
            result.put("inbound2OrderKey", ctx.getInbound2OrderKey());
            result.put("conveyorArrivedWaiting", ctx.isConveyorArrivedWaiting());
        } else {
            result.put("scenarioActive", false);
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/tspg-complete")
    public ResponseEntity<Map<String, Object>> sendTspgComplete(@RequestBody Map<String, String> request) {
        String orderKey = request.get("orderKey");

        if (orderKey == null || orderKey.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "orderKey is required"
            ));
        }

        logger.info("[TestController] TSPG 완료 콜백 전송 - orderKey={}", orderKey);
        testService.sendTspgComplete(orderKey);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "TSPG 완료 콜백 전송됨",
                "orderKey", orderKey
        ));
    }

    @PostMapping("/step1-last-complete")
    public ResponseEntity<Map<String, Object>> sendStep1LastComplete(@RequestBody Map<String, String> request) {
        String orderKey = request.get("orderKey");

        if (orderKey == null || orderKey.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "orderKey is required"
            ));
        }

        logger.info("[TestController] step1 마지막 완료 상황 테스트 - orderKey={}", orderKey);
        testService.sendStep1LastComplete(orderKey);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "step1 마지막 완료 상황 테스트 전송됨",
                "orderKey", orderKey
        ));
    }

    @PostMapping("/conveyor-arrived")
    public ResponseEntity<Map<String, Object>> sendConveyorArrived(@RequestBody Map<String, String> request) {
        String orderKey = request.get("orderKey");

        if (orderKey == null || orderKey.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "orderKey is required"
            ));
        }

        logger.info("[TestController] 렉단 컨베이어 도착 콜백 전송 - orderKey={}", orderKey);
        testService.sendConveyorArrived(orderKey);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "렉단 컨베이어 도착 콜백 전송됨",
                "orderKey", orderKey
        ));
    }

    @PostMapping("/agf-complete")
    public ResponseEntity<Map<String, Object>> sendAgfComplete(@RequestBody Map<String, String> request) {
        String taskId = request.get("taskId");
        String currentPositionCode = request.get("currentPositionCode");

        if (taskId == null || taskId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "taskId is required"
            ));
        }

        if (currentPositionCode == null || currentPositionCode.isEmpty()) {
            currentPositionCode = "UNKNOWN";
        }

        logger.info("[TestController] AGF 완료 콜백 전송 - taskId={}, position={}", taskId, currentPositionCode);
        testService.sendAgfComplete(taskId, currentPositionCode);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "AGF 완료 콜백 전송됨",
                "taskId", taskId,
                "currentPositionCode", currentPositionCode
        ));
    }

    @GetMapping("/order-keys")
    public ResponseEntity<Map<String, Object>> getOrderKeys() {
        KMat2026ScenarioContext ctx = wcsFacade.getContext();

        if (ctx == null) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "활성 시나리오 없음"
            ));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("outbound1", ctx.getOutbound1OrderKey());
        result.put("outbound2", ctx.getOutbound2OrderKey());
        result.put("move1", ctx.getMove1OrderKey());
        result.put("move2", ctx.getMove2OrderKey());
        result.put("inbound1", ctx.getInbound1OrderKey());
        result.put("inbound2", ctx.getInbound2OrderKey());

        return ResponseEntity.ok(result);
    }
}