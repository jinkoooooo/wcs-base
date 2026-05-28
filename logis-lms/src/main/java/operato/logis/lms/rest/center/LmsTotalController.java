package operato.logis.lms.rest.center;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import operato.logis.lms.dto.center.LmsAlarmRequestDto;
import operato.logis.lms.dto.center.LmsStatusRequestDto;
import operato.logis.lms.service.impl.center.LmsTotalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/rest/lms")
@RequiredArgsConstructor
@Slf4j
public class LmsTotalController {

    private final LmsTotalService lmsTotalService;

    /**
     * 알람 리스트 수신 (Batch)
     */
    @PostMapping("/alarm")
    public ResponseEntity<String> receiveAlarm(@RequestBody List<LmsAlarmRequestDto> requestData) {

        if (requestData == null || requestData.isEmpty()) {
            return ResponseEntity.badRequest().body("FAIL: Data list is empty");
        }

        try {
            // 서비스의 리스트 처리 메서드 호출
            lmsTotalService.saveAlarmList(requestData);
            return ResponseEntity.ok("SUCCESS: Processed " + requestData.size() + " items");
        } catch (Exception e) {
            log.error("알람 리스트 수신 중 에러", e);
            return ResponseEntity.internalServerError().body("ERROR: " + e.getMessage());
        }
    }

    /**
     * 상태 리스트 수신 (Batch)
     */
    @PostMapping("/status")
    public ResponseEntity<String> receiveStatus(@RequestBody List<LmsStatusRequestDto> requestData) {

        if (requestData == null || requestData.isEmpty()) {
            return ResponseEntity.badRequest().body("FAIL: Data list is empty");
        }

        try {
            // 서비스의 리스트 처리 메서드 호출
            lmsTotalService.saveStatusList(requestData);
            return ResponseEntity.ok("SUCCESS: Processed " + requestData.size() + " items");
        } catch (Exception e) {
            log.error("상태 리스트 수신 중 에러", e);
            return ResponseEntity.internalServerError().body("ERROR: " + e.getMessage());
        }
    }
}