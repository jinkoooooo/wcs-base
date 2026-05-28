package operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.simulation;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 실운영 기반 가상 시나리오 시뮬레이션 REST API
 *
 * 사용법:
 * 1. POST /rest/sim/seed   - Seed 데이터 생성 (기존 데이터가 없으면 생성)
 * 2. POST /rest/sim/start  - 시뮬레이션 시작 (기존 운영 데이터 기반)
 * 3. POST /rest/sim/stop   - 시뮬레이션 중지
 * 4. POST /rest/sim/reset  - 시뮬레이션 리셋 (SIM_ 오더만 삭제)
 * 5. GET  /rest/sim/status - 상태 조회
 *
 * 실운영 데이터 구조:
 * - tb_eq_group_mst: K_MAT_TSPG
 * - tb_eq_mst: RACK_1 (type=11), CV_1 (type=21), SHUTTLE_CAR_1/2 (type=22)
 * - tb_eq_rack_mst: 셀 ID 형식 = level(1) + row(02) + bay(02) = "10101"
 * - tb_eq_car_mst: rack_eq_id = "RACK_1", level/row/bay로 위치 추적
 */
@RestController
@RequestMapping(value = "/rest/sim", produces = MediaType.APPLICATION_JSON_VALUE)
public class OperationSimulationController {

    @Autowired
    private OperationSimulationService simulationService;

    /**
     * Seed 데이터 생성/확인
     *
     * 요청:
     * POST /rest/sim/seed
     * {
     *   "eqGroupId": "K_MAT_TSPG",  // 실운영 설비그룹 ID
     *   "floors": 2,                 // 층 수
     *   "shuttlesPerFloor": 1,       // 층별 셔틀 수
     *   "rackRows": 7,               // 랙 행 수
     *   "rackBays": 4                // 랙 열 수
     * }
     *
     * 참고: 기존 데이터가 있으면 유지하고, 없는 것만 생성
     */
    @PostMapping("/seed")
    public ResponseEntity<Map<String, Object>> seed(@RequestBody Map<String, Object> params) {
        String eqGroupId = getString(params, "eqGroupId", "K_MAT_TSPG");
        int floors = getInt(params, "floors", 2);
        int shuttlesPerFloor = getInt(params, "shuttlesPerFloor", 1);
        int rackRows = getInt(params, "rackRows", 7);
        int rackBays = getInt(params, "rackBays", 4);

        Map<String, Object> result = simulationService.seed(
            eqGroupId, floors, shuttlesPerFloor, rackRows, rackBays
        );
        return ResponseEntity.ok(result);
    }

    /**
     * 시뮬레이션 시작
     *
     * POST /rest/sim/start
     * { "eqGroupId": "K_MAT_TSPG" }
     *
     * 기존 운영 데이터(tb_eq_car_mst, tb_eq_rack_mst)를 기반으로
     * 셔틀 이동 및 입출고 작업 시뮬레이션 시작
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> start(@RequestBody Map<String, Object> params) {
        String eqGroupId = getString(params, "eqGroupId", "K_MAT_TSPG");

        Map<String, Object> result = simulationService.start(eqGroupId);
        return ResponseEntity.ok(result);
    }

    /**
     * 시뮬레이션 중지
     *
     * POST /rest/sim/stop
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stop() {
        Map<String, Object> result = simulationService.stop();
        return ResponseEntity.ok(result);
    }

    /**
     * 시뮬레이션 리셋 (SIM_ 오더만 삭제, 설비 데이터는 유지)
     *
     * POST /rest/sim/reset
     * { "eqGroupId": "K_MAT_TSPG" }
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> reset(@RequestBody Map<String, Object> params) {
        String eqGroupId = getString(params, "eqGroupId", "K_MAT_TSPG");

        Map<String, Object> result = simulationService.reset(eqGroupId);
        return ResponseEntity.ok(result);
    }

    /**
     * 상태 조회
     *
     * GET /rest/sim/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> result = simulationService.getStatus();
        return ResponseEntity.ok(result);
    }

    /**
     * Seed + Start 한번에 실행 (없으면 생성 후 시작)
     *
     * POST /rest/sim/init
     */
    @PostMapping("/init")
    public ResponseEntity<Map<String, Object>> init(@RequestBody Map<String, Object> params) {
        String eqGroupId = getString(params, "eqGroupId", "K_MAT_TSPG");
        int floors = getInt(params, "floors", 2);
        int shuttlesPerFloor = getInt(params, "shuttlesPerFloor", 1);
        int rackRows = getInt(params, "rackRows", 7);
        int rackBays = getInt(params, "rackBays", 4);

        // 1. Seed (기존 데이터 확인/생성)
        Map<String, Object> seedResult = simulationService.seed(
            eqGroupId, floors, shuttlesPerFloor, rackRows, rackBays
        );

        // 2. Start
        Map<String, Object> startResult = simulationService.start(eqGroupId);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "seed", seedResult,
            "start", startResult
        ));
    }

    /**
     * 기존 운영 데이터로 바로 시뮬레이션 시작 (Seed 없이)
     *
     * POST /rest/sim/start-existing
     * { "eqGroupId": "K_MAT_TSPG" }
     *
     * 이미 운영 데이터가 있는 경우 사용
     */
    @PostMapping("/start-existing")
    public ResponseEntity<Map<String, Object>> startExisting(@RequestBody Map<String, Object> params) {
        String eqGroupId = getString(params, "eqGroupId", "K_MAT_TSPG");

        Map<String, Object> result = simulationService.start(eqGroupId);
        return ResponseEntity.ok(result);
    }

    // ============================================
    // 유틸리티
    // ============================================

    private String getString(Map<String, Object> params, String key, String defaultValue) {
        Object val = params.get(key);
        return val != null ? val.toString() : defaultValue;
    }

    private int getInt(Map<String, Object> params, String key, int defaultValue) {
        Object val = params.get(key);
        if (val == null) return defaultValue;
        if (val instanceof Number) return ((Number) val).intValue();
        try {
            return Integer.parseInt(val.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
