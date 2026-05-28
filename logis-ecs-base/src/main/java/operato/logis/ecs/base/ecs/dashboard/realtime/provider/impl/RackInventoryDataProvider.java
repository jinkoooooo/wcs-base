package operato.logis.ecs.base.ecs.dashboard.realtime.provider.impl;

import operato.logis.ecs.base.ecs.dashboard.realtime.provider.RealTimeDataProvider;
import operato.logis.ecs.base.ecs.dashboard.realtime.provider.RealTimeFetchContext;
import operato.logis.ecs.base.ecs.service.CellStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static operato.logis.ecs.base.ecs.dashboard.realtime.util.RowConverter.getValue;
import static operato.logis.ecs.base.ecs.dashboard.realtime.util.RowConverter.toBoolean;
import static operato.logis.ecs.base.ecs.dashboard.realtime.util.RowConverter.toInt;
import static operato.logis.ecs.base.ecs.dashboard.realtime.util.RowConverter.toStringValue;

/**
 * ====================================================================
 * 랙 재고 데이터 Provider
 * ====================================================================
 *
 * [데이터 소스 — SSOT]
 *  - CellStateService.getCellsByGroup(eqGroupId, level)
 *      (WCS 모듈. state_code / 금지축 플래그의 단일 소스)
 *  - tb_ecs_2d_item: 페이지 레이아웃 좌표 결합 (layoutId / posX / posY)
 *
 * [주의]
 *  - tb_eq_rack_mst 를 직접 쿼리하지 않는다. SKU/cargo_yn/status 같은 ECS 쪽 랙 필드는
 *    사용 금지 (CLAUDE.md P1, Q4).
 *  - use_yn 필터 없음. "locked" 플래그가 대체 축으로 작동.
 */
@Service
public class RackInventoryDataProvider extends AbstractQueryService implements RealTimeDataProvider<Map<String, Object>> {

    private static final Logger logger = LoggerFactory.getLogger(RackInventoryDataProvider.class);

    @Autowired
    private CellStateService cellStateService;

    /**
     * pageId → floorLevel 캐시. 페이지 구성은 런타임 중 거의 불변이므로 TTL 없이 유지.
     * 250ms 폴링에서 매번 단일 SELECT 를 돌리는 비용 제거 목적.
     */
    private final Map<String, Integer> floorLevelCache = new ConcurrentHashMap<>();

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
            logger.trace("[RackInventoryDataProvider] No eqGroupId, skip");
            return new ArrayList<>();
        }
        if (pageId == null || pageId.isEmpty()) {
            logger.trace("[RackInventoryDataProvider] No pageId, skip");
            return new ArrayList<>();
        }

        try {
            Integer floorLevel = getFloorLevelByPageId(pageId);
            if (floorLevel == null) {
                logger.warn("[RackInventoryDataProvider] No floorLevel for pageId={}", pageId);
                return new ArrayList<>();
            }

            List<Map> cells = cellStateService.getCellsByGroup(eqGroupId, floorLevel);
            if (cells == null || cells.isEmpty()) {
                return new ArrayList<>();
            }

            Map<String, Map<String, Object>> layoutByRealEqId = getRackLayoutsByPage(pageId);

            List<Map<String, Object>> result = new ArrayList<>(cells.size());
            long ts = System.currentTimeMillis();

            for (Map cell : cells) {
                String rackId = toStringValue(cell.get("rack_id"));
                if (rackId == null || rackId.isEmpty()) continue;

                Map<String, Object> dto = new HashMap<>();
                dto.put("rackId", rackId);
                dto.put("eqId", toStringValue(cell.get("eq_id")));
                dto.put("eqGroupId", toStringValue(cell.get("eq_group_id")));
                dto.put("row", toInt(cell.get("row"), 0));
                dto.put("bay", toInt(cell.get("bay"), 0));
                dto.put("level", toInt(cell.get("level"), 0));
                dto.put("type", toInt(cell.get("type"), 0));
                dto.put("driveOnlyYn", toBoolean(cell.get("drive_only_yn")));
                dto.put("storLoc", toStringValue(cell.get("stor_loc")));
                dto.put("stateCode", toStringValue(cell.get("state_code")));
                dto.put("locked", toBoolean(cell.get("locked")));
                dto.put("inboundForbidden", toBoolean(cell.get("inbound_forbidden")));
                dto.put("outboundForbidden", toBoolean(cell.get("outbound_forbidden")));
                dto.put("itemCode", toStringValue(cell.get("item_code")));
                dto.put("lotNo", toStringValue(cell.get("lot_no")));
                dto.put("taskId", toStringValue(cell.get("task_id")));
                dto.put("stockId", toStringValue(cell.get("stock_id")));

                Map<String, Object> layout = layoutByRealEqId.get(rackId);
                if (layout != null) {
                    dto.put("layoutId", toStringValue(layout.get("layoutId")));
                    dto.put("realEqId", rackId);
                }

                dto.put("ts", ts);
                result.add(dto);
            }

            return result;

        } catch (Exception e) {
            logger.error("[RackInventoryDataProvider] fetch error. eqGroupId={}, pageId={}", eqGroupId, pageId, e);
            return new ArrayList<>();
        }
    }

    private Map<String, Map<String, Object>> getRackLayoutsByPage(String pageId) {
        String sql = """
            SELECT layout.id        AS "layoutId",
                   layout.real_eq_id AS "realEqId"
              FROM tb_ecs_2d_item layout
             WHERE layout.page_id = :pageId
               AND layout.equipment_type_code = 'RACK'
            """;
        Map<String, Object> params = new HashMap<>();
        params.put("pageId", pageId);

        List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);

        Map<String, Map<String, Object>> map = new HashMap<>();
        for (Map row : rows) {
            String realEqId = toStringValue(getValue(row, "realEqId", "realeqid", "real_eq_id"));
            if (realEqId == null || realEqId.isEmpty()) continue;

            Map<String, Object> entry = new HashMap<>();
            entry.put("layoutId", toStringValue(getValue(row, "layoutId", "layoutid", "layout_id")));
            entry.put("realEqId", realEqId);
            map.put(realEqId, entry);
        }
        return map;
    }

    private Integer getFloorLevelByPageId(String pageId) {
        // computeIfAbsent 로 캐시 히트 시 SQL 생략. DB 실패한 경우엔 캐시 오염을 막기 위해
        // 예외 시에는 put 하지 않고 null 반환.
        Integer cached = floorLevelCache.get(pageId);
        if (cached != null) return cached;

        try {
            String sql = "SELECT floor_level FROM tb_ecs_2d_page WHERE id = :pageId";
            Map<String, Object> params = new HashMap<>();
            params.put("pageId", pageId);

            List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 1);
            if (rows != null && !rows.isEmpty()) {
                Object floorLevel = rows.get(0).get("floor_level");
                if (floorLevel instanceof Number) {
                    int value = ((Number) floorLevel).intValue();
                    floorLevelCache.put(pageId, value);
                    return value;
                }
            }
        } catch (Exception e) {
            logger.warn("[RackInventoryDataProvider] getFloorLevelByPageId pageId={}: {}", pageId, e.getMessage());
        }
        return null;
    }

}
