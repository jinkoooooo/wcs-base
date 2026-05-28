package operato.logis.kmat_2026.query;

import org.springframework.stereotype.Component;

import xyz.anythings.sys.service.AbstractQueryStore;
import xyz.elidom.sys.SysConstants;

/**
 * KMat2026 쿼리
 * 
 * @author shortstop
 */
@Component
public class KMat2026QueryStore extends AbstractQueryStore {

	@Override
	public void initQueryStore(String databaseType) {
		this.databaseType = databaseType;
		this.basePath = "operato/logis/kmat_2026/query/" + this.databaseType + SysConstants.SLASH;
		this.defaultBasePath = "operato/logis/kmat_2026/query/ansi/";
	}
}