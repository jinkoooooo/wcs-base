package operato.logis.ecs.tspg4way.dashboard.realtime.provider.impl;

import operato.logis.ecs.tspg4way.dashboard.realtime.dto.AlarmDto;
import operato.logis.ecs.tspg4way.dashboard.realtime.provider.RealTimeDataProvider;
import operato.logis.ecs.tspg4way.dashboard.realtime.provider.RealTimeFetchContext;
import operato.logis.ecs.tspg4way.domain.enums.EcsDBConsts;
import operato.logis.wcs.service.impl.alarm.ReinboundAlarmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static operato.logis.ecs.tspg4way.dashboard.realtime.util.RowConverter.hasText;
import static operato.logis.ecs.tspg4way.dashboard.realtime.util.RowConverter.toInt;
import static operato.logis.ecs.tspg4way.dashboard.realtime.util.RowConverter.toStringValue;

/**
 * 알람/에러 데이터 Provider. 설비 에러(car/cv) + ECS 작업 에러(rack/route, order_status=99) + 재입고 대기 알람 집계.
 * lcId 기준(tb_ecs_2d_item.lc_id 에 배치된 설비)으로 조회. 250ms 폴링.
 */
@Service
public class AlarmDataProvider extends AbstractQueryService implements RealTimeDataProvider<AlarmDto> {

    private static final Logger logger = LoggerFactory.getLogger(AlarmDataProvider.class);

    // 재입고 대기 알람 단일 소스 (followUpSince 기반) — PalletWorkstation/REST 와 동일 메서드 공유
    @Autowired
    private ReinboundAlarmService reinboundAlarmService;

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
            return result;
        }

        try {
            fetchShuttleAlarms(lcId, result, timestamp);
            fetchConveyorAlarms(lcId, result, timestamp);
            fetchEcsRackOrderAlarms(lcId, result, timestamp);
            fetchEcsRouteOrderAlarms(lcId, result, timestamp);
            fetchReinboundAlarms(ctx.getEqGroupId(), result, timestamp);
        } catch (Exception e) {
            logger.error("[ Realtime ][ Alarm ] fetch failed: lcId={}", lcId, e);
        }

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
              AND car.error_id NOT IN ('0', '')
              AND EXISTS (
                    SELECT 1
                    FROM tb_ecs_2d_item item
                    WHERE item.lc_id = :lcId
                    )
            ORDER BY car.eq_id
            """;

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("lcId", lcId);

            List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);

            for (Map row : rows) {
                String equipmentId = toStringValue(row.get("id"));
                String eqId = toStringValue(row.get("eq_id"));
                if (!hasText(eqId)) eqId = equipmentId;

                AlarmDto dto = AlarmDto.builder()
                        .alarmId("SHUTTLE_" + equipmentId)
                        .alarmType("EQUIPMENT")
                        .equipmentType("SHUTTLE")
                        .equipmentId(equipmentId)
                        .equipmentCode(eqId)
                        .errorCode(toStringValue(row.get("error_id")))
                        .errorMessage(toStringValue(row.get("error_desc")))
                        .severity(determineSeverity(row.get("error_id"), row.get("status")))
                        .occurredAt(timestamp)
                        .acknowledged(false)
                        .ts(timestamp)
                        .build();

                result.add(dto);
            }
        } catch (Exception e) {
            logger.warn("[ Realtime ][ Alarm ] shuttle alarm fetch failed: lcId={}", lcId, e);
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
                String equipmentId = toStringValue(row.get("id"));
                String eqId = toStringValue(row.get("eq_id"));
                if (!hasText(eqId)) eqId = equipmentId;

                Integer typeObj = (Integer) row.get("type");
                // tb_eq_cv_mst.type 이 null 로 내려오는 경우 auto-unboxing NPE 방지.
                String equipmentType = (typeObj != null && isLifter(typeObj)) ? "LIFTER" : "CONVEYOR";

                AlarmDto dto = AlarmDto.builder()
                        .alarmId(equipmentType + "_" + equipmentId)
                        .alarmType("EQUIPMENT")
                        .equipmentType(equipmentType)
                        .equipmentId(equipmentId)
                        .equipmentCode(eqId)
                        .errorCode(toStringValue(row.get("error_id")))
                        .errorMessage(toStringValue(row.get("error_desc")))
                        .severity(determineSeverity(row.get("error_id"), row.get("status")))
                        .occurredAt(timestamp)
                        .acknowledged(false)
                        .ts(timestamp)
                        .build();

                result.add(dto);
            }
        } catch (Exception e) {
            logger.warn("[ Realtime ][ Alarm ] conveyor alarm fetch failed: lcId={}", lcId, e);
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
                String orderKey = toStringValue(row.get("order_key"));
                String eqCarId = toStringValue(row.get("eq_car_id"));
                String eqId = toStringValue(row.get("eq_id"));

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
                        .barcode(toStringValue(row.get("barcode")))
                        .errorCode(toStringValue(row.get("error_id")))
                        .errorMessage(toStringValue(row.get("error_desc")))
                        .severity(AlarmDto.AlarmSeverity.ERROR)
                        .occurredAt(timestamp)
                        .acknowledged(false)
                        .ts(timestamp)
                        .build();

                result.add(dto);
            }
        } catch (Exception e) {
            logger.warn("[ Realtime ][ Alarm ] rack order alarm fetch failed: lcId={}", lcId, e);
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
                String orderKey = toStringValue(row.get("order_key"));
                String eqId = toStringValue(row.get("eq_id"));
                String eqType = toStringValue(row.get("eq_type"));

                String equipmentType = determineEquipmentType(eqType);

                AlarmDto dto = AlarmDto.builder()
                        .alarmId("ECS_ROUTE_" + orderKey)
                        .alarmType("JOB_ERROR")
                        .equipmentType(equipmentType)
                        .equipmentId(eqId)
                        .equipmentCode(eqId)
                        .orderKey(orderKey)
                        .orderType(toInt(row.get("order_type"), 0))
                        .barcode(toStringValue(row.get("barcode")))
                        .errorCode(toStringValue(row.get("error_id")))
                        .errorMessage(toStringValue(row.get("error_desc")))
                        .severity(AlarmDto.AlarmSeverity.ERROR)
                        .occurredAt(timestamp)
                        .acknowledged(false)
                        .ts(timestamp)
                        .build();

                result.add(dto);
            }
        } catch (Exception e) {
            logger.warn("[ Realtime ][ Alarm ] route order alarm fetch failed: lcId={}", lcId, e);
        }
    }

    /**
     * 재입고 대기 알람 조회 (followUpSince 기반 단일 소스).
     * 현재 센터(eqGroupId)의 파렛트만. occurredAt 은 follow_up_since 시각 — 변동 없어 재전송 억제.
     */
    private void fetchReinboundAlarms(String eqGroupId, List<AlarmDto> result, long timestamp) {
        if (!hasText(eqGroupId)) return;

        try {
            List<Map> rows = reinboundAlarmService.getWaitingPallets();
            for (Map row : rows) {
                if (!eqGroupId.equals(toStringValue(row.get("eq_group_id")))) continue;

                String palletBarcode = toStringValue(row.get("pallet_barcode"));
                Long since = parseFollowUpEpoch(row.get("follow_up_since"));

                AlarmDto dto = AlarmDto.builder()
                        .alarmId("REINBOUND_" + palletBarcode)
                        .alarmType("REINBOUND")
                        .equipmentType("PALLET")
                        .equipmentId(palletBarcode)
                        .equipmentCode(palletBarcode)
                        .orderKey(toStringValue(row.get("host_order_key")))
                        .barcode(palletBarcode)
                        .errorCode("REINBOUND_NEEDED")
                        .errorMessage("재입고 대기 - 출고 후 미재입고 파렛트")
                        .severity(AlarmDto.AlarmSeverity.WARNING)
                        .occurredAt(since != null ? since : timestamp)
                        .acknowledged(false)
                        .build();

                result.add(dto);
            }
        } catch (Exception e) {
            logger.warn("[ Realtime ][ Alarm ] reinbound alarm fetch failed: eqGroupId={}", eqGroupId, e);
        }
    }

    /**
     * follow_up_since ISO 문자열(zone 없음) → epoch millis. 파싱 실패 시 null.
     */
    private Long parseFollowUpEpoch(Object isoLocal) {
        if (isoLocal == null) return null;
        try {
            return LocalDateTime.parse(isoLocal.toString())
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 리프터 여부 판단
     */
    private boolean isLifter(int type) {
        return EcsDBConsts.ConveyorType.find(type).equals(EcsDBConsts.ConveyorType.LIFT);
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
}