package operato.logis.asrs.query;

import org.springframework.stereotype.Component;

import xyz.anythings.sys.service.AbstractQueryStore;
import xyz.elidom.sys.SysConstants;

/**
 * WCS 쿼리
 * 
 * @author shortstop
 */
@Component
public class AsrsQueryStore extends AbstractQueryStore {

	@Override
	public void initQueryStore(String databaseType) {
		this.databaseType = databaseType;
		this.basePath = "operato/logis/asrs/query/" + this.databaseType + SysConstants.SLASH;
		this.defaultBasePath = "operato/logis/asrs/query/ansi/";
	}
}