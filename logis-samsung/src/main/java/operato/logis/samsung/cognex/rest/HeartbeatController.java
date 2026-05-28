package operato.logis.samsung.cognex.rest;
import operato.logis.samsung.cognex.core.HeartbeatScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/heartbeat")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class HeartbeatController {

    private final HeartbeatScheduler heartbeatScheduler;

    public HeartbeatController(HeartbeatScheduler heartbeatScheduler) {
        this.heartbeatScheduler = heartbeatScheduler;
    }

    @PostMapping("/start")
    public ResponseEntity<String> startHeartbeat() {
        heartbeatScheduler.startHeartbeat();
        return ResponseEntity.ok("Heartbeat 전송 시작 요청.");
    }

    @PostMapping("/stop")
    public ResponseEntity<String> stopHeartbeat() {
        heartbeatScheduler.stopHeartbeat();
        return ResponseEntity.ok("Heartbeat 전송 중지 요청.");
    }

    @PostMapping("/status")
    public ResponseEntity<String> getHeartbeatStatus() {
        if (heartbeatScheduler.isRunning()) {
            return ResponseEntity.ok("Heartbeat 상태: 실행 중");
        } else {
            return ResponseEntity.ok("Heartbeat 상태: 중지됨");
        }
    }
}