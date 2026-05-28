package operato.logis.ecs.tspg4way.dashboard.rest;

import java.util.Map;

import operato.logis.ecs.tspg4way.dashboard.service.impl.VirtualSimulationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 * 가상 운영 시뮬레이션 REST API
 */
@RestController
@RequestMapping(value = "/rest/simulation", produces = MediaType.APPLICATION_JSON_VALUE)
public class VirtualSimulationController {

    @Autowired
    private VirtualSimulationService simulationService;

    /**
     * 가상 설비 데이터 생성
     */
    @PostMapping("/seed")
    public ResponseEntity<Map<String, Object>> generateVirtualEquipment(
            @RequestBody Map<String, Object> params) {

        String lcId = (String) params.get("lcId");
        String eqGroupId = (String) params.get("eqGroupId");
        int floors = params.get("floors") != null ? ((Number) params.get("floors")).intValue() : 3;
        int shuttlesPerFloor = params.get("shuttlesPerFloor") != null ? ((Number) params.get("shuttlesPerFloor")).intValue() : 2;
        int rackRows = params.get("rackRows") != null ? ((Number) params.get("rackRows")).intValue() : 20;
        int rackCols = params.get("rackCols") != null ? ((Number) params.get("rackCols")).intValue() : 20;

        Map<String, Object> result = simulationService.generateVirtualEquipment(
                lcId, eqGroupId, floors, shuttlesPerFloor, rackRows, rackCols
        );
        return ResponseEntity.ok(result);
    }

    /**
     * Layout과 가상 설비 자동 매핑
     */
    @PostMapping("/auto-map")
    public ResponseEntity<Map<String, Object>> autoMapLayouts(
            @RequestBody Map<String, Object> params) {

        String lcId = (String) params.get("lcId");
        String pageId = (String) params.get("pageId");

        Map<String, Object> result = simulationService.autoMapLayoutsToVirtualEquipment(lcId, pageId);
        return ResponseEntity.ok(result);
    }

    /**
     * 가상 데이터 삭제
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearVirtualData(
            @RequestParam String eqGroupId) {

        simulationService.clearVirtualData(eqGroupId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Virtual data cleared"));
    }

    /**
     * 시뮬레이션 시작
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startSimulation(
            @RequestBody Map<String, Object> params) {

        String lcId = (String) params.get("lcId");
        String eqGroupId = (String) params.get("eqGroupId");

        simulationService.startSimulation(lcId, eqGroupId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Simulation started"));
    }

    /**
     * 시뮬레이션 중지
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopSimulation() {
        simulationService.stopSimulation();
        return ResponseEntity.ok(Map.of("success", true, "message", "Simulation stopped"));
    }

    /**
     * 시뮬레이션 상태 조회
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSimulationStatus() {
        return ResponseEntity.ok(simulationService.getSimulationStatus());
    }

    /**
     * WebSocket 테스트 - 수동으로 메시지 전송
     * 프론트엔드 WebSocket 구독이 제대로 동작하는지 확인용
     */
    @PostMapping("/test-broadcast")
    public ResponseEntity<Map<String, Object>> testBroadcast(
            @RequestBody Map<String, Object> params) {

        String lcId = (String) params.get("lcId");
        if (lcId == null || lcId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "lcId is required"
            ));
        }

        simulationService.testBroadcast(lcId);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Test broadcast sent to /topic/shuttle/positions/" + lcId
        ));
    }

    /**
     * 전체 초기화 및 시뮬레이션 시작 (원클릭)
     */
    @PostMapping("/init-and-start")
    public ResponseEntity<Map<String, Object>> initAndStart(
            @RequestBody Map<String, Object> params) {

        String lcId = (String) params.get("lcId");
        String eqGroupId = (String) params.get("eqGroupId");
        String pageId = (String) params.get("pageId");
        int floors = params.get("floors") != null ? ((Number) params.get("floors")).intValue() : 3;
        int shuttlesPerFloor = params.get("shuttlesPerFloor") != null ? ((Number) params.get("shuttlesPerFloor")).intValue() : 2;
        int rackRows = params.get("rackRows") != null ? ((Number) params.get("rackRows")).intValue() : 20;
        int rackCols = params.get("rackCols") != null ? ((Number) params.get("rackCols")).intValue() : 20;

        // 1. 가상 설비 생성
        Map<String, Object> seedResult = simulationService.generateVirtualEquipment(
                lcId, eqGroupId, floors, shuttlesPerFloor, rackRows, rackCols
        );

        // 2. 자동 매핑
        Map<String, Object> mapResult = simulationService.autoMapLayoutsToVirtualEquipment(lcId, pageId);

        // 3. 시뮬레이션 시작
        simulationService.startSimulation(lcId, eqGroupId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "seedResult", seedResult,
                "mapResult", mapResult,
                "message", "Virtual simulation initialized and started"
        ));
    }
}
