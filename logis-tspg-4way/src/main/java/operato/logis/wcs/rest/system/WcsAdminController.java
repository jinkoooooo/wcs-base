package operato.logis.wcs.rest.system;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.service.impl.allocation.port.PortTrafficService;
import xyz.elidom.util.ValueUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * WCS 운영(O&M) 관리 REST.
 *
 * 물리 장애·비상 상황에서 운영자가 수동으로 WCS 내부 상태를 제어한다.
 * 운영자 권한 이상만 호출 가능하도록 보안 설정 필요.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/rest/admin/wcs")
public class WcsAdminController {

    private static final Logger logger = LoggerFactory.getLogger(WcsAdminController.class);

    private final PortTrafficService portTrafficController;

    /**
     * 포트 장애 우회 (Bypass & Re-routing).
     * 시나리오: 포트 컨베이어/센서 고장으로 사용 불가, 해당 포트 할당 오더 존재.
     * 처리: status DISABLED(90), port_mode IDLE, active_task_count 0, 미완료 오더 to_loc_code NULL + WAITING(25).
     * 복구: 하드웨어 수리 후 tb_inventory_location.is_enabled = true 로 수동 복구.
     */
    @PostMapping("/port/{portCode}/bypass")
    public ResponseEntity<Map<String, Object>> bypassPort(@PathVariable String portCode) {
        logger.warn("[ Allocation ][ Port ] bypass start - portCode={}", portCode);

        if (ValueUtil.isEmpty(portCode) || portCode.isBlank()) {
            return ResponseEntity.badRequest().body(errorBody("PORT_CODE_REQUIRED",
                    "portCode 는 필수 경로 파라미터입니다.", portCode));
        }

        try {
            int reroutedOrderCount = portTrafficController.forceBypassPort(portCode);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success",          true);
            body.put("portCode",         portCode);
            body.put("reroutedOrders",   reroutedOrderCount);
            body.put("message",          String.format(
                    "포트 [%s] 이 DISABLED 처리되었습니다. 재대기(WAITING) 전환 오더: %d 건." +
                    " 해당 오더들은 다음 폴링 사이클에서 정상 포트로 재할당됩니다.",
                    portCode, reroutedOrderCount));
            body.put("timestamp",        Instant.now().toString());
            body.put("recoveryGuide",    "하드웨어 수리 후 tb_inventory_location.is_enabled = true 로 복구하세요.");

            logger.warn("[ Allocation ][ Port ] bypass completed - portCode={}, reroutedOrders={}", portCode, reroutedOrderCount);
            return ResponseEntity.ok(body);

        } catch (IllegalArgumentException e) {
            logger.warn("[ Allocation ][ Port ] bypass rejected - portCode={}, reason={}", portCode, e.getMessage());
            return ResponseEntity.badRequest().body(errorBody("INVALID_REQUEST", e.getMessage(), portCode));

        } catch (Exception e) {
            logger.error("[ Allocation ][ Port ] bypass failed - portCode={}", portCode, e);
            return ResponseEntity.internalServerError()
                    .body(errorBody("INTERNAL_ERROR", "서버 내부 오류: " + e.getMessage(), portCode));
        }
    }


    private Map<String, Object> errorBody(String errorCode, String message, String portCode) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success",   false);
        body.put("portCode",  portCode);
        body.put("errorCode", errorCode);
        body.put("message",   message);
        body.put("timestamp", Instant.now().toString());
        return body;
    }
}
