package operato.logis.connector.sineva.rest;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.util.ValueUtil;

import java.util.Map;

@RestController
@RequestMapping("/rest/ecs")
public class EcsMockController {

    private static final Map<String, Object> SUCCESS_RESPONSE = ValueUtil.newMap("code,msg","200","successful");

    @PostMapping("/createTask")
    public ResponseEntity<?> createTask(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(SUCCESS_RESPONSE);
    }

    @PostMapping("/cancelTask")
    public ResponseEntity<?> cancelTask(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(SUCCESS_RESPONSE);
    }

    @PostMapping("/setTaskPriority")
    public ResponseEntity<?> setTaskPriority(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(SUCCESS_RESPONSE);
    }

    @PostMapping("/releaseCode")
    public ResponseEntity<?> releaseCode(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(SUCCESS_RESPONSE);
    }

    @PostMapping("/dispatchable")
    public ResponseEntity<?> setDispatchable(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(SUCCESS_RESPONSE);
    }

    @PostMapping("/skipPoint")
    public ResponseEntity<?> skipPoint(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(SUCCESS_RESPONSE);
    }

    @GetMapping("/robotsStatus")
    public ResponseEntity<?> getAgvStatus() {
        return ResponseEntity.ok(SUCCESS_RESPONSE);
    }
}