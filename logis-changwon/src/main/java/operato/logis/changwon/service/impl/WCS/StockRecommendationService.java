package operato.logis.changwon.service.impl.WCS;

import lombok.RequiredArgsConstructor;
import operato.logis.changwon.entity.WCS.WcsStockAuto;
import operato.logis.changwon.query.store.InventoryQueryStore;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class StockRecommendationService extends AbstractQueryService {

    private final InventoryQueryStore stockQueryStore;

    /**
     * WCS 출고 재고 추천
     */
    public WcsStockAuto getOutboundStock(String itemCode, String itemOwner) {
        String sql = stockQueryStore.getRecommendOutboundStockSql();
        Map<String, Object> param = ValueUtil.newMap("itemCode,itemOwner", itemCode, itemOwner);
        return this.queryManager.selectBySql(sql, param, WcsStockAuto.class);
    }
}