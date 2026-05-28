package operato.logis.wcs.rest.system;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.WcsOperationMode;
import operato.logis.wcs.entity.TbWcsSystemMode;
import operato.logis.wcs.service.impl.system.SystemModeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.util.ValueUtil;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 전역/센터별 운영 모드 + 기능 플래그 관리 REST.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/rest/wcs/system-mode")
public class SystemModeController {

    private static final Logger logger = LoggerFactory.getLogger(SystemModeController.class);

    private final SystemModeService systemModeService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> get(@RequestParam(required = false) String eqGroupId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("eqGroupId", eqGroupId);
        body.put("operationMode", systemModeService.getCurrentMode(eqGroupId).code());
        Map<String, Boolean> flags = new LinkedHashMap<>();
        flags.put("isOperationModeEnabled", systemModeService.isOperationModeEnabled(eqGroupId));
        flags.put("isDispatchLockEnabled",  systemModeService.isDispatchLockEnabled(eqGroupId));
        flags.put("isInspectionEnabled",    systemModeService.isInspectionEnabled(eqGroupId));
        body.put("flags", flags);
        return ResponseEntity.ok(body);
    }

    @PutMapping
    public ResponseEntity<TbWcsSystemMode> changeMode(@RequestBody ChangeModeRequest req) {
        logger.info("[ System ][ Mode ] changeMode - eqGroupId={}, mode={}, operator={}",
                req.getEqGroupId(), req.getOperationMode(), req.getOperator());
        WcsOperationMode mode = WcsOperationMode.from(req.getOperationMode());
        if (ValueUtil.isEmpty(mode)) {
            return ResponseEntity.badRequest().build();
        }
        TbWcsSystemMode row = systemModeService.changeMode(
                req.getEqGroupId(), mode, req.getOperator(), req.getReason());
        return ResponseEntity.ok(row);
    }

    @PutMapping("/flag")
    public ResponseEntity<TbWcsSystemMode> changeFlag(@RequestBody ChangeFlagRequest req) {
        logger.info("[ System ][ Mode ] changeFlag - eqGroupId={}, flag={}, value={}, operator={}",
                req.getEqGroupId(), req.getFlagName(), req.getValue(), req.getOperator());
        TbWcsSystemMode row = systemModeService.updateFeatureFlag(
                req.getEqGroupId(), req.getFlagName(), req.getValue(),
                req.getOperator(), req.getReason());
        return ResponseEntity.ok(row);
    }

    // 요청 DTO (내부 static)
    public static class ChangeModeRequest {
        private String eqGroupId;
        private String operationMode;
        private String operator;
        private String reason;

        public String getEqGroupId() { return eqGroupId; }
        public void setEqGroupId(String eqGroupId) { this.eqGroupId = eqGroupId; }
        public String getOperationMode() { return operationMode; }
        public void setOperationMode(String operationMode) { this.operationMode = operationMode; }
        public String getOperator() { return operator; }
        public void setOperator(String operator) { this.operator = operator; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class ChangeFlagRequest {
        private String eqGroupId;
        private String flagName;
        /** true=ON, false=OFF, null=상속 */
        private Boolean value;
        private String operator;
        private String reason;

        public String getEqGroupId() { return eqGroupId; }
        public void setEqGroupId(String eqGroupId) { this.eqGroupId = eqGroupId; }
        public String getFlagName() { return flagName; }
        public void setFlagName(String flagName) { this.flagName = flagName; }
        public Boolean getValue() { return value; }
        public void setValue(Boolean value) { this.value = value; }
        public String getOperator() { return operator; }
        public void setOperator(String operator) { this.operator = operator; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}