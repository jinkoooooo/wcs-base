package operato.logis.ecs.base.wcs.query;

import org.springframework.stereotype.Component;
import xyz.anythings.sys.service.AbstractQueryStore;
import xyz.elidom.sys.SysConstants;

/**
 * WCS 쿼리
 *
 * @author shortstop
 */
@Component
public class EcsBaseQueryStore extends AbstractQueryStore {

    @Override
    public void initQueryStore(String databaseType) {
        this.databaseType = databaseType;
        this.basePath = "operato/logis/ecs/base/wcs/query/" + this.databaseType + SysConstants.SLASH;
        this.defaultBasePath = "operato/logis/ecs/base/wcs/query/ansi/";
    }
}