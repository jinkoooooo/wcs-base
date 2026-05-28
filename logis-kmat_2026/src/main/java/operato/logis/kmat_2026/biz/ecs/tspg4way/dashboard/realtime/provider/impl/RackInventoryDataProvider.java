package operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.realtime.provider.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.realtime.provider.RealTimeDataProvider;
import operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.realtime.provider.RealTimeFetchContext;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ====================================================================
 * 랙 재고 데이터 Provider
 * ====================================================================
 *
 * [데이터 소스]
 * - tb_eq_rack_mst: 랙 셀별 재고 정보
 * - tb_eq_mst: eq_group_id 기준 필터링
 * - tb_ecs_2d_item: 레이아웃 좌표 (real_eq_id = tb_eq_mst.id)
 *
 * [브로드캐스트 주기]
 * - 1000ms
 */
@Service
public class RackInventoryDataProvider extends AbstractQueryService implements RealTimeDataProvider<Map<String, Object>> {

    private static final Logger logger = LoggerFactory.getLogger(RackInventoryDataProvider.class);

    @Override
    public String getProviderType() {
        return "rack-inventory";
    }

    @Override
    public String getTopicPattern() {
        return "/topic/realtime/rack-inventory/{lcId}/{eqGroupId}/{pageId}";
    }

    @Override
    public long getIntervalMs() {
        return 250L;
    }

    @Override
    public List<Map<String, Object>> fetchData(RealTimeFetchContext ctx) {
        String eqGroupId = ctx.getEqGroupId();
        String pageId = ctx.getPageId();

        if (eqGroupId == null || eqGroupId.isEmpty()) {
            logger.trace("[RackInventoryDataProvider] No eqGroupId provided, skipping rack inventory fetch");
            return new ArrayList<>();
        }

        if (pageId == null || pageId.isEmpty()) {
            logger.trace("[RackInventoryDataProvider] No pageId provided, skipping rack inventory fetch");
            return new ArrayList<>();
        }

        try {
            String sql = """
                SELECT
                    layout.id               AS layoutId,
                    layout.real_eq_id       AS realEqId,
                    rack.id                 AS rackId,
                    rack.eq_id              AS eqId,
                    rack.row                AS row,
                    rack.bay                AS bay,
                    rack.level              AS level,
                    rack.sku_id             AS skuId,
                    rack.sku_qty            AS skuQty,
                    rack.status             AS status,
                    rack.error_id           AS errorId,
                    rack.error_desc         AS errorDesc,
                    rack.cargo_yn           AS cargoYn
                FROM tb_eq_rack_mst rack
                INNER JOIN tb_eq_mst em
                    ON em.id = rack.eq_id
                   AND em.eq_group_id = :eqGroupId
                LEFT JOIN tb_ecs_2d_item layout
                    ON layout.page_id = :pageId
                   AND layout.real_eq_id = rack.id
                   AND layout.equipment_type_code = 'RACK'
                WHERE rack.use_yn = true
                ORDER BY rack.eq_id, rack.row, rack.bay, rack.level, layout.id
                """;

            Map<String, Object> params = new HashMap<>();
            params.put("eqGroupId", eqGroupId);
            params.put("pageId", pageId);

            List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);

            List<Map<String, Object>> result = new ArrayList<>();
            long timestamp = System.currentTimeMillis();

            for (Map row : rows) {
                Object layoutId = getValue(row, "layoutId", "layoutid", "layout_id");
                Object realEqId = getValue(row, "realEqId", "realeqid", "real_eq_id");
                Object rackId = getValue(row, "rackId", "rackid", "rack_id");
                Object eqId = getValue(row, "eqId", "eqid", "eq_id");
                Object rowNo = getValue(row, "row");
                Object bay = getValue(row, "bay");
                Object level = getValue(row, "level");
                Object skuId = getValue(row, "skuId", "skuid", "sku_id");
                Object skuQty = getValue(row, "skuQty", "skuqty", "sku_qty");
                Object status = getValue(row, "status");
                Object errorId = getValue(row, "errorId", "errorid", "error_id");
                Object errorDesc = getValue(row, "errorDesc", "errordesc", "error_desc");
                Object cargoYn = getValue(row, "cargoYn", "cargoyn", "cargo_yn");

                Map<String, Object> dto = new HashMap<>();
                dto.put("layoutId", toString(layoutId));
                dto.put("realEqId", toString(realEqId));
                dto.put("rackId", toString(rackId));
                dto.put("eqId", toString(eqId));
                dto.put("row", toInt(rowNo, 0));
                dto.put("bay", toInt(bay, 0));
                dto.put("level", toInt(level, 0));
                dto.put("skuId", toString(skuId));
                dto.put("skuQty", toInt(skuQty, 0));
                dto.put("status", toInt(status, 0));
                dto.put("errorId", toString(errorId));
                dto.put("errorDesc", toString(errorDesc));
                dto.put("cargoYn", toBoolean(cargoYn));
                dto.put("hasInventory", skuId != null && toInt(skuQty, 0) > 0);
                dto.put("ts", timestamp);

                result.add(dto);
            }

//            logger.info("result = {}",result);

            return result;

        } catch (Exception e) {
            logger.error("[RackInventoryDataProvider] Error fetching rack inventory data. eqGroupId={}, pageId={}",
                    eqGroupId, pageId, e);
            return new ArrayList<>();
        }
    }

    private String toString(Object value) {
        if (value == null) return null;
        return value.toString();
    }

    private Boolean toBoolean(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).intValue() != 0;

        String s = value.toString();
        return "true".equalsIgnoreCase(s)
                || "1".equals(s)
                || "Y".equalsIgnoreCase(s)
                || "YES".equalsIgnoreCase(s);
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

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private Object getValue(Map row, String... keys) {
        if (row == null || keys == null) return null;

        for (String key : keys) {
            if (row.containsKey(key)) {
                return row.get(key);
            }
        }

        for (Object objKey : row.keySet()) {
            if (objKey == null) continue;
            String actualKey = String.valueOf(objKey);
            for (String expected : keys) {
                if (actualKey.equalsIgnoreCase(expected)) {
                    return row.get(objKey);
                }
            }
        }

        return null;
    }
}