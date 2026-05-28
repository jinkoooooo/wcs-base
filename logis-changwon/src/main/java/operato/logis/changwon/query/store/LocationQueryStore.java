package operato.logis.changwon.query.store;

import org.springframework.stereotype.Component;
import xyz.anythings.sys.service.AbstractQueryStore;
import xyz.elidom.sys.SysConstants;

@Component
public class LocationQueryStore extends AbstractQueryStore {

    @Override
    public void initQueryStore(String databaseType) {
        this.databaseType = databaseType;
        this.basePath = "operato/logis/changwon/query/" + this.databaseType + SysConstants.SLASH;
        this.defaultBasePath = "operato/logis/changwon/query/ansi/";
    }

    /**
     * 입고 로케이션 추천
     */
    public String getRecommendInboundLocationSql() {
        return this.getQueryByPath("location/CalculateInboundLocation");
    }

    /**
     * 정렬 로케이션 추천
     */
    public String getRecommendSortLocationSql() {
        return this.getQueryByPath("location/CalculateSortLocation");
    }

    /**
     * 강제 입고 로케이션 추천
     */
    public String getRecommendForceInboundLocationSql() {
        return this.getQueryByPath("location/CalculateForceInboundLocation");
    }
}