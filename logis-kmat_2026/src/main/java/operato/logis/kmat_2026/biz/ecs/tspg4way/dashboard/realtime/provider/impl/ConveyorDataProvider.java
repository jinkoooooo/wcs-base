package operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.realtime.provider.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.realtime.dto.ConveyorStatusDto;
import operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.realtime.provider.RealTimeDataProvider;
import operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.realtime.provider.RealTimeFetchContext;
import operato.logis.kmat_2026.biz.ecs.tspg4way.domain.enums.EcsDBConsts;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ====================================================================
 * 컨베이어 상태 데이터 Provider (리프터 제외)
 * ====================================================================
 *
 * [데이터 소스]
 * - tb_eq_cv_mst: 컨베이어 마스터
 * - tb_eq_mst: eq_group_id 기준 필터링
 * - tb_ecs_2d_item: 레이아웃 좌표 (real_eq_id = tb_eq_mst.id)
 *
 * [필터링]
 * - ConveyorType.LIFT(11) 제외 (LifterDataProvider에서 처리)
 *
 * [브로드캐스트 주기]
 * - 500ms
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

        if (!hasText(eqGroupId)) {
            logger.trace("No eqGroupId provided, skipping conveyor fetch");
            return new ArrayList<>();
        }

        if (!hasText(pageId)) {
            logger.trace("No pageId provided, skipping conveyor fetch");
            return new ArrayList<>();
        }

        try {
            int liftType = EcsDBConsts.ConveyorType.LIFT.getValue();

            String sql = """
            SELECT
                cv.id,
                cv.eq_id as eqId,
                cv.type,
                cv.cargo_yn as cargoYn,
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
            LEFT JOIN tb_ecs_route_order route
                ON (route.from_cv_id = cv.id OR route.to_cv_id = cv.id)
               AND route.order_status IN (0, 1, 2)
            WHERE cv.use_yn = true
              AND (cv.type IS NULL OR cv.type != :liftType)
            ORDER BY cv.eq_id
            """;

            Map<String, Object> params = new HashMap<>();
            params.put("eqGroupId", eqGroupId);
            params.put("pageId", pageId);
            params.put("liftType", liftType);

            List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);

            List<ConveyorStatusDto> result = new ArrayList<>();
            long timestamp = System.currentTimeMillis();

            for (Map row : rows) {
                String equipmentId = toString(row.get("id"));
                String eqId = toString(row.get("eqid"));
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

                String currentOrderKey = toString(row.get("currentorderkey"));
                boolean hasActiveJob = hasText(currentOrderKey);

                ConveyorStatusDto dto = ConveyorStatusDto.builder()
                        .equipmentId(equipmentId)
                        .eqId(eqId)
                        .type(cvType)
                        .typeDesc(typeDesc)
                        .posX(centerX)
                        .posY(centerY)
                        .status(status)
                        .statusDesc(statusDesc)
                        .mode("0")
                        .hasCargo(toBoolean(row.get("cargoyn")))
                        .moving(toBoolean(row.get("runyn")))
                        .plcCmdId(toInt(row.get("plccmdid"), 0))
                        .errorCode(toString(row.get("errorid")))
                        .errorMessage(toString(row.get("errordesc")))
                        .level(toInt(row.get("level"), 1))
                        .hasActiveJob(hasActiveJob)
                        .currentOrderKey(currentOrderKey)
                        .currentOrderType(toInt(row.get("currentordertype"), 0))
                        .currentOrderStatus(toInt(row.get("currentorderstatus"), 0))
                        .currentBarcode(toString(row.get("currentbarcode")))
                        .currentFromLoc(toString(row.get("currentfromloc")))
                        .currentToLoc(toString(row.get("currenttoloc")))
                        .ts(timestamp)
                        .build();

                result.add(dto);
            }

//            logger.info("result = {}", result);

            return result;

        } catch (Exception e) {
            logger.error("Error fetching conveyor data. eqGroupId={}, pageId={}", eqGroupId, pageId, e);
            return new ArrayList<>();
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String toString(Object value) {
        return value == null ? null : value.toString();
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

    private double toDouble(Object value, double defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private boolean toBoolean(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).intValue() != 0;
        return "true".equalsIgnoreCase(value.toString()) || "1".equals(value.toString());
    }
}