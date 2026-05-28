package operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.realtime.provider.impl;

import operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.realtime.dto.AlarmDto;
import operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.realtime.provider.RealTimeDataProvider;
import operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.realtime.provider.RealTimeFetchContext;
import operato.logis.kmat_2026.biz.ecs.tspg4way.domain.enums.EcsDBConsts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ====================================================================
 * 알람/에러 데이터 Provider
 * ====================================================================
 *
 * [데이터 소스]
 * 1. 설비 에러:
 *    - tb_eq_car_mst: 셔틀 에러
 *    - tb_eq_cv_mst: 컨베이어/리프터 에러
 *
 * 2. ECS 작업 에러:
 *    - tb_ecs_rack_order: 랙 작업 에러 (order_status=99)
 *    - tb_ecs_route_order: 구간 이동 에러 (order_status=99)
 *
 * [필터링 기준]
 * - eqGroupId 대신 lcId 기준
 * - tb_ecs_2d_item.lc_id = :lcId 에 배치된 설비만 조회
 *
 * [브로드캐스트 주기]
 * - 1000ms
 */
@Service
public class AlarmDataProvider extends AbstractQueryService implements RealTimeDataProvider<AlarmDto> {

    private static final Logger logger = LoggerFactory.getLogger(AlarmDataProvider.class);

    @Override
    public String getProviderType() {
        return "alarm";
    }

    @Override
    public String getTopicPattern() {
        // lcId 기준 조회
        return "/topic/realtime/alarm/{lcId}/{eqGroupId}";
    }

    @Override
    public long getIntervalMs() {
        return 250L;
    }

    @Override
    public List<AlarmDto> fetchData(RealTimeFetchContext ctx) {
        String lcId = ctx.getLcId();

        List<AlarmDto> result = new ArrayList<>();
        long timestamp = System.currentTimeMillis();

        if (!hasText(lcId)) {
            logger.trace("[AlarmDataProvider] No lcId provided, skipping alarm fetch");
            return result;
        }

        try {
            fetchShuttleAlarms(lcId, result, timestamp);
            fetchConveyorAlarms(lcId, result, timestamp);
            fetchEcsRackOrderAlarms(lcId, result, timestamp);
            fetchEcsRouteOrderAlarms(lcId, result, timestamp);

            logger.trace("[AlarmDataProvider] Fetched {} alarms for lcId={}", result.size(), lcId);
        } catch (Exception e) {
            logger.error("[AlarmDataProvider] Error fetching alarm data. lcId={}", lcId, e);
        }

//        logger.info("result = {}", result);
        return result;
    }

    /**
     * 셔틀 에러 조회
     * - 현재 lcId 센터에 배치된 shuttle만 조회
     */
    private void fetchShuttleAlarms(String lcId, List<AlarmDto> result, long timestamp) {
        String sql = """
            SELECT
                car.id,
                car.eq_id,
                car.error_id,
                car.error_desc,
                car.status
            FROM tb_eq_car_mst car
            WHERE car.use_yn = true
              AND car.error_id IS NOT NULL
              AND car.error_id <> '0'
              AND EXISTS (
                    SELECT 1
                    FROM tb_ecs_2d_item item
                    WHERE item.lc_id = :lcId                )
            ORDER BY car.eq_id
            """;

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("lcId", lcId);

            List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);

            for (Map row : rows) {
                String equipmentId = toString(row.get("id"));
                String eqId = toString(row.get("eq_id"));
                if (!hasText(eqId)) eqId = equipmentId;

                AlarmDto dto = AlarmDto.builder()
                        .alarmId("SHUTTLE_" + equipmentId)
                        .alarmType("EQUIPMENT")
                        .equipmentType("SHUTTLE")
                        .equipmentId(equipmentId)
                        .equipmentCode(eqId)
                        .errorCode(toString(row.get("error_id")))
                        .errorMessage(toString(row.get("error_desc")))
                        .severity(determineSeverity(row.get("error_id"), row.get("status")))
                        .occurredAt(timestamp)
                        .acknowledged(false)
                        .ts(timestamp)
                        .build();

                result.add(dto);
            }
        } catch (Exception e) {
            logger.warn("[AlarmDataProvider] Error fetching shuttle alarms. lcId={}", lcId, e);
        }
    }

    /**
     * 컨베이어/리프터 에러 조회
     * - 현재 lcId 센터에 배치된 cv/lifter만 조회
     */
    private void fetchConveyorAlarms(String lcId, List<AlarmDto> result, long timestamp) {
        String sql = """
            SELECT
                cv.id,
                cv.eq_id,
                cv.type,
                cv.error_id,
                cv.error_desc,
                cv.status
            FROM tb_eq_cv_mst cv
            WHERE cv.use_yn = true
              AND cv.error_id IS NOT NULL
              AND cv.error_id <> '0'
              AND EXISTS (
                    SELECT 1
                    FROM tb_ecs_2d_item item
                    WHERE item.lc_id = :lcId
                      AND item.real_eq_id IS NOT NULL
                      AND (
                            item.real_eq_id = cv.id
                         OR item.real_eq_id = cv.eq_id
                      )
                )
            ORDER BY cv.eq_id
            """;

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("lcId", lcId);

            List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);

            for (Map row : rows) {
                String equipmentId = toString(row.get("id"));
                String eqId = toString(row.get("eq_id"));
                if (!hasText(eqId)) eqId = equipmentId;

                Integer typeObj = (Integer) row.get("type");
                String equipmentType = isLifter(typeObj) ? "LIFTER" : "CONVEYOR";

                AlarmDto dto = AlarmDto.builder()
                        .alarmId(equipmentType + "_" + equipmentId)
                        .alarmType("EQUIPMENT")
                        .equipmentType(equipmentType)
                        .equipmentId(equipmentId)
                        .equipmentCode(eqId)
                        .errorCode(toString(row.get("error_id")))
                        .errorMessage(toString(row.get("error_desc")))
                        .severity(determineSeverity(row.get("error_id"), row.get("status")))
                        .occurredAt(timestamp)
                        .acknowledged(false)
                        .ts(timestamp)
                        .build();

                result.add(dto);
            }
        } catch (Exception e) {
            logger.warn("[AlarmDataProvider] Error fetching conveyor/lifter alarms. lcId={}", lcId, e);
        }
    }

    /**
     * ECS 랙 작업 에러 조회
     * - lcId 센터에 배치된 설비(eq_id/eq_car_id)와 관련된 작업만 조회
     */
    private void fetchEcsRackOrderAlarms(String lcId, List<AlarmDto> result, long timestamp) {
        String sql = """
            SELECT
                rack.id,
                rack.order_key,
                rack.order_type,
                rack.eq_id,
                rack.eq_car_id,
                rack.error_id,
                rack.error_desc,
                rack.barcode
            FROM tb_ecs_rack_order rack
            WHERE rack.order_status = 99
              AND rack.error_id IS NOT NULL
              AND rack.error_id <> ''
              AND EXISTS (
                    SELECT 1
                    FROM tb_ecs_2d_item item
                    WHERE item.lc_id = :lcId
                )
            ORDER BY rack.created_at DESC
            """;

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("lcId", lcId);

            List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 50);

            for (Map row : rows) {
                String orderKey = toString(row.get("order_key"));
                String eqCarId = toString(row.get("eq_car_id"));
                String eqId = toString(row.get("eq_id"));

                String equipmentCode = hasText(eqCarId) ? eqCarId : eqId;
                String equipmentId = equipmentCode;

                AlarmDto dto = AlarmDto.builder()
                        .alarmId("ECS_RACK_" + orderKey)
                        .alarmType("JOB_ERROR")
                        .equipmentType("SHUTTLE")
                        .equipmentId(equipmentId)
                        .equipmentCode(equipmentCode)
                        .orderKey(orderKey)
                        .orderType(toInt(row.get("order_type"), 0))
                        .barcode(toString(row.get("barcode")))
                        .errorCode(toString(row.get("error_id")))
                        .errorMessage(toString(row.get("error_desc")))
                        .severity(AlarmDto.AlarmSeverity.ERROR)
                        .occurredAt(timestamp)
                        .acknowledged(false)
                        .ts(timestamp)
                        .build();

                result.add(dto);
            }
        } catch (Exception e) {
            logger.warn("[AlarmDataProvider] Error fetching ECS rack order alarms. lcId={}", lcId, e);
        }
    }

    /**
     * ECS 구간 이동 에러 조회
     * - lcId 센터에 배치된 설비(eq_id)와 관련된 작업만 조회
     */
    private void fetchEcsRouteOrderAlarms(String lcId, List<AlarmDto> result, long timestamp) {
        String sql = """
            SELECT
                route.id,
                route.order_key,
                route.order_type,
                route.eq_id,
                route.eq_type,
                route.error_id,
                route.error_desc,
                route.barcode
            FROM tb_ecs_route_order route
            WHERE route.order_status = 99
              AND route.error_id IS NOT NULL
              AND route.error_id <> ''
              AND EXISTS (
                    SELECT 1
                    FROM tb_ecs_2d_item item
                    WHERE item.lc_id = :lcId
                      AND item.real_eq_id IS NOT NULL
                      AND item.real_eq_id = route.eq_id
                )
            ORDER BY route.created_at DESC
            """;

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("lcId", lcId);

            List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 50);

            for (Map row : rows) {
                String orderKey = toString(row.get("order_key"));
                String eqId = toString(row.get("eq_id"));
                String eqType = toString(row.get("eq_type"));

                String equipmentType = determineEquipmentType(eqType);

                AlarmDto dto = AlarmDto.builder()
                        .alarmId("ECS_ROUTE_" + orderKey)
                        .alarmType("JOB_ERROR")
                        .equipmentType(equipmentType)
                        .equipmentId(eqId)
                        .equipmentCode(eqId)
                        .orderKey(orderKey)
                        .orderType(toInt(row.get("order_type"), 0))
                        .barcode(toString(row.get("barcode")))
                        .errorCode(toString(row.get("error_id")))
                        .errorMessage(toString(row.get("error_desc")))
                        .severity(AlarmDto.AlarmSeverity.ERROR)
                        .occurredAt(timestamp)
                        .acknowledged(false)
                        .ts(timestamp)
                        .build();

                result.add(dto);
            }
        } catch (Exception e) {
            logger.warn("[AlarmDataProvider] Error fetching ECS route order alarms. lcId={}", lcId, e);
        }
    }

    /**
     * 리프터 여부 판단
     */
    private boolean isLifter(int type) {
        return EcsDBConsts.ConveyorType.find(type).equals(EcsDBConsts.ConveyorType.LIFT);
    }

    /**
     * 안전한 String 변환
     */
    private String toString(Object value) {
        if (value == null) return null;
        return value.toString();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * 설비 타입 판단
     */
    private String determineEquipmentType(String eqType) {
        if (eqType == null) return "UNKNOWN";
        String upper = eqType.toUpperCase();
        if (upper.contains("LIFT")) return "LIFTER";
        if (upper.contains("CV") || upper.contains("CONV")) return "CONVEYOR";
        if (upper.contains("CAR") || upper.contains("SHUTTLE")) return "SHUTTLE";
        return eqType;
    }

    /**
     * 에러 심각도 판단
     */
    private Integer determineSeverity(Object errorId, Object status) {
        String errorCode = errorId != null ? errorId.toString() : "";
        int statusCode = toInt(status, 0);

        if (errorCode.isEmpty()) {
            return AlarmDto.AlarmSeverity.INFO;
        }

        if (statusCode == 9 || statusCode == 99) {
            return AlarmDto.AlarmSeverity.CRITICAL;
        }

        if (errorCode.startsWith("E") || errorCode.startsWith("ERR")) {
            return AlarmDto.AlarmSeverity.ERROR;
        }
        if (errorCode.startsWith("W") || errorCode.startsWith("WARN")) {
            return AlarmDto.AlarmSeverity.WARNING;
        }

        return AlarmDto.AlarmSeverity.ERROR;
    }

    private int toInt(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}