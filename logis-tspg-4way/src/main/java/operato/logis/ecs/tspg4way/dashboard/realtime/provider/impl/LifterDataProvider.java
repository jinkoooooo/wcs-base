package operato.logis.ecs.tspg4way.dashboard.realtime.provider.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import operato.logis.ecs.tspg4way.dashboard.realtime.dto.LifterStatusDto;
import operato.logis.ecs.tspg4way.dashboard.realtime.provider.RealTimeDataProvider;
import operato.logis.ecs.tspg4way.dashboard.realtime.provider.RealTimeFetchContext;
import operato.logis.ecs.tspg4way.domain.enums.EcsDBConsts;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static operato.logis.ecs.tspg4way.dashboard.realtime.util.RowConverter.getValue;
import static operato.logis.ecs.tspg4way.dashboard.realtime.util.RowConverter.hasText;
import static operato.logis.ecs.tspg4way.dashboard.realtime.util.RowConverter.toBoolean;
import static operato.logis.ecs.tspg4way.dashboard.realtime.util.RowConverter.toDouble;
import static operato.logis.ecs.tspg4way.dashboard.realtime.util.RowConverter.toInt;
import static operato.logis.ecs.tspg4way.dashboard.realtime.util.RowConverter.toIntOrNull;
import static operato.logis.ecs.tspg4way.dashboard.realtime.util.RowConverter.toStringValue;

/**
 * 리프터 상태 데이터 Provider (ConveyorType.LIFT 만 처리). 250ms 폴링.
 */
@Service
public class LifterDataProvider extends AbstractQueryService implements RealTimeDataProvider<LifterStatusDto> {

    private static final Logger logger = LoggerFactory.getLogger(LifterDataProvider.class);

    @Override
    public String getProviderType() {
        return "lifter";
    }

    @Override
    public String getTopicPattern() {
        return "/topic/realtime/lifter/{lcId}/{eqGroupId}/{pageId}";
    }

    @Override
    public long getIntervalMs() {
        return 250L;
    }

    @Override
    public List<LifterStatusDto> fetchData(RealTimeFetchContext ctx) {
        String eqGroupId = ctx.getEqGroupId();
        String pageId = ctx.getPageId();

        if (!hasText(eqGroupId) || !hasText(pageId)) {
            return new ArrayList<>();
        }

        try {
            int liftType = EcsDBConsts.ConveyorType.LIFT.getValue();

            String sql = """
                WITH active_route AS (
                    SELECT
                        r.eq_id,
                        r.order_key,
                        r.order_type,
                        r.order_status,
                        r.barcode,
                        r.from_cv_id,
                        r.to_cv_id,
                        ROW_NUMBER() OVER (
                            PARTITION BY r.eq_id
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
                    FROM tb_ecs_route_order r
                    INNER JOIN tb_eq_mst em_r
                        ON em_r.id = r.eq_id
                       AND em_r.eq_group_id = :eqGroupId
                    WHERE r.order_status <> :completeStatus
                )
                SELECT
                    layout.id               AS "layoutId",
                    layout.pos_x            AS "posX",
                    layout.pos_y            AS "posY",
                    layout.width            AS "width",
                    layout.height           AS "height",

                    cv.id                   AS "equipmentId",
                    cv.eq_id                AS "eqId",
                    cv.type                 AS "cvType",
                    cv.cargo_yn             AS "cargoYn",
                    cv.stopper_open_yn      AS "stopperOpenYn",
                    cv.auto_yn              AS "autoYn",
                    cv.use_yn               AS "useYn",
                    cv.run_yn               AS "runYn",
                    cv.level                AS "currentLevel",
                    cv.status               AS "status",
                    cv.plc_cmd_id           AS "plcCmdId",
                    cv.error_id             AS "errorId",
                    cv.error_desc           AS "errorDesc",

                    ar.order_key            AS "currentOrderKey",
                    ar.order_type           AS "currentOrderType",
                    ar.order_status         AS "currentOrderStatus",
                    ar.barcode              AS "currentBarcode",
                    ar.from_cv_id        AS "currentFromLoc",
                    ar.to_cv_id          AS "currentToLoc"

                FROM tb_eq_cv_mst cv
                INNER JOIN tb_eq_mst em
                    ON em.id = cv.eq_id
                   AND em.eq_group_id = :eqGroupId
                INNER JOIN tb_ecs_2d_item layout
                    ON layout.page_id = :pageId
                   AND layout.real_eq_id = cv.id
                LEFT JOIN active_route ar
                    ON ar.eq_id = cv.eq_id
                   AND ar.rn = 1
                WHERE cv.type = :liftType
                ORDER BY cv.eq_id, layout.id
                """;

            Map<String, Object> params = new HashMap<>();
            params.put("eqGroupId", eqGroupId);
            params.put("pageId", pageId);
            params.put("liftType", liftType);
            params.put("completeStatus",
                    EcsDBConsts.OrderStatus.COMPLETE.getValue());

            List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);

            List<LifterStatusDto> result = new ArrayList<>();
            long timestamp = System.currentTimeMillis();

            for (Map row : rows) {
                String equipmentId = toStringValue(getValue(row, "equipmentId", "equipment_id", "equipmentid"));
                String eqId = toStringValue(getValue(row, "eqId", "eq_id", "eqid"));
                String layoutId = toStringValue(getValue(row, "layoutId", "layout_id", "layoutid"));

                double posX = toDouble(getValue(row, "posX", "pos_x", "posx"), 0);
                double posY = toDouble(getValue(row, "posY", "pos_y", "posy"), 0);
                double width = toDouble(getValue(row, "width"), 0);
                double height = toDouble(getValue(row, "height"), 0);

                double centerX = posX + width / 2.0;
                double centerY = posY + height / 2.0;

                Integer currentLevel = toIntOrNull(getValue(row, "currentLevel", "current_level", "currentlevel"));
                String toLocId = toStringValue(getValue(row, "currentToLoc", "current_to_loc", "currenttoloc"));
                Integer targetLevel = parseFloorFromLocIdOrNull(toLocId);

                String currentOrderKey = toStringValue(getValue(row, "currentOrderKey", "current_order_key", "currentorderkey"));
                Object currentOrderTypeObj = getValue(row, "currentOrderType", "current_order_type", "currentordertype");
                Object currentOrderStatusObj = getValue(row, "currentOrderStatus", "current_order_status", "currentorderstatus");

                boolean hasActiveJob = hasText(currentOrderKey);

                boolean moving = isMoving(
                        getValue(row, "runYn", "run_yn", "runyn"),
                        getValue(row, "status"),
                        currentLevel,
                        targetLevel
                );

                boolean hasShuttle = inferHasShuttle(currentOrderTypeObj, hasActiveJob);

                boolean stopperOpen = toBoolean(getValue(row, "stopperOpenYn", "stopper_open_yn", "stopperopenyn"));

                LifterStatusDto dto = LifterStatusDto.builder()
                        .equipmentId(equipmentId)
                        .eqId(eqId)
                        .layoutId(layoutId)
                        .posX(centerX)
                        .posY(centerY)
                        .currentLevel(currentLevel)
                        .targetLevel(targetLevel)
                        .status(toInt(getValue(row, "status"), 0))
                        .hasCargo(toBoolean(getValue(row, "cargoYn", "cargo_yn", "cargoyn")))
                        .hasShuttle(hasShuttle)
                        .moving(moving)
                        .stopperOpen(stopperOpen)
                        .plcCmdId(toInt(getValue(row, "plcCmdId", "plc_cmd_id", "plccmdid"), 0))
                        .errorId(toStringValue(getValue(row, "errorId", "error_id", "errorid")))
                        .errorMessage(toStringValue(getValue(row, "errorDesc", "error_desc", "errordesc")))
                        .useYn(toBoolean(getValue(row, "useYn", "use_yn", "useyn")))
                        .hasActiveJob(hasActiveJob)
                        .currentOrderKey(currentOrderKey)
                        .currentOrderType(toIntOrNull(currentOrderTypeObj))
                        .currentOrderStatus(toIntOrNull(currentOrderStatusObj))
                        .currentBarcode(toStringValue(getValue(row, "currentBarcode", "current_barcode", "currentbarcode")))
                        .currentFromLoc(toStringValue(getValue(row, "currentFromLoc", "current_from_loc", "currentfromloc")))
                        .currentToLoc(toLocId)
                        .ts(timestamp)
                        .build();

                result.add(dto);
            }

            return result;

        } catch (Exception e) {
            logger.error("[ Realtime ][ Lifter ] fetch failed: eqGroupId={}, pageId={}", eqGroupId, pageId, e);
            return new ArrayList<>();
        }
    }

    // run_yn=true 거나, status=2(이동중)이고 목표층≠현재층이면 이동중으로 판정.
    private boolean isMoving(Object runYnObj, Object statusObj, Integer currentLevel, Integer targetLevel) {
        if (toBoolean(runYnObj)) return true;
        return toInt(statusObj, 0) == 2
                && targetLevel != null
                && currentLevel != null
                && !targetLevel.equals(currentLevel);
    }

    // 활성 작업이 있고 오더 타입이 셔틀 적재/하역(21/22)이면 셔틀 탑승 상태로 판정.
    private boolean inferHasShuttle(Object currentOrderTypeObj, boolean hasActiveJob) {
        if (!hasActiveJob) return false;
        Integer currentOrderType = toIntOrNull(currentOrderTypeObj);
        return currentOrderType != null && (currentOrderType == 21 || currentOrderType == 22);
    }

    // locId 의 "_F<n>" 또는 "<n>F" 패턴에서 층 번호 추출. 없으면 null.
    private Integer parseFloorFromLocIdOrNull(String locId) {
        if (!hasText(locId)) return null;

        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("_F(\\d+)").matcher(locId);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
            }
        }

        matcher = java.util.regex.Pattern.compile("(\\d+)F").matcher(locId);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
            }
        }

        return null;
    }
}