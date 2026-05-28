package operato.logis.kmat_2026.biz.wcs.kmat_2026.rest;

import operato.logis.kmat_2026.biz.wcs.kmat_2026.service.KMat2026WcsFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/rest/kmat2026")
public class KMat2026ScenarioController {

    private static final Logger logger = LoggerFactory.getLogger(KMat2026ScenarioController.class);

    @Autowired
    private KMat2026WcsFacade wcsFacade;

    @PostMapping("/scenario/start")
    public ResponseEntity<Map<String, Object>> startScenario() {
        logger.info("[KMat2026ScenarioController] POST /scenario/start");
        Map<String, Object> res = new HashMap<>();
        try {
            String scenarioId = wcsFacade.startScenario();
            res.put("success", true);
            res.put("scenarioId", scenarioId);
            res.put("message", "시나리오 시작 - 현재 cycle 계획 조회 후 Step1 실행");
            return ResponseEntity.ok(res);
        } catch (IllegalStateException e) {
            res.put("success", false);
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            logger.error("[KMat2026ScenarioController] startScenario 오류", e);
            res.put("success", false);
            res.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    @PostMapping("/scenario/stop")
    public ResponseEntity<Map<String, Object>> stopScenario() {
        Map<String, Object> res = new HashMap<>();
        try {
            wcsFacade.stopScenario();
            res.put("success", true);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    @PostMapping("/scenario/resume")
    public ResponseEntity<Map<String, Object>> resumeScenario() {
        Map<String, Object> res = new HashMap<>();
        try {
            wcsFacade.resumeScenario();
            res.put("success", true);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    @PostMapping("/scenario/reset")
    public ResponseEntity<Map<String, Object>> resetScenario() {
        Map<String, Object> res = new HashMap<>();
        try {
            wcsFacade.resetScenario();
            res.put("success", true);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    @GetMapping("/scenario/status")
    public ResponseEntity<KMat2026WcsFacade.ScenarioStatus> getScenarioStatus() {
        try {
            return ResponseEntity.ok(wcsFacade.getStatus());
        } catch (Exception e) {
            logger.error("[KMat2026ScenarioController] status 오류", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/location/status")
    public ResponseEntity<KMat2026WcsFacade.LocationStatus> getLocationStatus() {
        try {
            return ResponseEntity.ok(wcsFacade.getLocationStatus());
        } catch (Exception e) {
            logger.error("[KMat2026ScenarioController] location status 오류", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}