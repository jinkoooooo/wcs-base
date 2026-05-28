package operato.logis.changwon.query.store;

import org.springframework.stereotype.Component;
import xyz.anythings.sys.service.AbstractQueryStore;
import xyz.elidom.sys.SysConstants;

@Component
public class InventoryQueryStore extends AbstractQueryStore {

    @Override
    public void initQueryStore(String databaseType) {
        this.databaseType = databaseType;
        this.basePath = "operato/logis/changwon/query/" + this.databaseType + SysConstants.SLASH;
        this.defaultBasePath = "operato/logis/changwon/query/ansi/";
    }

    /**
     * 출고 재고 추천
     */
    public String getRecommendOutboundStockSql() {
        return this.getQueryByPath("stock/CalculateOutboundStock");
    }
}
