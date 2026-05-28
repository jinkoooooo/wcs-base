package operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.realtime.provider.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.realtime.dto.JobStatusDto;
import operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.realtime.provider.RealTimeDataProvider;
import operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.realtime.provider.RealTimeFetchContext;
import operato.logis.kmat_2026.biz.ecs.tspg4way.domain.enums.EcsDBConsts;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ====================================================================
 * ECS 작업 현황 데이터 Provider
 * ====================================================================
 *
 * [데이터 소스]
 * 1. tb_wcs_shuttle_order : WCS 상위 오더
 * 2. tb_ecs_rack_order    : ECS 랙 작업 오더
 * 3. tb_ecs_route_order   : ECS 구간 이동 오더
 *
 * [eqGroupId 필터링 원칙]
 * - WCS  : tb_wcs_shuttle_order.eq_group_id = :eqGroupId
 * - ECS_RACK  : tb_eq_mst.id = rack.eq_id AND tb_eq_mst.eq_group_id = :eqGroupId
 * - ECS_ROUTE : tb_eq_mst.id = route.eq_id AND tb_eq_mst.eq_group_id = :eqGroupId
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
            logger.trace("[JobDataProvider] No eqGroupId provided, skipping job fetch");
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

            logger.trace("[JobDataProvider] fetched total jobs={}, eqGroupId={}", result.size(), eqGroupId);
        } catch (Exception e) {
            logger.error("[JobDataProvider] Error fetching job data: {}", e.getMessage(), e);
        }

//        logger.info("result = {}",result);

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
            w.from_loc_code  AS fromLocCode,
            w.to_loc_code    AS toLocCode,
            w.barcode        AS barcode,
            w.ecs_if_status  AS ecsIfStatus,
            w.eq_group_id    AS eqGroupId,
            w.created_at     AS createdAt,
            w.updated_at     AS updatedAt
        FROM tb_wcs_shuttle_order w
        WHERE w.eq_group_id = :eqGroupId
          AND w.ecs_if_status <> 50
        ORDER BY
            w.order_key DESC,
            w.created_at DESC
        """;

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("eqGroupId", eqGroupId);

            List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 100);

            if (!rows.isEmpty()) {
                logger.debug("[JobDataProvider] WCS first row keys={}", rows.get(0).keySet());
            }

            for (Map row : rows) {
                Integer orderType = parseJobType(getValue(row, "orderType", "ordertype", "order_type"));
                Integer orderStatus = getInteger(row, 0, "orderStatus", "orderstatus", "order_status");

                JobStatusDto dto = JobStatusDto.builder()
                        .jobKey(getString(row, "orderKey", "orderkey", "order_key"))
                        .jobType(orderType)
                        .status(toUiStatusForWcs(orderStatus))
                        .fromLoc(emptyToDash(getString(row, "fromLocCode", "fromloccode", "from_loc_code")))
                        .toLoc(emptyToDash(getString(row, "toLocCode", "toloccode", "to_loc_code")))
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
            logger.warn("[JobDataProvider] Error fetching WCS orders: {}", e.getMessage(), e);
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
            rack.from_loc_code  AS fromLocCode,
            rack.from_row       AS fromRow,
            rack.from_bay       AS fromBay,
            rack.to_loc_code    AS toLocCode,
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

            if (!rows.isEmpty()) {
                logger.debug("[JobDataProvider] ECS_RACK first row keys={}", rows.get(0).keySet());
            }

            for (Map row : rows) {
                String parentOrderKey = getString(row, "orderKey", "orderkey", "order_key");
                Integer orderType = parseJobType(getValue(row, "orderType", "ordertype", "order_type"));
                Integer orderStatus = getInteger(row, 0, "orderStatus", "orderstatus", "order_status");

                String fromLoc = buildLocation(
                        getValue(row, "fromLocCode", "fromloccode", "from_loc_code"),
                        getValue(row, "fromRow", "fromrow", "from_row"),
                        getValue(row, "fromBay", "frombay", "from_bay")
                );

                String toLoc = buildLocation(
                        getValue(row, "toLocCode", "toloccode", "to_loc_code"),
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

//            logger.info("result = {}", result);
        } catch (Exception e) {
            logger.warn("[JobDataProvider] Error fetching ECS rack orders: {}", e.getMessage(), e);
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
            route.from_loc_code  AS fromLocCode,
            route.to_loc_code    AS toLocCode,
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

            if (!rows.isEmpty()) {
                logger.debug("[JobDataProvider] ECS_ROUTE first row keys={}", rows.get(0).keySet());
            }

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
                        .fromLoc(emptyToDash(getString(row, "fromLocCode", "fromloccode", "from_loc_code")))
                        .toLoc(emptyToDash(getString(row, "toLocCode", "toloccode", "to_loc_code")))
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
            logger.warn("[JobDataProvider] Error fetching ECS route orders: {}", e.getMessage(), e);
        }
    }

    private String toUiStatusForWcs(Integer orderStatus) {
        if (orderStatus == null) return "UNKNOWN";

        return switch (orderStatus) {
            case 0 -> "PENDING";
            case 10, 20 -> "ASSIGNED";
            case 30 -> "RUNNING";
            case 90 -> "COMPLETED";
            case 98 -> "CANCELLED";
            case 99 -> "FAILED";
            default -> "UNKNOWN";
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
            case "RUNNING" -> 0;
            case "ASSIGNED" -> 1;
            case "PENDING" -> 2;
            case "FAILED" -> 3;
            case "CANCELLED" -> 4;
            case "COMPLETED" -> 5;
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

        EcsDBConsts.EqType eqType = EcsDBConsts.EqType.find(eqTypeValue);
        switch (eqType) {
            case SHUTTLE_CAR:
                return "SHUTTLE";
            case CONVEYOR:
                return "CONVEYOR";
            case RACK:
                return "RACK";
            default:
                return "UNKNOWN";
        }
    }

    private Integer parseJobType(Object value) {
        if (value == null) return null;

        if (value instanceof Number) {
            return ((Number) value).intValue();
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

    private String buildLocation(Object locCode, Object row, Object bay) {
        String loc = locCode != null ? locCode.toString().trim() : "";
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
        if (value instanceof Number) return ((Number) value).intValue();
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
        if (value instanceof Number) return ((Number) value).intValue();

        try {
            return Integer.parseInt(value.toString().trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private Long getTimestamp(Map row, String... keys) {
        Object value = getValue(row, keys);
        if (value == null) return null;
        if (value instanceof Date) return ((Date) value).getTime();
        if (value instanceof Number) return ((Number) value).longValue();

        try {
            return new Date(value.toString()).getTime();
        } catch (Exception e) {
            return null;
        }
    }
}