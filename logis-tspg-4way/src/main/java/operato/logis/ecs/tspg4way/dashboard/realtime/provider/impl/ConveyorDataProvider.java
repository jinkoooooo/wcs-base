package operato.logis.ecs.tspg4way.dashboard.realtime.provider.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import operato.logis.ecs.tspg4way.dashboard.realtime.dto.ConveyorStatusDto;
import operato.logis.ecs.tspg4way.dashboard.realtime.provider.RealTimeDataProvider;
import operato.logis.ecs.tspg4way.dashboard.realtime.provider.RealTimeFetchContext;
import operato.logis.ecs.tspg4way.dashboard.realtime.service.EquipmentStateClassifier;
import operato.logis.ecs.tspg4way.domain.enums.EcsDBConsts;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static operato.logis.ecs.tspg4way.dashboard.realtime.util.RowConverter.hasText;
import static operato.logis.ecs.tspg4way.dashboard.realtime.util.RowConverter.toBoolean;
import static operato.logis.ecs.tspg4way.dashboard.realtime.util.RowConverter.toDouble;
import static operato.logis.ecs.tspg4way.dashboard.realtime.util.RowConverter.toInt;
import static operato.logis.ecs.tspg4way.dashboard.realtime.util.RowConverter.toStringValue;

/**
 * 컨베이어 상태 데이터 Provider. ConveyorType.LIFT 는 제외(LifterDataProvider 담당).
 * 소스: tb_eq_cv_mst + tb_eq_mst(eq_group_id) + tb_ecs_2d_item(레이아웃 좌표).
 */
@Service
public class ConveyorDataProvider extends AbstractQueryService implements RealTimeDataProvider<ConveyorStatusDto> {

    private static final Logger logger = LoggerFactory.getLogger(ConveyorDataProvider.class);

    @Override
    public String getProviderType() {
        return "conveyor";
    }

    @Override
    public String getTopicPattern() {
        return "/topic/realtime/conveyor/{lcId}/{eqGroupId}/{pageId}";
    }

    @Override
    public long getIntervalMs() {
        return 250L;
    }

    @Override
    public List<ConveyorStatusDto> fetchData(RealTimeFetchContext ctx) {
        String eqGroupId = ctx.getEqGroupId();
        String pageId = ctx.getPageId();

        if (!hasText(eqGroupId) || !hasText(pageId)) {
            return new ArrayList<>();
        }

        try {
            int liftType = EcsDBConsts.ConveyorType.LIFT.getValue();

            // pallet 판정용 has_route_data 는 EXISTS 로 단일 boolean 을 뽑고,
            // 표시용 오더 정보는 LATERAL 서브쿼리 + LIMIT 1 로 "진행중 오더 하나" 만 가져온다.
            // → LEFT JOIN 으로 인한 row 중복 방지 + pallet/display 관심사 분리.
            String sql = """
            SELECT
                cv.id,
                cv.eq_id as eqId,
                cv.type,
                cv.cargo_yn as cargoYn,
                cv.auto_yn as autoYn,
                cv.use_yn as useYn,
                cv.level,
                cv.status,
                cv.run_yn as runYn,
                cv.plc_cmd_id as plcCmdId,
                cv.error_id as errorId,
                cv.error_desc as errorDesc,
                layout.pos_x as posX,
                layout.pos_y as posY,
                layout.width as width,
                layout.height as height,
                EXISTS (
                    SELECT 1 FROM tb_ecs_route_order ro
                     WHERE (ro.from_cv_id = cv.id OR ro.to_cv_id = cv.id)
                       AND ro.order_status <> :completeStatus
                ) AS hasRouteData,
                route.order_key as currentOrderKey,
                route.order_type as currentOrderType,
                route.order_status as currentOrderStatus,
                route.barcode as currentBarcode,
                route.from_cv_id as currentFromLoc,
                route.to_cv_id as currentToLoc
            FROM tb_eq_cv_mst cv
            INNER JOIN tb_eq_mst em
                ON em.id = cv.eq_id
               AND em.eq_group_id = :eqGroupId
            INNER JOIN tb_ecs_2d_item layout
                ON layout.page_id = :pageId
               AND layout.real_eq_id = cv.id
            LEFT JOIN LATERAL (
                SELECT ro.order_key, ro.order_type, ro.order_status,
                       ro.barcode, ro.from_cv_id, ro.to_cv_id
                  FROM tb_ecs_route_order ro
                 WHERE (ro.from_cv_id = cv.id OR ro.to_cv_id = cv.id)
                   AND ro.order_status <> :completeStatus
                 ORDER BY CASE ro.order_status WHEN 2 THEN 1 WHEN 1 THEN 2 WHEN 0 THEN 3 ELSE 9 END
                 LIMIT 1
            ) route ON TRUE
            WHERE cv.type IS NULL OR cv.type != :liftType
            ORDER BY cv.eq_id
            """;

            Map<String, Object> params = new HashMap<>();
            params.put("eqGroupId", eqGroupId);
            params.put("pageId", pageId);
            params.put("liftType", liftType);
            params.put("completeStatus", EcsDBConsts.OrderStatus.COMPLETE.getValue());

            List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);

            List<ConveyorStatusDto> result = new ArrayList<>();
            long timestamp = System.currentTimeMillis();

            for (Map row : rows) {
                String equipmentId = toStringValue(row.get("id"));
                String eqId = toStringValue(row.get("eqid"));
                if (!hasText(eqId)) {
                    eqId = equipmentId;
                }

                int status = toInt(row.get("status"), 0);
                int cvType = toInt(row.get("type"), 0);

                String statusDesc = EcsDBConsts.EqConveyorStatus.find(status).getDescription();
                String typeDesc = EcsDBConsts.ConveyorType.find(cvType).getDescription();

                double posX = toDouble(row.get("posx"), 0);
                double posY = toDouble(row.get("posy"), 0);
                double width = toDouble(row.get("width"), 60);
                double height = toDouble(row.get("height"), 40);
                double centerX = posX + width / 2.0;
                double centerY = posY + height / 2.0;

                String currentOrderKey = toStringValue(row.get("currentorderkey"));
                boolean hasActiveJob = hasText(currentOrderKey);

                boolean hasCargo = toBoolean(row.get("cargoyn"));
                boolean autoYn = toBoolean(row.get("autoyn"));
                boolean useYn = toBoolean(row.get("useyn"));
                String errorId = toStringValue(row.get("errorid"));
                boolean hasRouteData = toBoolean(row.get("hasroutedata"));

                EquipmentStateClassifier.ConveyorState conveyorState =
                        EquipmentStateClassifier.classifyConveyorState(
                                new EquipmentStateClassifier.ConveyorStateInput(useYn, errorId, autoYn));
                EquipmentStateClassifier.PalletState palletState =
                        EquipmentStateClassifier.classifyPalletState(hasRouteData, hasCargo);

                ConveyorStatusDto dto = ConveyorStatusDto.builder()
                        .equipmentId(equipmentId)
                        .eqId(eqId)
                        .type(cvType)
                        .typeDesc(typeDesc)
                        .posX(centerX)
                        .posY(centerY)
                        .status(status)
                        .statusDesc(statusDesc)
                        .hasCargo(hasCargo)
                        .moving(toBoolean(row.get("runyn")))
                        .autoYn(autoYn)
                        .useYn(useYn)
                        .conveyorState(conveyorState.name())
                        .palletState(palletState == null ? null : palletState.name())
                        .plcCmdId(toInt(row.get("plccmdid"), 0))
                        .errorCode(errorId)
                        .errorMessage(toStringValue(row.get("errordesc")))
                        .level(toInt(row.get("level"), 1))
                        .hasActiveJob(hasActiveJob)
                        .currentOrderKey(currentOrderKey)
                        .currentOrderType(toInt(row.get("currentordertype"), 0))
                        .currentOrderStatus(toInt(row.get("currentorderstatus"), 0))
                        .currentBarcode(toStringValue(row.get("currentbarcode")))
                        .currentFromLoc(toStringValue(row.get("currentfromloc")))
                        .currentToLoc(toStringValue(row.get("currenttoloc")))
                        .ts(timestamp)
                        .build();

                result.add(dto);
            }

            return result;

        } catch (Exception e) {
            logger.error("[ Realtime ][ Conveyor ] fetch failed: eqGroupId={}, pageId={}", eqGroupId, pageId, e);
            return new ArrayList<>();
        }
    }

}