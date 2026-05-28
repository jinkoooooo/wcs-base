package operato.logis.posco.query;

import org.springframework.stereotype.Component;

import xyz.anythings.sys.service.AbstractQueryStore;
import xyz.elidom.sys.SysConstants;

/**
 * posco 쿼리
 * 
 * @author shortstop
 */
@Component
public class PoscoQueryStore extends AbstractQueryStore {

	@Override
	public void initQueryStore(String databaseType) {
		this.databaseType = databaseType;
		this.basePath = "operato/logis/posco/query/" + this.databaseType + SysConstants.SLASH;
		this.defaultBasePath = "operato/logis/posco/query/ansi/";
	}
}