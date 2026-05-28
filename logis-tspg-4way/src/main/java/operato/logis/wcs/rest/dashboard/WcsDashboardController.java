package operato.logis.wcs.rest.dashboard;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.service.impl.system.SystemModeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import operato.logis.wcs.consts.EnumCode;
import operato.logis.wcs.consts.HostOrderStatus;
import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.consts.ShuttleOrderStatus;
import operato.logis.wcs.consts.LocType;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

/**
 * TSPG 4-way WCS 통합 대시보드 데이터 API.
 * 실 운영 데이터(셔틀·호스트 주문, 시스템 모드)를 한눈에 볼 수 있도록 집계.
 * 모든 상태 코드는 operato.logis.wcs.consts enum 에서 가져온다 (raw 정수 금지).
 * Endpoints: GET /summary, GET /throughput.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/rest/wcs/dashboard")
public class WcsDashboardController extends AbstractQueryService {

    /** 단일 센터 가정. 다센터 확장 시 query param 으로 분리. */
    private static final String SYSTEM_MODE_ID = "GLOBAL";

    private final SystemModeService systemModeService;

    // 상태 집합 — enum 코드를 한 번 펼쳐서 SQL IN 절용 CSV 로 보관 (raw 숫자 금지)
    /** ShuttleOrderStatus 종료 상태 (셔틀 진행 아님): COMPLETED, CANCELLED, ABORTED */
    private static final String SHUTTLE_TERMINAL_CSV = csv(
            ShuttleOrderStatus.COMPLETED,
            ShuttleOrderStatus.CANCELLED,
            ShuttleOrderStatus.ABORTED);

    /** ShuttleOrderStatus 정상 완료 단일 코드 */
    private static final int SHUTTLE_COMPLETED_CODE = ShuttleOrderStatus.COMPLETED.codeAsIntOrNull();

    /** HostOrderStatus — 검증 통과 이상에 도달한 정상 흐름 (terminal 오류 제외) */
    private static final String HOST_VALIDATED_OR_PAST_CSV = csv(
            HostOrderStatus.VALIDATED,
            HostOrderStatus.READY_FOR_ALLOC,
            HostOrderStatus.WAITING_EXEC,
            HostOrderStatus.EXECUTING,
            HostOrderStatus.COMPLETED);

    /**
     * HostOrderStatus — 할당(=실행 대기) 이상에 도달한 정상 흐름.
     * ALLOCATED(20) enum 제거 후 READY_FOR_ALLOC → WAITING_EXEC 직결이므로
     * "할당 단계 도달" 은 WAITING_EXEC 부터.
     */
    private static final String HOST_ALLOCATED_OR_PAST_CSV = csv(
            HostOrderStatus.WAITING_EXEC,
            HostOrderStatus.EXECUTING,
            HostOrderStatus.COMPLETED);

    /** HostOrderStatus — 실행 이상에 도달한 정상 흐름 */
    private static final String HOST_EXECUTING_OR_PAST_CSV = csv(
            HostOrderStatus.EXECUTING,
            HostOrderStatus.COMPLETED);

    /** HostOrderStatus 완료 단일 코드 */
    private static final int HOST_COMPLETED_CODE = HostOrderStatus.COMPLETED.codeAsIntOrNull();

    /** HostOrderStatus — 터미널 오류/거절/취소 */
    private static final String HOST_ERROR_TERMINAL_CSV = csv(
            HostOrderStatus.CANCELLED,
            HostOrderStatus.REJECTED,
            HostOrderStatus.ERROR);

    // 그룹 목록 신규 엔드포인트
    @GetMapping("/eq-groups")
    public ResponseEntity<List<Map<String, Object>>> eqGroups(
            @RequestParam(defaultValue = "1") String lcId) {
        String sql = """
            SELECT id, name
              FROM tb_eq_group_mst
             WHERE lc_id = :lcId
             ORDER BY id
            """;
        Map<String, Object> p = ValueUtil.newMap("lcId", lcId);
        @SuppressWarnings("rawtypes")
        List<Map> rows = queryManager.selectListBySql(sql, p, Map.class, 0, 100);
        @SuppressWarnings({"rawtypes", "unchecked"})
        List<Map<String, Object>> result = (List) rows;
        return ResponseEntity.ok(result);
    }

    // 통합 요약 (운영모드 + KPI + 파이프라인)
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> summary(@RequestParam String eqGroupId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("eqGroupId", eqGroupId);
        body.put("operation", readOperationMode(eqGroupId));
        body.put("inProgressByType", countShuttleByType(true, false, eqGroupId));
        body.put("todayByType",      countShuttleByType(false, true, eqGroupId));
        body.put("hostPipeline", readHostPipeline(eqGroupId));
        body.put("rack", readRackSummary(eqGroupId));
        return ResponseEntity.ok(body);
    }

    private Map<String, Object> readOperationMode(String eqGroupId) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("mode",                   systemModeService.getCurrentMode(eqGroupId).code());
        out.put("isOperationModeEnabled", systemModeService.isOperationModeEnabled(eqGroupId));
        out.put("isDispatchLockEnabled",  systemModeService.isDispatchLockEnabled(eqGroupId));
        out.put("isInspectionEnabled",    systemModeService.isInspectionEnabled(eqGroupId));
        return out;
    }

    /**
     * 셔틀 주문을 order_type 별로 카운트.
     * @param onlyInProgress true 면 종료(COMPLETED/CANCELLED/ABORTED) 제외 (현재 진행)
     * @param todayOnly      true 면 오늘 created 만
     */
    @SuppressWarnings({"rawtypes"})
    private Map<String, Integer> countShuttleByType(boolean onlyInProgress, boolean todayOnly, String eqGroupId) {
        StringBuilder sql = new StringBuilder()
                .append("SELECT order_type, COUNT(*) AS cnt ")
                .append("  FROM tb_wcs_shuttle_order ")
                .append(" WHERE eq_group_id = :eqGroupId ");
        if (onlyInProgress) {
            sql.append("   AND order_status NOT IN (").append(SHUTTLE_TERMINAL_CSV).append(") ");
        }
        if (todayOnly) {
            sql.append("   AND created_at >= CURRENT_DATE ");
        }
        sql.append(" GROUP BY order_type");

        Map<String, Object> p = ValueUtil.newMap("eqGroupId", eqGroupId);
        List<Map> rows = queryManager.selectListBySql(sql.toString(), p, Map.class, 0, 0);

        Map<String, Integer> out = new LinkedHashMap<>();
        // OrderType enum 순서대로 0 으로 초기화
        for (OrderType t : OrderType.values()) {
            out.put(t.code().toString(), 0);
        }
        for (Map r : rows) {
            String type = String.valueOf(r.get("order_type"));
            out.put(type, toInt(r.get("cnt")));
        }
        return out;
    }

    /**
     * 호스트 주문 파이프라인 — 누적 funnel (오늘 created 한정).
     * "현재 그 단계 머무는 건수" 가 아니라 "그 단계까지 도달한 누적 건수" 를 센다.
     * facade 동기 호출이라 RECEIVED 머무는 시간이 거의 0. 사용자 직관에 맞춤.
     */
    @SuppressWarnings({"rawtypes"})
    private Map<String, Integer> readHostPipeline(String eqGroupId) {
        String sql = """
                SELECT
                  COUNT(*)                                                              AS received,
                  COUNT(*) FILTER (WHERE order_status IN (%s)) AS validated,
                  COUNT(*) FILTER (WHERE order_status IN (%s)) AS allocated,
                  COUNT(*) FILTER (WHERE order_status IN (%s)) AS executing,
                  COUNT(*) FILTER (WHERE order_status = %d)  AS completed,
                  COUNT(*) FILTER (WHERE order_status IN (%s)) AS error_cnt
                  FROM tb_wcs_host_order
                 WHERE eq_group_id = :eqGroupId
                   AND created_at >= CURRENT_DATE
                """.formatted(HOST_VALIDATED_OR_PAST_CSV, HOST_ALLOCATED_OR_PAST_CSV,
                        HOST_EXECUTING_OR_PAST_CSV, HOST_COMPLETED_CODE, HOST_ERROR_TERMINAL_CSV);
        Map<String, Object> p = ValueUtil.newMap("eqGroupId", eqGroupId);
        List<Map> rows = queryManager.selectListBySql(sql, p, Map.class, 0, 1);

        Map<String, Integer> out = new LinkedHashMap<>();
        out.put("received",  0);
        out.put("validated", 0);
        out.put("allocated", 0);
        out.put("executing", 0);
        out.put("completed", 0);
        out.put("error",     0);
        out.put("total",     0);
        if (ValueUtil.isNotEmpty(rows)) {
            Map r = rows.get(0);
            int received = toInt(r.get("received"));
            out.put("received",  received);
            out.put("validated", toInt(r.get("validated")));
            out.put("allocated", toInt(r.get("allocated")));
            out.put("executing", toInt(r.get("executing")));
            out.put("completed", toInt(r.get("completed")));
            out.put("error",     toInt(r.get("error_cnt")));
            out.put("total",     received);
        }
        return out;
    }

    @SuppressWarnings({"rawtypes"})
    private Map<String, Object> readRackSummary(String eqGroupId) {
        String sql = """
                SELECT
                  COUNT(*) FILTER (WHERE stock_id IS NULL AND task_id IS NULL) AS empty,
                  COUNT(*) FILTER (WHERE stock_id IS NOT NULL)                  AS occupied,
                  COUNT(*) FILTER (WHERE task_id  IS NOT NULL)                  AS reserved,
                  COUNT(*) AS total
                  FROM tb_inventory_location
                 WHERE loc_group = :locGroup AND loc_type = :rackType
                """;
        Map<String, Object> p = ValueUtil.newMap("locGroup,rackType",
                eqGroupId, LocType.RACK.code());
        List<Map> rows = queryManager.selectListBySql(sql, p, Map.class, 0, 1);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("empty",    0);
        out.put("occupied", 0);
        out.put("reserved", 0);
        out.put("total",    0);
        if (ValueUtil.isNotEmpty(rows)) {
            Map r = rows.get(0);
            out.put("empty",    toInt(r.get("empty")));
            out.put("occupied", toInt(r.get("occupied")));
            out.put("reserved", toInt(r.get("reserved")));
            out.put("total",    toInt(r.get("total")));
        }
        return out;
    }

    // 시간대별 처리량 — 최근 N분, 분단위 완료 건수(타입별)
    @GetMapping("/throughput")
    @SuppressWarnings({"rawtypes", "unchecked"})
    public ResponseEntity<List<Map<String, Object>>> throughput(
            @RequestParam String eqGroupId,
            @RequestParam(value = "minutes", required = false, defaultValue = "30") int minutes) {
        if (minutes < 1)   minutes = 1;
        if (minutes > 720) minutes = 720;

        // 분 단위 버킷 생성 + LEFT JOIN 으로 빈 분에도 0. 완료 상태는 ShuttleOrderStatus.COMPLETED 단 하나.
        String sql = """
                WITH buckets AS (
                  SELECT generate_series(
                    date_trunc('minute', NOW()) - (:mins || ' minutes')::interval,
                    date_trunc('minute', NOW()),
                    interval '1 minute'
                  ) AS bucket
                )
                SELECT b.bucket AS ts,
                       COALESCE(SUM(CASE WHEN s.order_type = :tInbound  AND s.parent_order_key IS NULL THEN 1 ELSE 0 END), 0) AS inbound,
                       COALESCE(SUM(CASE WHEN s.order_type = :tOutbound THEN 1 ELSE 0 END), 0) AS outbound,
                       COALESCE(SUM(CASE WHEN s.order_type = :tMove     THEN 1 ELSE 0 END), 0) AS move_cnt,
                       COALESCE(SUM(CASE WHEN s.order_type = :tInbound  AND s.parent_order_key IS NOT NULL THEN 1 ELSE 0 END), 0) AS reinbound
                  FROM buckets b
                  LEFT JOIN tb_wcs_shuttle_order s
                    ON date_trunc('minute', s.updated_at) = b.bucket
                   AND s.eq_group_id = :eqGroupId
                   AND s.order_status = :completedStatus
                 GROUP BY b.bucket
                 ORDER BY b.bucket
                """;

        Map<String, Object> p = ValueUtil.newMap("");
        p.put("mins",            String.valueOf(minutes));
        p.put("eqGroupId",       eqGroupId);
        p.put("completedStatus", SHUTTLE_COMPLETED_CODE);
        p.put("tInbound",  OrderType.INBOUND.code());
        p.put("tOutbound", OrderType.OUTBOUND.code());
        p.put("tMove",     OrderType.MOVE.code());

        List<Map> rows = queryManager.selectListBySql(sql, p, Map.class, 0, 0);

        return ResponseEntity.ok(rows.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ts",       r.get("ts"));
            m.put(OrderType.INBOUND.code().toString(),  toInt(r.get("inbound")));
            m.put(OrderType.OUTBOUND.code().toString(), toInt(r.get("outbound")));
            m.put(OrderType.MOVE.code().toString(),     toInt(r.get("move_cnt")));
            m.put("REINBOUND",                          toInt(r.get("reinbound")));
            return m;
        }).collect(Collectors.toList()));
    }

    // 헬퍼

    // EnumCode 들을 SQL IN 절용 CSV 문자열로 변환. Integer/String enum 모두 안전.
    private static String csv(EnumCode... codes) {
        return Stream.of(codes)
                .map(c -> {
                    Object v = c.code();
                    return (v instanceof Number n)
                            ? String.valueOf(n)
                            : "'" + String.valueOf(v).replace("'", "''") + "'";
                })
                .collect(Collectors.joining(","));
    }

    private static int toInt(Object o) {
        return (o instanceof Number n) ? n.intValue() : 0;
    }
}
