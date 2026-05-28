package operato.logis.wcs.rest.system;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.PortMode;
import operato.logis.wcs.facade.Tspg4WayShuttleWcsFacade;
import operato.logis.wcs.service.impl.allocation.port.PortService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.elidom.util.ValueUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * [wcs-ops Step 13] 포트 모드 전환 + 포트/락 목록 + 관리자 강제 락 해제 REST.
 * ※ 경고: forceUnlock 은 플래그와 무관하게 tb_inventory_location.task_id 를 NULL 로 초기화한다.
 *    운영 실수로 진행 중 PUTBACK/OUTBOUND 가 존재하는 포트에 호출 시 데이터 정합성 이슈 발생 가능.
 *    반드시 현장 상태 확인 후 사용할 것.
 */
@RestController
@RequiredArgsConstructor
public class PortAdminController {

    private static final Logger logger = LoggerFactory.getLogger(PortAdminController.class);

    private static final String FORCE_UNLOCK_WARNING =
            "본 API 는 플래그 무관 강제 해제. 진행 중 작업 유무를 반드시 확인할 것.";

    private final PortService portService;
    private final Tspg4WayShuttleWcsFacade facade;

    @PutMapping("/rest/wcs/port/{portCode}/mode")
    public ResponseEntity<Map<String, Object>> changeMode(@PathVariable String portCode,
                                                          @RequestBody ChangeModeRequest req) {
        logger.warn("[ Allocation ][ Port ] changeMode - portCode={}, eqGroupId={}, mode={}, operator={}, reason={}",
                portCode, req.getEqGroupId(), req.getPortMode(), req.getOperator(), req.getReason());

        PortMode mode = PortMode.from(req.getPortMode());
        if (ValueUtil.isEmpty(mode)) {
            return ResponseEntity.badRequest().body(errorBody(
                    "ERR_INVALID_PARAM", "invalid portMode: " + req.getPortMode()));
        }

        PortService.ChangeResult r = facade.changePortMode(
                req.getEqGroupId(), portCode, mode, req.getOperator(), req.getReason());

        if (!r.success()) {
            return ResponseEntity.badRequest().body(errorBody(r.errorCode(), r.errorDesc()));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("previousMode", r.previousMode());
        body.put("currentMode", r.currentMode());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/rest/wcs/port")
    public ResponseEntity<List<Map<String, Object>>> listPorts(
            @RequestParam(required = false) String eqGroupId,
            @RequestParam(required = false) String locType,
            @RequestParam(required = false) String portMode,
            @RequestParam(required = false) Boolean locked,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(portService.listPorts(eqGroupId, locType, portMode, locked, keyword));
    }

    @GetMapping("/rest/wcs/admin/port-locks")
    public ResponseEntity<List<Map<String, Object>>> listLocks(@RequestParam(required = false) String eqGroupId) {
        return ResponseEntity.ok(portService.listLocks(eqGroupId));
    }

    @PostMapping("/rest/wcs/admin/port/{portCode}/unlock")
    public ResponseEntity<Map<String, Object>> forceUnlock(@PathVariable String portCode,
                                                           @RequestBody UnlockRequest req) {
        logger.warn("[ Allocation ][ Port ] forceUnlock - portCode={}, eqGroupId={}, operator={}, reason={}",
                portCode, req.getEqGroupId(), req.getOperator(), req.getReason());
        facade.forceUnlockPort(req.getEqGroupId(), portCode, req.getOperator(), req.getReason());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "OK");
        body.put("eqGroupId", req.getEqGroupId());
        body.put("portCode", portCode);
        body.put("warning", FORCE_UNLOCK_WARNING);
        return ResponseEntity.ok(body);
    }

    private static Map<String, Object> errorBody(String errorCode, String errorDesc) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("errorCode", errorCode);
        body.put("errorDesc", errorDesc);
        return body;
    }

    @Data
    public static class ChangeModeRequest {
        @JsonProperty("eqGroupId")
        private String eqGroupId;

        @JsonProperty("portMode")
        private String portMode;

        @JsonProperty("operator")
        private String operator;

        @JsonProperty("reason")
        private String reason;
    }

    @Data
    public static class UnlockRequest {
        @JsonProperty("eqGroupId")
        private String eqGroupId;

        @JsonProperty("operator")
        private String operator;

        @JsonProperty("reason")
        private String reason;
    }
}
