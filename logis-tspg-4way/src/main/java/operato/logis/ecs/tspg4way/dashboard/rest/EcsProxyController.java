package operato.logis.ecs.tspg4way.dashboard.rest;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * ECS 제어 명령 프록시 Controller
 *
 * LMS 2D → ECS 방향의 제어 명령을 프록시.
 * LMS 2D는 제어 로직을 직접 구현하지 않고, ECS 제어 API를 호출하는 트리거 역할만 함.
 *
 * 명령 실행 원칙:
 * - 명령의 진짜 실행과 상태의 진실은 ECS
 * - LMS 2D는 ECS에 명령을 전달하고 즉시 ACK(accepted/commandId/message)를 받음
 * - 실제 상태 변화 반영은 ECS가 발행하는 이벤트로 자동 갱신
 *
 * 네트워크/보안 정책에 따라:
 * - "프론트가 ECS를 직접 호출" - 이 컨트롤러 불필요
 * - "LMS 2D 백엔드가 ECS로 프록시 호출" - 이 컨트롤러 사용
 */
@RestController
@RequestMapping("/rest/shuttle/ecs-proxy")
public class EcsProxyController {

    private static final Logger logger = LoggerFactory.getLogger(EcsProxyController.class);

    @Autowired
    private RestTemplate restTemplate;

    // ECS API 기본 URL (설정 파일에서 주입)
    @Value("${ecs.api.base-url:http://localhost:8080/ecs}")
    private String ecsBaseUrl;

    // ECS API 타임아웃 (ms)
    @Value("${ecs.api.timeout:5000}")
    private int ecsTimeout;

    // 작업 제어 API

    /**
     * 작업 취소 요청
     * POST /rest/shuttle/ecs-proxy/{lcId}/jobs/{jobKey}/cancel
     *
     * ECS: POST /ecs/jobs/{jobKey}/cancel
     */
    @PostMapping("/{lcId}/jobs/{jobKey}/cancel")
    public ResponseEntity<Map<String, Object>> cancelJob(
            @PathVariable String lcId,
            @PathVariable String jobKey,
            @RequestBody(required = false) Map<String, Object> body) {

        logger.info("[ Ecs ][ Proxy ] cancel job: lcId={}, jobKey={}", lcId, jobKey);

        try {
            String ecsUrl = ecsBaseUrl + "/jobs/" + jobKey + "/cancel";
            HttpHeaders headers = createHeaders(lcId);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(ecsUrl, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok(Map.of(
                    "accepted", true,
                    "commandId", generateCommandId(),
                    "message", "Job cancel request accepted",
                    "jobKey", jobKey,
                    "ecsResponse", response.getBody()
                ));
            } else {
                return ResponseEntity.status(response.getStatusCode()).body(Map.of(
                    "accepted", false,
                    "message", "ECS rejected the cancel request",
                    "jobKey", jobKey,
                    "ecsStatus", response.getStatusCode().value()
                ));
            }
        } catch (RestClientException e) {
            logger.error("[ Ecs ][ Proxy ] cancel job failed", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "accepted", false,
                "message", "Failed to connect to ECS: " + e.getMessage(),
                "jobKey", jobKey
            ));
        }
    }

    /**
     * 작업 재개 요청
     * POST /rest/shuttle/ecs-proxy/{lcId}/jobs/{jobKey}/resume
     *
     * ECS: POST /ecs/jobs/{jobKey}/resume
     */
    @PostMapping("/{lcId}/jobs/{jobKey}/resume")
    public ResponseEntity<Map<String, Object>> resumeJob(
            @PathVariable String lcId,
            @PathVariable String jobKey,
            @RequestBody(required = false) Map<String, Object> body) {

        logger.info("[ Ecs ][ Proxy ] resume job: lcId={}, jobKey={}", lcId, jobKey);

        try {
            String ecsUrl = ecsBaseUrl + "/jobs/" + jobKey + "/resume";
            HttpHeaders headers = createHeaders(lcId);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(ecsUrl, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok(Map.of(
                    "accepted", true,
                    "commandId", generateCommandId(),
                    "message", "Job resume request accepted",
                    "jobKey", jobKey,
                    "ecsResponse", response.getBody()
                ));
            } else {
                return ResponseEntity.status(response.getStatusCode()).body(Map.of(
                    "accepted", false,
                    "message", "ECS rejected the resume request",
                    "jobKey", jobKey,
                    "ecsStatus", response.getStatusCode().value()
                ));
            }
        } catch (RestClientException e) {
            logger.error("[ Ecs ][ Proxy ] resume job failed", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "accepted", false,
                "message", "Failed to connect to ECS: " + e.getMessage(),
                "jobKey", jobKey
            ));
        }
    }

    /**
     * 작업 우선순위 변경 요청
     * PUT /rest/shuttle/ecs-proxy/{lcId}/jobs/{jobKey}/priority
     *
     * ECS: PUT /ecs/jobs/{jobKey}/priority
     */
    @PutMapping("/{lcId}/jobs/{jobKey}/priority")
    public ResponseEntity<Map<String, Object>> updateJobPriority(
            @PathVariable String lcId,
            @PathVariable String jobKey,
            @RequestBody Map<String, Object> body) {

        logger.info("[ Ecs ][ Proxy ] update priority: lcId={}, jobKey={}, priority={}",
            lcId, jobKey, body.get("priority"));

        try {
            String ecsUrl = ecsBaseUrl + "/jobs/" + jobKey + "/priority";
            HttpHeaders headers = createHeaders(lcId);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                ecsUrl, HttpMethod.PUT, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok(Map.of(
                    "accepted", true,
                    "commandId", generateCommandId(),
                    "message", "Job priority update accepted",
                    "jobKey", jobKey,
                    "ecsResponse", response.getBody()
                ));
            } else {
                return ResponseEntity.status(response.getStatusCode()).body(Map.of(
                    "accepted", false,
                    "message", "ECS rejected the priority update",
                    "jobKey", jobKey
                ));
            }
        } catch (RestClientException e) {
            logger.error("[ Ecs ][ Proxy ] update priority failed", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "accepted", false,
                "message", "Failed to connect to ECS: " + e.getMessage(),
                "jobKey", jobKey
            ));
        }
    }

    // 설비 제어 API

    /**
     * 셔틀 정지 요청
     * POST /rest/shuttle/ecs-proxy/{lcId}/shuttles/{shuttleCode}/stop
     */
    @PostMapping("/{lcId}/shuttles/{shuttleCode}/stop")
    public ResponseEntity<Map<String, Object>> stopShuttle(
            @PathVariable String lcId,
            @PathVariable String shuttleCode,
            @RequestBody(required = false) Map<String, Object> body) {

        logger.info("[ Ecs ][ Proxy ] stop shuttle: lcId={}, shuttleCode={}", lcId, shuttleCode);

        try {
            String ecsUrl = ecsBaseUrl + "/shuttles/" + shuttleCode + "/stop";
            HttpHeaders headers = createHeaders(lcId);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(ecsUrl, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok(Map.of(
                    "accepted", true,
                    "commandId", generateCommandId(),
                    "message", "Shuttle stop request accepted",
                    "shuttleCode", shuttleCode,
                    "ecsResponse", response.getBody()
                ));
            } else {
                return ResponseEntity.status(response.getStatusCode()).body(Map.of(
                    "accepted", false,
                    "message", "ECS rejected the stop request",
                    "shuttleCode", shuttleCode
                ));
            }
        } catch (RestClientException e) {
            logger.error("[ Ecs ][ Proxy ] stop shuttle failed", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "accepted", false,
                "message", "Failed to connect to ECS: " + e.getMessage(),
                "shuttleCode", shuttleCode
            ));
        }
    }

    /**
     * 셔틀 재시작 요청
     * POST /rest/shuttle/ecs-proxy/{lcId}/shuttles/{shuttleCode}/restart
     */
    @PostMapping("/{lcId}/shuttles/{shuttleCode}/restart")
    public ResponseEntity<Map<String, Object>> restartShuttle(
            @PathVariable String lcId,
            @PathVariable String shuttleCode,
            @RequestBody(required = false) Map<String, Object> body) {

        logger.info("[ Ecs ][ Proxy ] restart shuttle: lcId={}, shuttleCode={}", lcId, shuttleCode);

        try {
            String ecsUrl = ecsBaseUrl + "/shuttles/" + shuttleCode + "/restart";
            HttpHeaders headers = createHeaders(lcId);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(ecsUrl, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok(Map.of(
                    "accepted", true,
                    "commandId", generateCommandId(),
                    "message", "Shuttle restart request accepted",
                    "shuttleCode", shuttleCode,
                    "ecsResponse", response.getBody()
                ));
            } else {
                return ResponseEntity.status(response.getStatusCode()).body(Map.of(
                    "accepted", false,
                    "message", "ECS rejected the restart request",
                    "shuttleCode", shuttleCode
                ));
            }
        } catch (RestClientException e) {
            logger.error("[ Ecs ][ Proxy ] restart shuttle failed", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "accepted", false,
                "message", "Failed to connect to ECS: " + e.getMessage(),
                "shuttleCode", shuttleCode
            ));
        }
    }

    /**
     * 셔틀 수동 이동 요청
     * POST /rest/shuttle/ecs-proxy/{lcId}/shuttles/{shuttleCode}/move
     */
    @PostMapping("/{lcId}/shuttles/{shuttleCode}/move")
    public ResponseEntity<Map<String, Object>> moveShuttle(
            @PathVariable String lcId,
            @PathVariable String shuttleCode,
            @RequestBody Map<String, Object> body) {

        logger.info("[ Ecs ][ Proxy ] move shuttle: lcId={}, shuttleCode={}, target={}",
            lcId, shuttleCode, body.get("targetPointCode"));

        try {
            String ecsUrl = ecsBaseUrl + "/shuttles/" + shuttleCode + "/move";
            HttpHeaders headers = createHeaders(lcId);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(ecsUrl, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok(Map.of(
                    "accepted", true,
                    "commandId", generateCommandId(),
                    "message", "Shuttle move request accepted",
                    "shuttleCode", shuttleCode,
                    "ecsResponse", response.getBody()
                ));
            } else {
                return ResponseEntity.status(response.getStatusCode()).body(Map.of(
                    "accepted", false,
                    "message", "ECS rejected the move request",
                    "shuttleCode", shuttleCode
                ));
            }
        } catch (RestClientException e) {
            logger.error("[ Ecs ][ Proxy ] move shuttle failed", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "accepted", false,
                "message", "Failed to connect to ECS: " + e.getMessage(),
                "shuttleCode", shuttleCode
            ));
        }
    }

    // 알람 제어 API

    /**
     * 알람 확인(ACK) 요청
     * POST /rest/shuttle/ecs-proxy/{lcId}/alarms/{alarmId}/ack
     */
    @PostMapping("/{lcId}/alarms/{alarmId}/ack")
    public ResponseEntity<Map<String, Object>> acknowledgeAlarm(
            @PathVariable String lcId,
            @PathVariable String alarmId,
            @RequestBody(required = false) Map<String, Object> body) {

        logger.info("[ Ecs ][ Proxy ] acknowledge alarm: lcId={}, alarmId={}", lcId, alarmId);

        try {
            String ecsUrl = ecsBaseUrl + "/alarms/" + alarmId + "/ack";
            HttpHeaders headers = createHeaders(lcId);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(ecsUrl, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok(Map.of(
                    "accepted", true,
                    "commandId", generateCommandId(),
                    "message", "Alarm acknowledge request accepted",
                    "alarmId", alarmId,
                    "ecsResponse", response.getBody()
                ));
            } else {
                return ResponseEntity.status(response.getStatusCode()).body(Map.of(
                    "accepted", false,
                    "message", "ECS rejected the alarm acknowledge",
                    "alarmId", alarmId
                ));
            }
        } catch (RestClientException e) {
            logger.error("[ Ecs ][ Proxy ] alarm ack failed", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "accepted", false,
                "message", "Failed to connect to ECS: " + e.getMessage(),
                "alarmId", alarmId
            ));
        }
    }

    /**
     * 알람 리셋 요청
     * POST /rest/shuttle/ecs-proxy/{lcId}/alarms/{alarmId}/reset
     */
    @PostMapping("/{lcId}/alarms/{alarmId}/reset")
    public ResponseEntity<Map<String, Object>> resetAlarm(
            @PathVariable String lcId,
            @PathVariable String alarmId,
            @RequestBody(required = false) Map<String, Object> body) {

        logger.info("[ Ecs ][ Proxy ] reset alarm: lcId={}, alarmId={}", lcId, alarmId);

        try {
            String ecsUrl = ecsBaseUrl + "/alarms/" + alarmId + "/reset";
            HttpHeaders headers = createHeaders(lcId);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(ecsUrl, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok(Map.of(
                    "accepted", true,
                    "commandId", generateCommandId(),
                    "message", "Alarm reset request accepted",
                    "alarmId", alarmId,
                    "ecsResponse", response.getBody()
                ));
            } else {
                return ResponseEntity.status(response.getStatusCode()).body(Map.of(
                    "accepted", false,
                    "message", "ECS rejected the alarm reset",
                    "alarmId", alarmId
                ));
            }
        } catch (RestClientException e) {
            logger.error("[ Ecs ][ Proxy ] alarm reset failed", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "accepted", false,
                "message", "Failed to connect to ECS: " + e.getMessage(),
                "alarmId", alarmId
            ));
        }
    }

    // ECS 상태 확인 API

    /**
     * ECS 연결 상태 확인
     * GET /rest/shuttle/ecs-proxy/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkEcsHealth() {
        try {
            String ecsUrl = ecsBaseUrl + "/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(ecsUrl, Map.class);

            return ResponseEntity.ok(Map.of(
                "ecsReachable", true,
                "ecsStatus", response.getStatusCode().value(),
                "ecsUrl", ecsBaseUrl,
                "timestamp", System.currentTimeMillis()
            ));
        } catch (RestClientException e) {
            return ResponseEntity.ok(Map.of(
                "ecsReachable", false,
                "error", e.getMessage(),
                "ecsUrl", ecsBaseUrl,
                "timestamp", System.currentTimeMillis()
            ));
        }
    }

    // 유틸리티 메서드

    /**
     * HTTP 헤더 생성
     */
    private HttpHeaders createHeaders(String lcId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-LC-ID", lcId);
        headers.set("X-Request-Source", "LMS-2D");
        headers.set("X-Request-Time", String.valueOf(System.currentTimeMillis()));
        return headers;
    }

    /**
     * 명령 ID 생성
     */
    private String generateCommandId() {
        return "CMD-" + System.currentTimeMillis() + "-" +
            String.format("%04d", (int) (Math.random() * 10000));
    }
}
