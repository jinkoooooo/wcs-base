package operato.logis.wcs.simulator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import operato.logis.wcs.consts.ShuttleOrderStatus;
import operato.logis.wcs.entity.TbWcsSimulatorState;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

/**
 * 시뮬레이터 REST API — INBOUND ↔ OUTBOUND 순환만 지원.
 */
@RestController
@RequestMapping("/rest/simulator")
@RequiredArgsConstructor
public class SimulatorController extends AbstractQueryService {

    private final HostSimulator host;
    private final EcsPlcSimulator plc;
    private final SimulatorMetrics metrics;
    private final SimulatorStateService stateService;

    /**
     * UI 드롭다운용 eqGroupId 목록.
     */
    @GetMapping("/eq-groups")
    @SuppressWarnings({"rawtypes", "unchecked"})
    public ResponseEntity<List<Map<String, Object>>> eqGroups() {
        String sql = "SELECT id, name FROM tb_eq_group_mst ORDER BY id";
        List<Map> rows = queryManager.selectListBySql(sql, null, Map.class, 0, 100);
        return ResponseEntity.ok((List) rows);
    }

    @PostMapping("/host/start")
    public ResponseEntity<Map<String, Object>> hostStart(@RequestParam String eqGroupId) {
        if (!isValidEqGroup(eqGroupId)) return bad("유효하지 않은 eqGroupId: " + eqGroupId);
        host.start(eqGroupId);
        return ok();
    }

    @PostMapping("/host/stop")
    public ResponseEntity<Map<String, Object>> hostStop(@RequestParam String eqGroupId) {
        host.stop(eqGroupId);
        return ok();
    }

    @PostMapping("/plc/start")
    public ResponseEntity<Map<String, Object>> plcStart(@RequestParam String eqGroupId) {
        if (!isValidEqGroup(eqGroupId)) return bad("유효하지 않은 eqGroupId: " + eqGroupId);
        plc.start(eqGroupId);
        return ok();
    }

    @PostMapping("/plc/stop")
    public ResponseEntity<Map<String, Object>> plcStop(@RequestParam String eqGroupId) {
        plc.stop(eqGroupId);
        return ok();
    }

    @PostMapping("/all/start")
    public ResponseEntity<Map<String, Object>> startAll(@RequestParam String eqGroupId) {
        if (!isValidEqGroup(eqGroupId)) return bad("유효하지 않은 eqGroupId: " + eqGroupId);
        plc.start(eqGroupId);
        host.start(eqGroupId);
        return ok();
    }

    @PostMapping("/all/stop")
    public ResponseEntity<Map<String, Object>> stopAll(@RequestParam String eqGroupId) {
        host.stop(eqGroupId);
        plc.stop(eqGroupId);
        return ok();
    }

    /**
     * eqGroupId 존재 검증.
     */
    @SuppressWarnings("rawtypes")
    private boolean isValidEqGroup(String eqGroupId) {
        if (ValueUtil.isEmpty(eqGroupId)) return false;
        String sql = "SELECT COUNT(*) AS cnt FROM tb_eq_group_mst WHERE id = :eqGroupId";
        Map<String, Object> p = ValueUtil.newMap("eqGroupId", eqGroupId);
        List<Map> rows = queryManager.selectListBySql(sql, p, Map.class, 0, 1);
        return ValueUtil.isNotEmpty(rows) && rows.get(0).get("cnt") instanceof Number n && n.intValue() > 0;
    }

    @PostMapping("/config/host-interval")
    public ResponseEntity<Map<String, Object>> setHostInterval(@RequestParam long ms) {
        if (ms < 200 || ms > 60_000) return bad("ms 는 200~60000 범위");
        SimulatorConfig.HOST_INTERVAL_MS = ms;
        return ok();
    }

    @PostMapping("/config/plc-step-delay")
    public ResponseEntity<Map<String, Object>> setPlcStepDelay(@RequestParam long ms) {
        if (ms < 0 || ms > 10_000) return bad("ms 는 0~10000 범위");
        SimulatorConfig.PLC_STEP_DELAY_MS = ms;
        return ok();
    }

    /**
     * 글로벌 기본 페이즈 카운트 — 그룹별 설정 없을 때 fallback.
     */
    @PostMapping("/config/ratio")
    public ResponseEntity<Map<String, Object>> setRatio(
            @RequestParam int inbound,
            @RequestParam int outbound) {
        if (inbound < 0 || outbound < 0) return bad("음수 비율 불가");
        if (inbound + outbound == 0) return bad("적어도 하나는 양수");
        SimulatorConfig.RATIO_INBOUND  = inbound;
        SimulatorConfig.RATIO_OUTBOUND = outbound;
        return ok();
    }

    /**
     * 그룹별 페이즈 카운트 설정 — DB 저장.
     */
    @PostMapping("/config/phase-count")
    public ResponseEntity<Map<String, Object>> setPhaseCount(
            @RequestParam String eqGroupId,
            @RequestParam(required = false) Integer inbound,
            @RequestParam(required = false) Integer outbound) {

        logger.info("[ Sim ][ Api ] setPhaseCount - eqGroupId={}, inbound={}, outbound={}", eqGroupId, inbound, outbound);

        if (!isValidEqGroup(eqGroupId)) return bad("유효하지 않은 eqGroupId: " + eqGroupId);

        TbWcsSimulatorState s = stateService.loadOrCreate(eqGroupId);
        if (ValueUtil.isNotEmpty(inbound)) s.setTargetInbound(inbound);
        if (ValueUtil.isNotEmpty(outbound)) s.setTargetOutbound(outbound);
        queryManager.update(s);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("result", "ok");
        body.put("eqGroupId", eqGroupId);
        body.put("targetInbound",  s.getTargetInbound());
        body.put("targetOutbound", s.getTargetOutbound());
        return ResponseEntity.ok(body);
    }

    /**
     * 시뮬레이터 전체 상태 — UI 대시보드용.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("enabled", SimulatorConfig.ENABLED);

        // 그룹별 가동 상태
        List<TbWcsSimulatorState> all = stateService.loadAll();
        List<Map<String, Object>> groups = all.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("eqGroupId",        s.getEqGroupId());
            m.put("hostRunning",      Boolean.TRUE.equals(s.getHostRunning()));
            m.put("plcRunning",       Boolean.TRUE.equals(s.getPlcRunning()));
            m.put("portModeRunning",  Boolean.TRUE.equals(s.getPortModeRunning()));
            m.put("currentPhase",     s.getCurrentPhase());
            m.put("phaseStartedAt",   s.getPhaseStartedAt());
            m.put("targetInbound",    s.getTargetInbound());
            m.put("targetOutbound",   s.getTargetOutbound());
            m.put("phaseSubmitted",   host.getPhaseSubmitted(s.getEqGroupId()));
            m.put("phaseCompleted",   host.getPhaseCompleted(s.getEqGroupId()));
            m.put("phaseTarget",      host.getPhaseTarget(s.getEqGroupId()));
            return m;
        }).toList();
        body.put("groups", groups);

        // 글로벌 config 스냅샷
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("ownerCode",      SimulatorConfig.OWNER_CODE);
        config.put("hostSystemCode", SimulatorConfig.HOST_SYSTEM_CODE);
        config.put("hostIntervalMs", SimulatorConfig.HOST_INTERVAL_MS);
        config.put("hostMaxPending", SimulatorConfig.HOST_MAX_PENDING);
        config.put("plcPollMs",      SimulatorConfig.PLC_POLL_MS);
        config.put("plcStepDelayMs", SimulatorConfig.PLC_STEP_DELAY_MS);
        config.put("ratioInbound",   SimulatorConfig.RATIO_INBOUND);
        config.put("ratioOutbound",  SimulatorConfig.RATIO_OUTBOUND);
        body.put("config", config);

        body.put("plcInFlight", plc.inFlightCount());
        body.put("seedDone",    host.isSeedDone());
        body.put("seedSkus", SimulatorConfig.SEED_SKUS.stream()
                .map(s -> Map.of("code", s.code(), "name", s.name()))
                .toList());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> metricsJson() {
        return ResponseEntity.ok(metrics.snapshot());
    }

    @GetMapping("/timeseries")
    public ResponseEntity<List<Map<String, Object>>> timeseries() {
        return ResponseEntity.ok(metrics.timeseries());
    }

    // 진행 중인 셔틀 오더 목록.
    @GetMapping("/orders/in-progress")
    public ResponseEntity<List<Map<String, Object>>> inProgressOrders(@RequestParam String eqGroupId) {
        String sql = """
                SELECT order_key, order_type, order_status, ecs_if_status,
                       from_loc_code, to_loc_code, host_order_key, created_at
                  FROM tb_wcs_shuttle_order
                 WHERE eq_group_id = :eqGroupId
                   AND order_status < :completeStatus
                 ORDER BY created_at DESC
                """;
        Map<String, Object> p = ValueUtil.newMap("eqGroupId,completeStatus",
                eqGroupId, ShuttleOrderStatus.COMPLETED.code());
        return ResponseEntity.ok(selectRows(sql, p, 30));
    }

    /**
     * 최근 호스트 오더 목록 — source=sim 면 시뮬 마커만, eqGroupId 옵셔널.
     */
    @GetMapping("/orders/host-recent")
    public ResponseEntity<List<Map<String, Object>>> hostRecent(
            @RequestParam(value = "source", required = false, defaultValue = "all") String source,
            @RequestParam(value = "eqGroupId", required = false) String eqGroupId) {
        boolean simOnly = "sim".equalsIgnoreCase(source);
        boolean hasGroup = ValueUtil.isNotEmpty(eqGroupId);

        // 조건부 SQL 빌드
        StringBuilder sql = new StringBuilder()
                .append("SELECT host_order_key, host_system_code, order_type, order_status, ")
                .append("       test_required, test_status, test_requested_at, test_resulted_at, test_reason, ")
                .append("       wcs_order_key, error_code, created_at ")
                .append("  FROM tb_wcs_host_order WHERE 1=1 ");
        Map<String, Object> p = ValueUtil.newMap("");
        if (simOnly) {
            sql.append(" AND host_system_code = :hostSystemCode ");
            p.put("hostSystemCode", SimulatorConfig.HOST_SYSTEM_CODE);
        }
        if (hasGroup) {
            sql.append(" AND eq_group_id = :eqGroupId ");
            p.put("eqGroupId", eqGroupId);
        }
        sql.append(" ORDER BY created_at DESC");
        return ResponseEntity.ok(selectRows(sql.toString(), p, 20));
    }

    @GetMapping("/ports")
    public ResponseEntity<List<Map<String, Object>>> ports(@RequestParam String eqGroupId) {
        String sql = """
                SELECT loc_id, port_mode, active_task_count, task_id, stock_id
                  FROM tb_inventory_location
                 WHERE loc_group = :locGroup
                   AND loc_type IN ('IN_OUTBOUND_PORT','INBOUND_PORT','OUTBOUND_PORT')
                 ORDER BY loc_id
                """;
        Map<String, Object> p = ValueUtil.newMap("locGroup", eqGroupId);
        return ResponseEntity.ok(selectRows(sql, p, 50));
    }

    // 랙 점유 요약 — empty/occupied/reserved/total + 시뮬 stock 수.
    @GetMapping("/racks/summary")
    @SuppressWarnings({"rawtypes", "unchecked"})
    public ResponseEntity<Map<String, Object>> rackSummary(@RequestParam String eqGroupId) {
        String sql = """
                SELECT
                  COUNT(*) FILTER (WHERE stock_id IS NULL AND task_id IS NULL) AS empty,
                  COUNT(*) FILTER (WHERE stock_id IS NOT NULL)                  AS occupied,
                  COUNT(*) FILTER (WHERE task_id  IS NOT NULL)                  AS reserved,
                  COUNT(*) AS total
                  FROM tb_inventory_location
                 WHERE loc_group = :locGroup AND loc_type = 'RACK'
                """;
        Map<String, Object> p = ValueUtil.newMap("locGroup", eqGroupId);
        List<Map> rows = queryManager.selectListBySql(sql, p, Map.class, 0, 1);
        Map<String, Object> body = new LinkedHashMap<>();
        if (ValueUtil.isNotEmpty(rows)) body.putAll(rows.get(0));
        body.put("simStocks", countSimStocks());
        return ResponseEntity.ok(body);
    }

    /**
     * 시뮬 데이터 정리 미리보기 — 삭제 대상 건수만 조회.
     */
    @PostMapping("/cleanup/preview")
    @SuppressWarnings({"rawtypes"})
    public ResponseEntity<Map<String, Object>> cleanupPreview() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("hostOrders",       countSimHostOrders());
        body.put("hostOrderItems",   countByJoin("tb_wcs_host_order_item", "ho.host_system_code = :hsc",
                "host_order_key", "tb_wcs_host_order ho"));
        body.put("shuttleOrders",    countSimShuttleOrders());
        body.put("shuttleOrderItems",countByJoin("tb_wcs_shuttle_order_item", "ho.host_system_code = :hsc",
                "order_key",
                "tb_wcs_shuttle_order so JOIN tb_wcs_host_order ho ON so.host_order_key = ho.host_order_key"));
        body.put("simStocks",        countSimStocks());
        return ResponseEntity.ok(body);
    }

    /**
     * 시뮬 데이터 일괄 삭제 — 시뮬 마커(SIM_LOT_PREFIX, OWNER_CODE) 기준.
     */
    @PostMapping("/cleanup/execute")
    @Transactional
    public ResponseEntity<Map<String, Object>> cleanupExecute() {
        Map<String, Object> body = new LinkedHashMap<>();
        // 'SIM' 키 + 로트 prefix + owner 코드
        Map<String, Object> params = ValueUtil.newMap("simKey,lotPrefix,owner_code",
                "SIM%", SimulatorConfig.SIM_LOT_PREFIX + "%", SimulatorConfig.OWNER_CODE);

        // location.task_id null 처리
        int locTaskDelete = queryManager.executeBySql("""
                UPDATE tb_inventory_location SET task_id = NULL
                 WHERE task_id IN (
                   SELECT order_key FROM tb_wcs_shuttle_order
                    WHERE host_order_key LIKE :simKey)
                """, params);
        body.put("locTaskDelete", locTaskDelete);

        // 셔틀 오더 아이템 삭제
        int shuttleItems = queryManager.executeBySql(
                "DELETE FROM tb_wcs_shuttle_order_item WHERE item_code LIKE :simKey", params);
        body.put("shuttleItems", shuttleItems);

        // location.stock_id 매핑 해제 (시뮬 owner 관련)
        int locTaskUnlocked = queryManager.executeBySql("""
                UPDATE tb_inventory_location SET stock_id = NULL
                 WHERE task_id IN (
                   SELECT order_key FROM tb_wcs_shuttle_order
                    WHERE owner_code LIKE :owner_code)
                """, params);
        body.put("locationsUnlinked", locTaskUnlocked);

        // 셔틀 오더 삭제
        int shuttleOrders = queryManager.executeBySql(
                "DELETE FROM tb_wcs_shuttle_order WHERE owner_code LIKE :owner_code", params);
        body.put("shuttleOrders", shuttleOrders);

        // 호스트 오더 아이템 삭제
        int hostItems = queryManager.executeBySql(
                "DELETE FROM tb_wcs_host_order_item WHERE item_code LIKE :simKey", params);
        body.put("hostItems", hostItems);

        // 호스트 오더 삭제
        int hostOrders = queryManager.executeBySql(
                "DELETE FROM tb_wcs_host_order WHERE owner_code LIKE :owner_code", params);
        body.put("hostOrders", hostOrders);

        // 시뮬 재고와 연결된 로케이션 해제
        int locUnlinked = queryManager.executeBySql("""
                UPDATE tb_inventory_location SET stock_id = NULL
                 WHERE stock_id IN (SELECT stock_id FROM tb_inventory_stock WHERE lot_no LIKE :lotPrefix)
                """, params);
        body.put("locationsUnlinked", locUnlinked);

        // 시뮬 재고 삭제
        int simStocks = queryManager.executeBySql(
                "DELETE FROM tb_inventory_stock WHERE lot_no LIKE :lotPrefix", params);
        body.put("simStocks", simStocks);

        return ResponseEntity.ok(body);
    }

    @SuppressWarnings({"rawtypes"})
    private int countSimHostOrders() {
        String sql = "SELECT COUNT(*) AS cnt FROM tb_wcs_host_order WHERE owner_code LIKE :owner_code";
        return readCount(sql, ValueUtil.newMap("owner_code", SimulatorConfig.OWNER_CODE));
    }

    @SuppressWarnings({"rawtypes"})
    private int countSimShuttleOrders() {
        String sql = "SELECT COUNT(*) AS cnt FROM tb_wcs_shuttle_order WHERE owner_code LIKE :owner_code";
        return readCount(sql, ValueUtil.newMap("owner_code", SimulatorConfig.OWNER_CODE));
    }

    @SuppressWarnings({"rawtypes"})
    private int countSimStocks() {
        String sql = "SELECT COUNT(*) AS cnt FROM tb_inventory_stock WHERE lot_no LIKE :lotPrefix";
        return readCount(sql, ValueUtil.newMap("lotPrefix", SimulatorConfig.SIM_LOT_PREFIX + "%"));
    }

    // JOIN 기반 카운트 헬퍼.
    @SuppressWarnings({"rawtypes"})
    private int countByJoin(String subTable, String whereClause, String fkColumn, String baseFromJoin) {
        String sql = """
                SELECT COUNT(*) AS cnt FROM %s
                 WHERE %s IN (SELECT %s FROM %s WHERE %s)
                """.formatted(subTable, fkColumn, fkColumn, baseFromJoin, whereClause);
        return readCount(sql, ValueUtil.newMap("hsc", SimulatorConfig.HOST_SYSTEM_CODE));
    }

    @SuppressWarnings({"rawtypes"})
    private int readCount(String sql, Map<String, Object> params) {
        List<Map> rows = queryManager.selectListBySql(sql, params, Map.class, 0, 1);
        if (ValueUtil.isEmpty(rows)) return 0;
        Object cnt = rows.get(0).get("cnt");
        return (cnt instanceof Number n) ? n.intValue() : 0;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<Map<String, Object>> selectRows(String sql, Map<String, Object> params, int limit) {
        List<Map> rows = queryManager.selectListBySql(sql, params, Map.class, 0, limit);
        return (List) rows;
    }

    private ResponseEntity<Map<String, Object>> ok() {
        return ResponseEntity.ok(Map.of("result", "ok"));
    }

    private ResponseEntity<Map<String, Object>> bad(String msg) {
        return ResponseEntity.badRequest().body(Map.of("result", "fail", "message", msg));
    }
}
