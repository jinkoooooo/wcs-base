package operato.logis.ecs.tspg4way.dashboard.realtime.provider.impl;

import operato.logis.ecs.tspg4way.dashboard.realtime.dto.ShuttlePositionDto;
import operato.logis.ecs.tspg4way.dashboard.realtime.provider.RealTimeDataProvider;
import operato.logis.ecs.tspg4way.dashboard.realtime.provider.RealTimeFetchContext;
import operato.logis.ecs.tspg4way.dashboard.realtime.service.EquipmentStateClassifier;
import operato.logis.ecs.tspg4way.domain.enums.EcsDBConsts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static operato.logis.ecs.tspg4way.dashboard.realtime.util.RowConverter.getString;
import static operato.logis.ecs.tspg4way.dashboard.realtime.util.RowConverter.getValue;
import static operato.logis.ecs.tspg4way.dashboard.realtime.util.RowConverter.toBoolean;
import static operato.logis.ecs.tspg4way.dashboard.realtime.util.RowConverter.toDouble;
import static operato.logis.ecs.tspg4way.dashboard.realtime.util.RowConverter.toInt;

/**
 * 셔틀 위치/상태 데이터 Provider. 셀(row/bay)을 레이아웃 좌표로 매핑(1분 TTL 캐시). 250ms 폴링.
 */
@Service
public class ShuttleDataProvider extends AbstractQueryService implements RealTimeDataProvider<ShuttlePositionDto> {

    private static final Logger logger = LoggerFactory.getLogger(ShuttleDataProvider.class);

    // 레이아웃 캐시 엔트리 (1분 TTL)
    private static class CacheEntry {
        final Map<String, RackCellLayout> data;
        final long timestamp;

        CacheEntry(Map<String, RackCellLayout> data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        // 1분(60,000ms) 유효기간 체크
        boolean isExpired() {
            return (System.currentTimeMillis() - this.timestamp) > 60_000L;
        }
    }

    // 레이아웃 좌표 캐시 (eqGroupId:pageId → 셀 좌표맵)
    private final Map<String, CacheEntry> layoutCache = new ConcurrentHashMap<>();

    @Override
    public String getProviderType() {
        return "shuttle";
    }

    @Override
    public String getTopicPattern() {
        return "/topic/realtime/shuttle/{lcId}/{eqGroupId}/{pageId}";
    }

    @Override
    public long getIntervalMs() {
        return 250L;
    }

    @Override
    public List<ShuttlePositionDto> fetchData(RealTimeFetchContext ctx) {
        String eqGroupId = ctx.getEqGroupId();
        String pageId = ctx.getPageId();

        if (eqGroupId == null || eqGroupId.isEmpty() || pageId == null || pageId.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            Integer floorLevel = getFloorLevelByPageId(pageId);

            if (floorLevel == null) {
                logger.warn("[ Realtime ][ Shuttle ] floorLevel not found: pageId={}", pageId);
                return new ArrayList<>();
            }

            // 캐싱된 좌표 또는 DB 최신 좌표(1분 주기)
            Map<String, RackCellLayout> rackCellMap = getRackCellLayoutMap(eqGroupId, pageId);

            String sql = """
                WITH active_rack_order AS (
                    SELECT
                        r.eq_car_id,
                        r.order_key,
                        r.order_type,
                        r.order_status,
                        r.barcode,
                        r.from_loc_code,
                        r.to_loc_code,
                        ROW_NUMBER() OVER (
                            PARTITION BY r.eq_car_id
                            ORDER BY
                                CASE r.order_status
                                    WHEN 2 THEN 1
                                    WHEN 1 THEN 2
                                    WHEN 0 THEN 3
                                    ELSE 9
                                END,
                                r.updated_at DESC NULLS LAST,
                                r.created_at DESC NULLS LAST
                        ) AS rn
                    FROM tb_ecs_rack_order r
                    WHERE r.order_status IN (0, 1, 2)
                )
                SELECT
                    car.id,
                    car.eq_id,
                    car.type,
                    car.row,
                    car.bay,
                    car.level,
                    car.rack_id,
                    car.status,
                    car.battery_status AS battery_status,
                    car.cargo_yn AS cargo_yn,
                    car.auto_yn AS auto_yn,
                    car.use_yn AS use_yn,
                    car.error_id AS error_id,
                    car.error_desc AS error_desc,
                    aro.order_key AS current_order_key,
                    aro.order_type AS current_order_type,
                    aro.order_status AS current_order_status,
                    aro.barcode AS current_barcode,
                    aro.from_loc_code AS current_from_loc,
                    aro.to_loc_code AS current_to_loc
                FROM tb_eq_car_mst car
                INNER JOIN tb_eq_mst em
                    ON em.id = car.eq_id
                   AND em.eq_group_id = :eqGroupId
                LEFT JOIN active_rack_order aro
                    ON aro.eq_car_id = car.eq_id
                   AND aro.rn = 1
                WHERE car.level = :floorLevel
                ORDER BY car.eq_id
                """;

            Map<String, Object> params = new HashMap<>();
            params.put("eqGroupId", eqGroupId);
            params.put("floorLevel", floorLevel);

            List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);

            List<ShuttlePositionDto> result = new ArrayList<>();
            long timestamp = System.currentTimeMillis();

            for (Map row : rows) {
                String equipmentId = getString(row, "eq_id", "eqId");
                String id = getString(row, "id");
                if (equipmentId == null || equipmentId.isEmpty()) {
                    equipmentId = id;
                }

                int carRow = toInt(getValue(row, "row"), 1);
                int carBay = toInt(getValue(row, "bay"), 1);

                String cellKey = buildCellKey(carRow, carBay);
                RackCellLayout cellLayout = rackCellMap.get(cellKey);

                double centerX;
                double centerY;

                if (cellLayout != null) {
                    centerX = cellLayout.centerX;
                    centerY = cellLayout.centerY;
                } else {
                    centerX = 100;
                    centerY = 100;
                    logger.debug(
                            "[ Realtime ][ Shuttle ] rack cell layout missing: shuttle={}, row={}, bay={}, eqGroupId={}, pageId={}",
                            equipmentId, carRow, carBay, eqGroupId, pageId
                    );
                }

                int status = toInt(getValue(row, "status"), 0);
                int batteryStatus = toInt(getValue(row, "battery_status", "batteryStatus"), 0);
                int batteryLevel = convertBatteryStatusToLevel(batteryStatus);
                String statusDesc = EcsDBConsts.EqCarStatus.find(status).getDescription();

                String currentOrderKey = getString(row, "current_order_key", "currentOrderKey");
                boolean hasActiveJob = currentOrderKey != null && !currentOrderKey.isEmpty();

                boolean autoYn = toBoolean(getValue(row, "auto_yn", "autoYn"));
                boolean useYn = toBoolean(getValue(row, "use_yn", "useYn"));
                String errorId = getString(row, "error_id", "errorId");

                EquipmentStateClassifier.ShuttleState shuttleState =
                        EquipmentStateClassifier.classifyShuttleState(
                                new EquipmentStateClassifier.ShuttleStateInput(
                                        useYn, errorId, status, autoYn, batteryStatus));

                ShuttlePositionDto dto = ShuttlePositionDto.builder()
                        .equipmentId(equipmentId)
                        .equipmentCode(equipmentId)
                        .posX(centerX)
                        .posY(centerY)
                        .floor(toInt(getValue(row, "level"), 1))
                        .status(status)
                        .statusDesc(statusDesc)
                        .movementStatus(0)
                        .batteryLevel(batteryLevel)
                        .batteryStatus(batteryStatus)
                        .hasCargo(toBoolean(getValue(row, "cargo_yn", "cargoYn")))
                        .autoYn(autoYn)
                        .useYn(useYn)
                        .shuttleState(shuttleState.name())
                        .errorCode(errorId)
                        .errorMessage(getString(row, "error_desc", "errorDesc"))
                        .row(carRow)
                        .bay(carBay)
                        .hasActiveJob(hasActiveJob)
                        .currentOrderKey(currentOrderKey)
                        .currentOrderType(toInt(getValue(row, "current_order_type", "currentOrderType"), 0))
                        .currentOrderStatus(toInt(getValue(row, "current_order_status", "currentOrderStatus"), 0))
                        .currentBarcode(getString(row, "current_barcode", "currentBarcode"))
                        .currentFromLoc(getString(row, "current_from_loc", "currentFromLoc"))
                        .currentToLoc(getString(row, "current_to_loc", "currentToLoc"))
                        .ts(timestamp)
                        .build();

                result.add(dto);
            }

            return result;

        } catch (Exception e) {
            logger.error("[ Realtime ][ Shuttle ] fetch failed: eqGroupId={}, pageId={}", eqGroupId, pageId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 현재 eqGroupId/pageId 기준으로 배치된 RACK들의 실제 셀 좌표 맵
     * key = "row|bay"
     */
    private Map<String, RackCellLayout> getRackCellLayoutMap(String eqGroupId, String pageId) {
        String cacheKey = eqGroupId + ":" + pageId;
        CacheEntry entry = layoutCache.get(cacheKey);

        // TTL(1분) 내면 DB 조회 없이 캐시 반환
        if (entry != null && !entry.isExpired()) {
            return entry.data;
        }

        Map<String, RackCellLayout> map = new HashMap<>();

        try {
            String sql = """
                SELECT
                    rack.row,
                    rack.bay,
                    layout.pos_x    AS posX,
                    layout.pos_y    AS posY,
                    layout.width,
                    layout.height
                FROM tb_eq_rack_mst rack
                INNER JOIN tb_eq_mst em
                    ON em.id = rack.eq_id
                   AND em.eq_group_id = :eqGroupId
                INNER JOIN tb_ecs_2d_item layout
                    ON layout.page_id = :pageId
                   AND layout.real_eq_id = rack.rack_id
                   AND layout.equipment_type_code = 'RACK'
                WHERE rack.use_yn = true
                ORDER BY rack.row, rack.bay
                """;

            Map<String, Object> params = new HashMap<>();
            params.put("eqGroupId", eqGroupId);
            params.put("pageId", pageId);

            List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);

            for (Map row : rows) {
                int rackRow = toInt(getValue(row, "row"), -1);
                int rackBay = toInt(getValue(row, "bay"), -1);

                double posX = toDouble(getValue(row, "posx", "posX"), 0);
                double posY = toDouble(getValue(row, "posy", "posY"), 0);
                double width = toDouble(getValue(row, "width"), 60);
                double height = toDouble(getValue(row, "height"), 60);

                RackCellLayout layout = new RackCellLayout();
                layout.centerX = posX + width / 2.0;
                layout.centerY = posY + height / 2.0;

                map.put(buildCellKey(rackRow, rackBay), layout);
            }

            // DB 조회 성공 시에만 타임스탬프와 함께 캐시 저장
            if (!map.isEmpty()) {
                layoutCache.put(cacheKey, new CacheEntry(map));
                logger.debug("[ Realtime ][ Shuttle ] layout cached: count={}, key={}", map.size(), cacheKey);
            }

        } catch (Exception e) {
            logger.warn("[ Realtime ][ Shuttle ] rack layout fetch failed: eqGroupId={}, pageId={}", eqGroupId, pageId, e);
        }

        return map;
    }

    private String buildCellKey(int row, int bay) {
        return row + "|" + bay;
    }

    private static class RackCellLayout {
        double centerX;
        double centerY;
    }

    private Integer getFloorLevelByPageId(String pageId) {
        try {
            String sql = "SELECT floor_level FROM tb_ecs_2d_page WHERE id = :pageId";
            Map<String, Object> params = new HashMap<>();
            params.put("pageId", pageId);

            List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 1);
            if (rows != null && !rows.isEmpty()) {
                Object floorLevel = rows.get(0).get("floor_level");
                if (floorLevel instanceof Number number) {
                    return number.intValue();
                }
            }
        } catch (Exception e) {
            logger.warn("[ Realtime ][ Shuttle ] floorLevel lookup failed: pageId={}", pageId, e);
        }
        return null;
    }

    // 배터리 상태 코드 → 표시용 잔량(%) 근사값.
    private int convertBatteryStatusToLevel(int batteryStatus) {
        return switch (batteryStatus) {
            case 0 -> 80;
            case 1 -> 15;
            case 2 -> 50;
            case 9 -> 100;
            default -> 50;
        };
    }
}