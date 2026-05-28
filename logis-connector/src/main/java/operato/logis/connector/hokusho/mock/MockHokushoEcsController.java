package operato.logis.connector.hokusho.mock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;


@RestController
@RequestMapping("/rest/mock/hokusho")
public class MockHokushoEcsController {
    /**
     * Logger
     */
    private Logger logger = LoggerFactory.getLogger(MockHokushoEcsController.class);


    /**
     * ✅ [1] CommandTask 요청 시 (WCS → ECS)
     * ECS가 명령 수신했다고 가정하고 결과 반환
     */
    @PostMapping("/commandTask")
    public ResponseEntity<Map<String, Object>> mockCommandTask(@RequestBody Map<String, Object> body) {
        logger.info("[MOCK-ECS] CommandTask 수신 ✅ body={}", body);

        // (옵션) 필수값 검증해서 400으로 돌려주고 싶으면 아래 블록 사용
        // List<String> required = List.of("requestedAt","parcelId","plcSeqNo","lineId","equipId","commandType","commandId","params");
        // List<String> missing = required.stream().filter(k -> !body.containsKey(k) || body.get(k) == null).toList();
        // if (!missing.isEmpty()) {
        //     Map<String,Object> err = new LinkedHashMap<>();
        //     err.put("code", 400);
        //     err.put("msg", "missing/nullable fields: " + missing);
        //     return ResponseEntity.badRequest().body(err);
        // }

        // ✅ null 허용: HashMap/LinkedHashMap 사용
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("echo", body);
        data.put("result", "OK");

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("code", 0);
        res.put("msg", "success");
        res.put("data", data);

        return ResponseEntity.ok(res);
    }

    /**
     * ✅ [2] Performance 콜백 (ECS → WCS)
     * 실제 ECS가 작업 완료 후 실적 보고한다고 가정
     */
    @PostMapping("/performance")
    public ResponseEntity<Map<String, Object>> mockPerformance(@RequestBody Map<String, Object> body) {
        logger.info("[MOCK-ECS] Performance 콜백 수신 ✅ body={}", body);

        Map<String, Object> response = Map.of(
                "code", "000",
                "message", "Performance received successfully",
                "processedAt", LocalDateTime.now().toString()
        );

        return ResponseEntity.ok(response);
    }


    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        return ResponseEntity.ok(Map.of(
                "status", "mock-ecs-alive",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

}