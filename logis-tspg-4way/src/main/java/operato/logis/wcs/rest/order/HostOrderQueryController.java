package operato.logis.wcs.rest.order;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.HostOrderStatus;
import operato.logis.wcs.consts.WcsError;
import operato.logis.wcs.entity.TbWcsHostOrder;
import operato.logis.wcs.entity.TbWcsHostOrderHistory;
import operato.logis.wcs.entity.TbWcsHostOrderItem;
import operato.logis.wcs.service.impl.external.ExternalOrderNotifier;
import operato.logis.wcs.service.impl.order.host.HostOrderAuditLogger;
import operato.logis.wcs.service.impl.order.host.HostOrderEvents;
import operato.logis.wcs.service.impl.order.host.HostOrderStateWriter;
import operato.logis.wcs.service.repository.HostOrderHistoryRepository;
import operato.logis.wcs.service.repository.HostOrderItemRepository;
import operato.logis.wcs.service.repository.HostOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@RestController
@RequiredArgsConstructor
@RequestMapping("/rest/wcs/host-order")
public class HostOrderQueryController extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(HostOrderQueryController.class);

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;

    private static final String BASE_TABLE = " FROM tb_wcs_host_order WHERE 1=1 ";
    private static final String ORDER_BY = " ORDER BY received_at DESC ";

    /** 취소 허용 상태. */
    private static final Set<Integer> CANCELLABLE = Set.of(
            HostOrderStatus.RECEIVED.code(),
            HostOrderStatus.WAITING_SCHEDULE.code(),
            HostOrderStatus.READY_FOR_ALLOC.code(),
            HostOrderStatus.VALIDATED.code()
    );

    private final HostOrderRepository hostOrderRepository;
    private final HostOrderItemRepository hostOrderItemRepository;
    private final HostOrderHistoryRepository hostOrderHistoryRepository;
    private final ExternalOrderNotifier externalNotifier;
    private final HostOrderAuditLogger historyLogger;
    private final HostOrderStateWriter hostOrderStateWriter;

    // host_order 검색 - 동적 필터 + 페이징
    @GetMapping
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam(required = false) String eqGroupId,
            @RequestParam(required = false) String hostSystemCode,
            @RequestParam(required = false) String orderType,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {

        page = Math.max(1, page);
        size = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(MAX_PAGE_SIZE, size);
        int offset = (page - 1) * size;

        StringBuilder where = new StringBuilder();
        Map<String, Object> params = new LinkedHashMap<>();
        if (StringUtils.hasText(eqGroupId))      { where.append(" AND eq_group_id = :eqGroupId ");           params.put("eqGroupId", eqGroupId); }
        if (StringUtils.hasText(hostSystemCode)) { where.append(" AND host_system_code = :hostSystemCode "); params.put("hostSystemCode", hostSystemCode); }
        if (StringUtils.hasText(orderType))      { where.append(" AND order_type = :orderType ");            params.put("orderType", orderType); }
        if (StringUtils.hasText(ownerCode))      { where.append(" AND owner_code = :ownerCode ");            params.put("ownerCode", ownerCode); }
        List<Integer> statuses = parseStatuses(status);
        if (!ValueUtil.isEmpty(statuses))        { where.append(" AND order_status IN (:statuses) ");        params.put("statuses", statuses); }
        if (ValueUtil.isNotEmpty(from))           { where.append(" AND received_at >= :from ");              params.put("from", from); }
        if (ValueUtil.isNotEmpty(to))             { where.append(" AND received_at <= :to ");                params.put("to", to); }
        if (StringUtils.hasText(keyword)) {
            where.append(" AND (host_order_key LIKE :kw OR wcs_order_key LIKE :kw OR barcode LIKE :kw) ");
            params.put("kw", "%" + keyword + "%");
        }

        String dataSql = "SELECT *" + BASE_TABLE + where + ORDER_BY;
        String countSql = "SELECT COUNT(*) AS cnt" + BASE_TABLE + where;

        List<TbWcsHostOrder> rows = this.queryManager.selectListBySql(
                dataSql, params, TbWcsHostOrder.class, offset, size);
        List<Map> cntRows = this.queryManager.selectListBySql(
                countSql, params, Map.class, 0, 1);
        long total = ValueUtil.isEmpty(cntRows) ? 0 : ((Number) cntRows.get(0).get("cnt")).longValue();

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("page", page);
        resp.put("size", size);
        resp.put("total", total);
        resp.put("items", rows);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{hostSystemCode}/{hostOrderKey}")
    public ResponseEntity<Map<String, Object>> detail(@PathVariable String hostSystemCode,
                                                       @PathVariable String hostOrderKey) {
        TbWcsHostOrder h = findOrderOrThrow(hostSystemCode, hostOrderKey);
        List<TbWcsHostOrderItem> items = hostOrderItemRepository.findByHostOrderKey(
                h.getHostSystemCode(), h.getHostOrderKey());
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("order", h);
        resp.put("items", items);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/{hostSystemCode}/{hostOrderKey}/cancel")
    public ResponseEntity<TbWcsHostOrder> cancel(@PathVariable String hostSystemCode,
                                                  @PathVariable String hostOrderKey,
                                                  @RequestBody CancelRequest req) {
        String operator = req == null ? null : req.getOperator();
        String reason = req == null ? null : req.getReason();
        logger.info("[ Order ][ Host ] cancel - hostOrderKey={}, operator={}, reason={}", hostOrderKey, operator, reason);

        TbWcsHostOrder h = findOrderOrThrow(hostSystemCode, hostOrderKey);
        int current = h.getOrderStatus();
        if (!CANCELLABLE.contains(current)) {
            throw new ElidomRuntimeException(WcsError.INVALID_PARAMETER.codeAsString(),
                    "취소 불가 상태: current=" + current);
        }
        String prev = String.valueOf(current);
        hostOrderStateWriter.markCancelled(h, reason);
        externalNotifier.notifyOrderStatusChanged(h, prev);
        historyLogger.log(h, current, HostOrderEvents.CANCELLED, operator, reason);
        return ResponseEntity.ok(h);
    }

    @GetMapping("/{hostSystemCode}/{hostOrderKey}/history")
    public ResponseEntity<List<TbWcsHostOrderHistory>> history(@PathVariable String hostSystemCode,
                                                                @PathVariable String hostOrderKey) {
        return ResponseEntity.ok(hostOrderHistoryRepository.findByOrderKey(hostSystemCode, hostOrderKey));
    }

    // 내부 헬퍼
    private TbWcsHostOrder findOrderOrThrow(String hostSystemCode, String hostOrderKey) {
        TbWcsHostOrder h = StringUtils.hasText(hostSystemCode)
                ? hostOrderRepository.findByHostOrderKey(hostSystemCode, hostOrderKey)
                : hostOrderRepository.findByHostOrderKey(hostOrderKey);
        if (ValueUtil.isEmpty(h)) {
            throw new ElidomRuntimeException(WcsError.ORDER_NOT_FOUND.codeAsString(),
                    "host_order not found: " + hostOrderKey);
        }
        return h;
    }

    private static List<Integer> parseStatuses(String csv) {
        if (!StringUtils.hasText(csv)) return null;
        return Arrays.stream(csv.split(","))
                .map(String::trim).filter(ValueUtil::isNotEmpty)
                .map(Integer::parseInt).collect(Collectors.toList());
    }

    public static class CancelRequest {
        private String operator;
        private String reason;
        public String getOperator() { return operator; }
        public void setOperator(String operator) { this.operator = operator; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
