package operato.logis.inventory.service;

import lombok.RequiredArgsConstructor;
import operato.logis.inventory.dto.StockMetricsDto;
import operato.logis.inventory.query.InventoryQueryStore;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class InventoryStockService extends AbstractQueryService {

    private final InventoryQueryStore inventoryQueryStore;

    /**
     * Pallet ID에 해당하는 총 무게, 총 높이 조회
     *
     * @param stockId 조회할 대상의 Pallet ID
     * @return 해당 재고의 총 무게, 총 높이 정보
     */
    public StockMetricsDto getTotalStockMetrics(String stockId) {
        String sql = """
                SELECT
                    S.STOCK_ID,
                    SUM(S.ITEM_QTY * M.ITEM_WEIGHT) AS TOTAL_WEIGHT,
                    SUM(S.STOCK_HEIGHT) AS TOTAL_HEIGHT
                FROM
                    TB_INVENTORY_STOCK S
                INNER JOIN
                    TB_INVENTORY_ITEM_MST M ON S.ITEM_OWNER = M.ITEM_OWNER
                                               AND S.ITEM_CODE = M.ITEM_CODE
                WHERE
                    S.STOCK_ID = :stockId
                GROUP BY
                    S.STOCK_ID;
                """;
        Map<String, Object> param = ValueUtil.newMap("stockId", stockId);

        return this.queryManager.selectBySql(sql, param, StockMetricsDto.class);
    }
}
