package operato.logis.wcs.simulator;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import operato.logis.inventory.consts.StockStatus;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

// 시뮬레이터 재고·랙 조회 전담 (Lookup). HostSimulator 의 오더 생성 흐름이 가용 자원 판단에 사용.
@Component
public class SimStockQuery extends AbstractQueryService {

    // 입고용 빈 RACK 수 카운트.
    @SuppressWarnings("rawtypes")
    public int countEmptyRacks(String groupId) {
        String sql = """
                SELECT COUNT(*) AS cnt FROM tb_inventory_location
                 WHERE loc_group = :eqGroupId
                   AND loc_type = 'RACK'
                   AND is_enabled = TRUE
                   AND task_id IS NULL
                   AND stock_id IS NULL
                """;
        Map<String, Object> p = ValueUtil.newMap("eqGroupId", groupId);
        List<Map> rows = queryManager.selectListBySql(sql, p, Map.class, 0, 1);
        return ValueUtil.isEmpty(rows) ? 0 : toInt(rows.get(0).get("cnt"));
    }

    // 출고 가능한 시뮬 재고 수 카운트 — IDLE + 50 이상.
    @SuppressWarnings("rawtypes")
    public int countSimStocks(String groupId) {
        String sql = """
                SELECT COUNT(*) AS cnt FROM tb_inventory_stock s
                  JOIN tb_inventory_location l ON l.stock_id = s.stock_id
                 WHERE l.loc_group = :eqGroupId
                   AND l.task_id IS NULL
                   AND s.lot_no LIKE :lotPrefix
                   AND s.is_enabled = TRUE
                   AND s.item_owner = :owner
                   AND s.item_qty >= 50
                   AND s.stock_status = :stockStatus
                """;
        List<Map> rows = queryManager.selectListBySql(sql, simStockParams(groupId), Map.class, 0, 1);
        return ValueUtil.isEmpty(rows) ? 0 : toInt(rows.get(0).get("cnt"));
    }

    // 랜덤 시뮬 재고 1건 (buildOutbound 보조).
    @SuppressWarnings({"rawtypes", "unchecked"})
    public SimStock pickRandom(String groupId) {
        String sql = """
                SELECT s.stock_id AS stock_id, s.item_code AS item_code,
                       s.lot_no AS lot_no, s.item_qty AS item_qty, l.loc_id AS loc_id
                  FROM tb_inventory_stock s
                  JOIN tb_inventory_location l ON l.stock_id = s.stock_id
                 WHERE l.loc_group = :eqGroupId
                   AND l.task_id IS NULL
                   AND s.lot_no LIKE :lotPrefix
                   AND s.is_enabled = TRUE
                   AND s.item_owner = :owner
                   AND s.item_qty >= 50
                   AND s.stock_status = :stockStatus
                 ORDER BY RANDOM()
                """;
        List<Map> rows = queryManager.selectListBySql(sql, simStockParams(groupId), Map.class, 0, 1);
        if (ValueUtil.isEmpty(rows)) return null;
        Map row = rows.get(0);
        return new SimStock(
                (String) row.get("stock_id"),
                (String) row.get("item_code"),
                (String) row.get("lot_no"),
                toInt(row.get("item_qty")),
                (String) row.get("loc_id"));
    }

    // 출고 후보 N건 일괄 픽업 — excludeLocIds 와 중복 제거.
    @SuppressWarnings({"rawtypes", "unchecked"})
    public List<SimStock> fetchAvailable(String groupId, int limit, Set<String> excludeLocIds) {
        String sql = """
                SELECT s.stock_id, s.item_code, s.lot_no, s.item_qty, l.loc_id
                  FROM tb_inventory_stock s
                  JOIN tb_inventory_location l ON l.stock_id = s.stock_id
                 WHERE l.loc_group = :eqGroupId
                   AND l.task_id IS NULL
                   AND s.lot_no LIKE :lotPrefix
                   AND s.is_enabled = TRUE
                   AND s.item_owner = :owner
                   AND s.item_qty >= 50
                   AND s.stock_status = :stockStatus
                 ORDER BY s.created_at ASC
                """;
        List<Map> rows = queryManager.selectListBySql(sql, simStockParams(groupId), Map.class, 0, limit + 10);
        return rows.stream()
                .map(row -> new SimStock(
                        (String) row.get("stock_id"), (String) row.get("item_code"),
                        (String) row.get("lot_no"), toInt(row.get("item_qty")), (String) row.get("loc_id")))
                .filter(s -> !excludeLocIds.contains(s.locId()))
                .limit(limit)
                .toList();
    }

    // 시뮬 재고 조회 공통 파라미터.
    private Map<String, Object> simStockParams(String groupId) {
        return ValueUtil.newMap("eqGroupId,lotPrefix,owner,stockStatus",
                groupId,
                SimulatorConfig.SIM_LOT_PREFIX + "%",
                SimulatorConfig.OWNER_CODE,
                StockStatus.IDLE.value());
    }

    private static int toInt(Object o) {
        return (o instanceof Number n) ? n.intValue() : 0;
    }
}
