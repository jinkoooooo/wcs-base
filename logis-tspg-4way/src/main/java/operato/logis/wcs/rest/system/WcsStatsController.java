package operato.logis.wcs.rest.system;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.HostOrderStatus;
import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.service.impl.order.host.HostOrderAuditLogger;
import operato.logis.wcs.service.impl.order.host.HostOrderEvents;
import operato.logis.wcs.service.impl.system.SystemModeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequiredArgsConstructor
@RequestMapping("/rest/wcs")
public class WcsStatsController extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(WcsStatsController.class);


    private final SystemModeService systemModeService;

    @GetMapping("/ops-status")
    public ResponseEntity<Map<String, Object>> opsStatus(@RequestParam(required = false) String eqGroupId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("mode", systemModeService.getCurrentMode(eqGroupId).code());

        Map<String, Boolean> flags = new LinkedHashMap<>();
        flags.put("isOperationModeEnabled", systemModeService.isOperationModeEnabled(eqGroupId));
        flags.put("isDispatchLockEnabled",  systemModeService.isDispatchLockEnabled(eqGroupId));
        flags.put("isInspectionEnabled",    systemModeService.isInspectionEnabled(eqGroupId));
        body.put("flags", flags);

        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("waitingSchedule",  countHostByStatus(eqGroupId, HostOrderStatus.WAITING_SCHEDULE.code()));
        counts.put("readyForAlloc",    countHostByStatus(eqGroupId, HostOrderStatus.READY_FOR_ALLOC.code()));
        counts.put("executing",        countHostByStatus(eqGroupId, HostOrderStatus.EXECUTING.code()));
        counts.put("putbackActive",    countPutbackActive(eqGroupId));
        counts.put("portLocked",       countPortLocked(eqGroupId));
        body.put("counts", counts);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats(
            @RequestParam(required = false) String eqGroupId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {

        long totalOutbound = countHostByType(eqGroupId, OrderType.OUTBOUND.code(), from, to);
        long totalInbound  = countHostByType(eqGroupId, OrderType.INBOUND.code(),  from, to);

        long inspectionFailedCount = countHistoryByEvent(eqGroupId, HostOrderEvents.TEST_FAILED.code(), from, to);
        long inspectionPassedCount = countHistoryByEvent(eqGroupId, HostOrderEvents.TEST_PASSED.code(), from, to);
        long inspectionTotal = inspectionFailedCount + inspectionPassedCount;
        double inspectionPassRate = inspectionTotal == 0 ? 1.0
                : (double) inspectionPassedCount / (double) inspectionTotal;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("totalOutbound",         totalOutbound);
        body.put("totalInbound",          totalInbound);
        body.put("inspectionPassRate",    round2(inspectionPassRate));
        body.put("inspectionFailedCount", inspectionFailedCount);
        return ResponseEntity.ok(body);
    }

    // 내부 집계 쿼리 헬퍼

    private long countHostByStatus(String eqGroupId, int status) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) AS cnt FROM tb_wcs_host_order WHERE order_status = :s ");
        Map<String, Object> p = ValueUtil.newMap("s", status);
        applyEqGroup(sql, p, eqGroupId);
        return selectCount(sql, p);
    }

    private long countPutbackActive(String eqGroupId) {
        // 재입고 INBOUND shuttle (parent_order_key NOT NULL, NOT COMPLETED) 건수
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*) AS cnt FROM tb_wcs_shuttle_order
                 WHERE order_type = :t AND parent_order_key IS NOT NULL
                   AND order_status < :completed
                """);
        Map<String, Object> p = ValueUtil.newMap("t,completed",
                OrderType.INBOUND.code(),
                operato.logis.wcs.consts.ShuttleOrderStatus.COMPLETED.code());
        applyEqGroup(sql, p, eqGroupId);
        return selectCount(sql, p);
    }

    private long countPortLocked(String eqGroupId) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*) AS cnt FROM tb_inventory_location
                 WHERE loc_type IN ('INBOUND_PORT','OUTBOUND_PORT','IN_OUTBOUND_PORT')
                   AND task_id IS NOT NULL AND task_id <> ''
                """);
        Map<String, Object> p = new LinkedHashMap<>();
        if (StringUtils.hasText(eqGroupId)) {
            sql.append(" AND loc_group = :eg ");
            p.put("eg", eqGroupId);
        }
        return selectCount(sql, p);
    }

    private long countHostByType(String eqGroupId, String orderType,
                                  OffsetDateTime from, OffsetDateTime to) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) AS cnt FROM tb_wcs_host_order WHERE order_type = :t ");
        Map<String, Object> p = ValueUtil.newMap("t", orderType);
        applyEqGroup(sql, p, eqGroupId);
        applyRange(sql, p, "received_at", from, to);
        return selectCount(sql, p);
    }

    private long countHistoryByEvent(String eqGroupId, String eventType,
                                      OffsetDateTime from, OffsetDateTime to) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) AS cnt FROM tb_wcs_host_order_history WHERE event_type = :ev ");
        Map<String, Object> p = ValueUtil.newMap("ev", eventType);
        applyEqGroup(sql, p, eqGroupId);
        applyRange(sql, p, "created_at", from, to);
        return selectCount(sql, p);
    }

    private void applyEqGroup(StringBuilder sql, Map<String, Object> p, String eqGroupId) {
        if (StringUtils.hasText(eqGroupId)) {
            sql.append(" AND eq_group_id = :eg ");
            p.put("eg", eqGroupId);
        }
    }

    private void applyRange(StringBuilder sql, Map<String, Object> p, String column,
                             OffsetDateTime from, OffsetDateTime to) {
        if (ValueUtil.isNotEmpty(from)) { sql.append(" AND ").append(column).append(" >= :from "); p.put("from", from); }
        if (ValueUtil.isNotEmpty(to))   { sql.append(" AND ").append(column).append(" <= :to ");   p.put("to",   to);   }
    }

    private long selectCount(StringBuilder sql, Map<String, Object> params) {
        String s = sql.toString();
        try {
            List<Map> rows = this.queryManager.selectListBySql(s, params, Map.class, 0, 1);
            if (ValueUtil.isEmpty(rows)) return 0;
            Object cnt = rows.get(0).get("cnt");
            return ValueUtil.isEmpty(cnt) ? 0 : ((Number) cnt).longValue();
        } catch (Exception e) {
            logger.error("[ Stats ] selectCount failed - sql={}", s, e);
            return 0;
        }
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
