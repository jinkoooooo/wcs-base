package operato.logis.changwon.query.store;

import org.springframework.stereotype.Component;
import xyz.anythings.sys.service.AbstractQueryStore;
import xyz.elidom.sys.SysConstants;

@Component
public class TaskQueryStore extends AbstractQueryStore {

    @Override
    public void initQueryStore(String databaseType) {
        this.databaseType = databaseType;
        this.basePath = "operato/logis/changwon/query/" + this.databaseType + SysConstants.SLASH;
        this.defaultBasePath = "operato/logis/changwon/query/ansi/";
    }

    /**
     * 입고, 출고 충돌 방지
     */
    public String getSelectInboundOutboundMixingSql() {
        return this.getQueryByPath("task/SelectInboundOutboundMixing");
    }
}
