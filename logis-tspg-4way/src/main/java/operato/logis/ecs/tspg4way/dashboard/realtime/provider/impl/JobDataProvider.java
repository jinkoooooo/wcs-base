package operato.logis.ecs.tspg4way.dashboard.realtime.provider.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import operato.logis.ecs.tspg4way.dashboard.realtime.dto.JobStatusDto;
import operato.logis.ecs.tspg4way.dashboard.realtime.provider.RealTimeDataProvider;
import operato.logis.ecs.tspg4way.dashboard.realtime.provider.RealTimeFetchContext;
import operato.logis.ecs.tspg4way.domain.enums.EcsDBConsts;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ECS 작업 현황 데이터 Provider. WCS 상위 오더 + ECS 랙/구간 오더를 eqGroupId 기준으로 합쳐 작업 목록 제공. 250ms 폴링.
 */
@Service
public class JobDataProvider extends AbstractQueryService implements RealTimeDataProvider<JobStatusDto> {

    private static final Logger logger = LoggerFactory.getLogger(JobDataProvider.class);

    @Override
    public String getProviderType() {
        return "job";
    }

    @Override
    public String getTopicPattern() {
        return "/topic/realtime/job/{lcId}/{eqGroupId}";
    }

    @Override
    public long getIntervalMs() {
        return 250L;
    }

    @Override
    public List<JobStatusDto> fetchData(RealTimeFetchContext ctx) {
        String eqGroupId = ctx.getEqGroupId();
        List<JobStatusDto> result = new ArrayList<>();
        long timestamp = System.currentTimeMillis();

        if (!hasText(eqGroupId)) {
            return result;
        }

        try {
            fetchWcsOrders(result, timestamp, eqGroupId);
            fetchEcsRackOrders(result, timestamp, eqGroupId);
            fetchEcsRouteOrders(result, timestamp, eqGroupId);

            result.sort((a, b) -> {
                int statusCompare = Integer.compare(
                        getStatusSortOrder(a.getStatus()),
                        getStatusSortOrder(b.getStatus())
                );
                if (statusCompare != 0) return statusCompare;

                String aOrderKey = getSortBaseOrderKey(a);
                String bOrderKey = getSortBaseOrderKey(b);

                int orderKeyCompare = compareOrderKeyDesc(aOrderKey, bOrderKey);
                if (orderKeyCompare != 0) return orderKeyCompare;

                Long aCreatedAt = a.getCreatedAt() != null ? a.getCreatedAt() : 0L;
                Long bCreatedAt = b.getCreatedAt() != null ? b.getCreatedAt() : 0L;
                return Long.compare(bCreatedAt, aCreatedAt);
            });

        } catch (Exception e) {
            logger.error("[ Realtime ][ Job ] fetch failed: eqGroupId={}", eqGroupId, e);
        }

        return result;
    }

    /**
     * WCS 상위 오더 조회
     * tb_wcs_shuttle_order.eq_group_id 직접 사용
     */
    private void fetchWcsOrders(List<JobStatusDto> result, long timestamp, String eqGroupId) {
        String sql = """
        SELECT
            w.id,
            w.order_key      AS orderKey,
            w.order_type     AS orderType,
            w.order_status   AS orderStatus,
            w.priority       AS priority,
            w.from_loc_code  AS fromLocId,
            w.to_loc_code    AS toLocId,
            w.barcode        AS barcode,
            w.ecs_if_status  AS ecsIfStatus,
            w.eq_group_id    AS eqGroupId,
            w.created_at     AS createdAt,
            w.updated_at     AS updatedAt
        FROM tb_wcs_shuttle_order w
        WHERE w.eq_group_id = :eqGroupId
          AND w.order_status <> 90
        ORDER BY
            w.order_key DESC,
            w.created_at DESC
        """;

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("eqGroupId", eqGroupId);

            List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 100);

            for (Map row : rows) {
                Integer orderType = parseJobType(getValue(row, "orderType", "ordertype", "order_type"));
                Integer orderStatus = getInteger(row, 0, "orderStatus", "orderstatus", "order_status");

                JobStatusDto dto = JobStatusDto.builder()
                        .jobKey(getString(row, "orderKey", "orderkey", "order_key"))
                        .jobType(orderType)
                        .status(toUiStatusForWcs(orderStatus))
                        .fromLoc(emptyToDash(getString(row, "fromLocId", "fromLocId", "from_loc_code")))
                        .toLoc(emptyToDash(getString(row, "toLocId", "toLocId", "to_loc_code")))
                        .priority(getInteger(row, 99, "priority"))
                        .barcode(getString(row, "barcode"))
                        .eqGroupId(getString(row, "eqGroupId", "eqgroupid", "eq_group_id"))
                        .jobLevel("WCS")
                        .assignedEquipmentId(null)
                        .assignedEquipmentType(null)
                        .plcCmdId(null)
                        .cmdStatus(null)
                        .errorCode(null)
                        .errorMessage(null)
                        .createdAt(getTimestamp(row, "createdAt", "createdat", "created_at"))
                        .ts(timestamp)
                        .build();

                if (dto.getJobKey() != null) {
                    result.add(dto);
                }
            }
        } catch (Exception e) {
            logger.warn("[ Realtime ][ Job ] WCS order fetch failed: eqGroupId={}", eqGroupId, e);
        }
    }

    /**
     * ECS 랙 오더 조회
     * tb_eq_mst.id = rack.eq_id AND tb_eq_mst.eq_group_id = :eqGroupId
     */
    private void fetchEcsRackOrders(List<JobStatusDto> result, long timestamp, String eqGroupId) {
        String sql = """
        SELECT
            rack.id,
            rack.order_key      AS orderKey,
            rack.order_type     AS orderType,
            rack.order_status   AS orderStatus,
            rack.priority       AS priority,
            rack.barcode        AS barcode,
            rack.eq_id          AS eqId,
            rack.eq_type        AS eqType,
            rack.eq_car_id      AS eqCarId,
            rack.plc_cmd_id     AS plcCmdId,
            rack.cmd_status     AS cmdStatus,
            rack.from_loc_code  AS fromLocId,
            rack.from_row       AS fromRow,
            rack.from_bay       AS fromBay,
            rack.to_loc_code    AS toLocId,
            rack.to_row         AS toRow,
            rack.to_bay         AS toBay,
            rack.error_id       AS errorId,
            rack.error_desc     AS errorDesc,
            rack.started_at     AS startedAt,
            rack.finished_at    AS finishedAt,
            rack.created_at     AS createdAt
        FROM tb_ecs_rack_order rack
        INNER JOIN tb_eq_mst em
            ON em.id = rack.eq_id
           AND em.eq_group_id = :eqGroupId
        WHERE rack.order_status <> 9
        ORDER BY
            rack.order_key DESC,
            rack.created_at DESC
        """;

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("eqGroupId", eqGroupId);

            List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 200);

            for (Map row : rows) {
                String parentOrderKey = getString(row, "orderKey", "orderkey", "order_key");
                Integer orderType = parseJobType(getValue(row, "orderType", "ordertype", "order_type"));
                Integer orderStatus = getInteger(row, 0, "orderStatus", "orderstatus", "order_status");

                String fromLoc = buildLocation(
                        getValue(row, "fromLocId", "fromLocId", "from_loc_code"),
                        getValue(row, "fromRow", "fromrow", "from_row"),
                        getValue(row, "fromBay", "frombay", "from_bay")
                );

                String toLoc = buildLocation(
                        getValue(row, "toLocId", "toLocId", "to_loc_code"),
                        getValue(row, "toRow", "torow", "to_row"),
                        getValue(row, "toBay", "tobay", "to_bay")
                );

                String eqCarId = getString(row, "eqCarId", "eqcarid", "eq_car_id");

                JobStatusDto dto = JobStatusDto.builder()
                        .jobKey(parentOrderKey)
                        .parentJobKey(parentOrderKey)
                        .jobType(orderType)
                        .status(toUiStatusForEcs(orderStatus))
                        .fromLoc(emptyToDash(fromLoc))
                        .toLoc(emptyToDash(toLoc))
                        .priority(getInteger(row, 99, "priority"))
                        .barcode(getString(row, "barcode"))
                        .eqGroupId(eqGroupId)
                        .assignedEquipmentId(eqCarId)
                        .assignedEquipmentType("SHUTTLE")
                        .jobLevel("ECS_RACK")
                        .plcCmdId(getString(row, "plcCmdId", "plccmdid", "plc_cmd_id"))
                        .cmdStatus(getInteger(row, null, "cmdStatus", "cmdstatus", "cmd_status"))
                        .errorCode(getString(row, "errorId", "errorid", "error_id"))
                        .errorMessage(getString(row, "errorDesc", "errordesc", "error_desc"))
                        .createdAt(getTimestamp(row, "createdAt", "createdat", "created_at"))
                        .ts(timestamp)
                        .build();

                if (dto.getJobKey() != null) {
                    result.add(dto);
                }
            }
        } catch (Exception e) {
            logger.warn("[ Realtime ][ Job ] rack order fetch failed: eqGroupId={}", eqGroupId, e);
        }
    }

    /**
     * ECS Route 오더 조회
     * tb_eq_mst.id = route.eq_id AND tb_eq_mst.eq_group_id = :eqGroupId
     */
    private void fetchEcsRouteOrders(List<JobStatusDto> result, long timestamp, String eqGroupId) {
        String sql = """
        SELECT
            route.id,
            route.order_key      AS orderKey,
            route.order_type     AS orderType,
            route.order_status   AS orderStatus,
            route.priority       AS priority,
            route.barcode        AS barcode,
            route.eq_id          AS eqId,
            route.eq_type        AS eqType,
            route.plc_cmd_id     AS plcCmdId,
            route.cmd_status     AS cmdStatus,
            route.from_cv_id  AS fromLocId,
            route.to_cv_id    AS toLocId,
            route.error_id       AS errorId,
            route.error_desc     AS errorDesc,
            route.started_at     AS startedAt,
            route.finished_at    AS finishedAt,
            route.created_at     AS createdAt
        FROM tb_ecs_route_order route
        INNER JOIN tb_eq_mst em
            ON em.id = route.eq_id
           AND em.eq_group_id = :eqGroupId
        WHERE route.order_status <> 9
        ORDER BY
            route.order_key DESC,
            route.created_at DESC
        """;

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("eqGroupId", eqGroupId);

            List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 200);

            for (Map row : rows) {
                String parentOrderKey = getString(row, "orderKey", "orderkey", "order_key");
                Integer orderType = parseJobType(getValue(row, "orderType", "ordertype", "order_type"));
                Integer orderStatus = getInteger(row, 0, "orderStatus", "orderstatus", "order_status");

                Integer eqTypeValue = getInteger(row, null, "eqType", "eqtype", "eq_type");
                String assignedEquipmentType = determineEquipmentType(eqTypeValue);

                JobStatusDto dto = JobStatusDto.builder()
                        .jobKey(parentOrderKey)
                        .parentJobKey(parentOrderKey)
                        .jobType(orderType)
                        .status(toUiStatusForEcs(orderStatus))
                        .fromLoc(emptyToDash(getString(row, "fromLocId", "fromLocId", "from_loc_code")))
                        .toLoc(emptyToDash(getString(row, "toLocId", "toLocId", "to_loc_code")))
                        .priority(getInteger(row, 99, "priority"))
                        .barcode(getString(row, "barcode"))
                        .eqGroupId(eqGroupId)
                        .assignedEquipmentId(getString(row, "eqId", "eqid", "eq_id"))
                        .assignedEquipmentType(assignedEquipmentType)
                        .jobLevel("ECS_ROUTE")
                        .plcCmdId(getString(row, "plcCmdId", "plccmdid", "plc_cmd_id"))
                        .cmdStatus(getInteger(row, null, "cmdStatus", "cmdstatus", "cmd_status"))
                        .errorCode(getString(row, "errorId", "errorid", "error_id"))
                        .errorMessage(getString(row, "errorDesc", "errordesc", "error_desc"))
                        .createdAt(getTimestamp(row, "createdAt", "createdat", "created_at"))
                        .ts(timestamp)
                        .build();

                if (dto.getJobKey() != null) {
                    result.add(dto);
                }
            }
        } catch (Exception e) {
            logger.warn("[ Realtime ][ Job ] route order fetch failed: eqGroupId={}", eqGroupId, e);
        }
    }

    /**
     * WCS ShuttleOrderStatus 코드 → 프론트 UI 작업 상태 변환.
     * 백엔드 SSOT: operato.logis.wcs.consts.ShuttleOrderStatus
     *
     *   0  CREATED   → CREATED        (주문 생성, ECS 전송 전)
     *   10 SENT, 20 ACCEPTED → ASSIGNED
     *   25 WAITING   → PENDING        (방해물/포트 미가용 — 시스템 대기)
     *   30 RUNNING   → RUNNING
     *   40 ARRIVED   → AWAITING_SCAN  (OUTBOUND 도착 후 작업자 박스 스캔 / finalize 대기)
     *   90 COMPLETED → COMPLETED
     *   91 CANCELLED → CANCELLED
     *   95 ABORTED   → FAILED
     *   100+ ERROR_* → FAILED
     */
    private String toUiStatusForWcs(Integer orderStatus) {
        if (orderStatus == null) return "UNKNOWN";

        return switch (orderStatus) {
            case 0 -> "CREATED";
            case 10, 20 -> "ASSIGNED";
            case 25 -> "PENDING";
            case 30 -> "RUNNING";
            case 40 -> "AWAITING_SCAN";
            case 90 -> "COMPLETED";
            case 91 -> "CANCELLED";
            case 95 -> "FAILED";
            default -> orderStatus >= 100 ? "FAILED" : "UNKNOWN";
        };
    }

    private String toUiStatusForEcs(Integer orderStatus) {
        if (orderStatus == null) {
            return "UNKNOWN";
        }

        EcsDBConsts.OrderStatus status = EcsDBConsts.OrderStatus.find(orderStatus);

        return switch (status) {
            case READY -> "PENDING";
            case EQ_SEND -> "ASSIGNED";
            case WORKING -> "RUNNING";
            case COMPLETE -> "COMPLETED";
            default -> {
                if (orderStatus == 90) yield "CANCELLED";
                if (orderStatus == 99) yield "FAILED";
                yield "UNKNOWN";
            }
        };
    }

    private int getStatusSortOrder(String status) {
        if (status == null) return 99;

        return switch (status) {
            case "AWAITING_SCAN" -> 0;
            case "RUNNING" -> 1;
            case "ASSIGNED" -> 2;
            case "PENDING" -> 3;
            case "CREATED" -> 4;
            case "FAILED" -> 5;
            case "CANCELLED" -> 6;
            case "COMPLETED" -> 7;
            default -> 99;
        };
    }

    private String getSortBaseOrderKey(JobStatusDto dto) {
        if (dto == null) return null;
        if (dto.getParentJobKey() != null && !dto.getParentJobKey().isBlank()) {
            return dto.getParentJobKey();
        }
        return dto.getJobKey();
    }

    private int compareOrderKeyDesc(String a, String b) {
        String left = normalizeOrderKey(a);
        String right = normalizeOrderKey(b);

        if (left == null && right == null) return 0;
        if (left == null) return 1;
        if (right == null) return -1;

        return right.compareTo(left);
    }

    private String normalizeOrderKey(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private String determineEquipmentType(Integer eqTypeValue) {
        if (eqTypeValue == null) return "UNKNOWN";

        return switch (EcsDBConsts.EqType.find(eqTypeValue)) {
            case SHUTTLE_CAR -> "SHUTTLE";
            case CONVEYOR -> "CONVEYOR";
            case RACK -> "RACK";
            default -> "UNKNOWN";
        };
    }

    private Integer parseJobType(Object value) {
        if (value == null) return null;

        if (value instanceof Number number) {
            return number.intValue();
        }

        String strValue = value.toString().trim().toUpperCase();

        try {
            return Integer.parseInt(strValue);
        } catch (NumberFormatException ignored) {
        }

        EcsDBConsts.OrderType orderType = EcsDBConsts.OrderType.find(strValue);
        if (orderType != EcsDBConsts.OrderType.UNKNOWN) {
            return orderType.getValue();
        }

        return null;
    }

    private String buildLocation(Object locId, Object row, Object bay) {
        String loc = locId != null ? locId.toString().trim() : "";
        if (!loc.isEmpty()) {
            return loc;
        }

        int r = toInt(row, 0);
        int b = toInt(bay, 0);

        if (r > 0 || b > 0) {
            return String.format("R%02d-B%02d", r, b);
        }

        return "";
    }

    private String emptyToDash(String value) {
        return (value == null || value.trim().isEmpty()) ? "-" : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private int toInt(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private Object getValue(Map row, String... keys) {
        if (row == null || keys == null) return null;

        for (String key : keys) {
            if (key != null && row.containsKey(key)) {
                return row.get(key);
            }
        }

        for (Object mapKeyObj : row.keySet()) {
            if (mapKeyObj == null) continue;

            String mapKey = mapKeyObj.toString();
            for (String key : keys) {
                if (key != null && mapKey.equalsIgnoreCase(key)) {
                    return row.get(mapKeyObj);
                }
            }
        }

        return null;
    }

    private String getString(Map row, String... keys) {
        Object value = getValue(row, keys);
        return value == null ? null : value.toString();
    }

    private Integer getInteger(Map row, Integer defaultValue, String... keys) {
        Object value = getValue(row, keys);
        if (value == null) return defaultValue;
        if (value instanceof Number number) return number.intValue();

        try {
            return Integer.parseInt(value.toString().trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private Long getTimestamp(Map row, String... keys) {
        Object value = getValue(row, keys);
        if (value == null) return null;
        if (value instanceof Timestamp ts) return ts.getTime();  // JDBC 대응 (Timestamp 가 Date 의 하위라 먼저 검사)
        if (value instanceof Date date) return date.getTime();
        if (value instanceof Number number) return number.longValue();

        String str = value.toString().trim();
        if (str.isEmpty()) return null;

        // ISO-8601 형식 먼저 시도 (예: "2026-04-21T08:14:08")
        try {
            return LocalDateTime.parse(str)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
        } catch (Exception ignored) {
        }

        // "2026-04-21 08:14:08" 형식 (PostgreSQL 기본 출력)
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(str, fmt)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
        } catch (Exception ignored) {
        }

        return null;
    }
}