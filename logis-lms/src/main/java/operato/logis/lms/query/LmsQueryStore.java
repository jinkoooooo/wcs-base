package operato.logis.lms.query;

import org.springframework.stereotype.Component;
import xyz.anythings.sys.service.AbstractQueryStore;
import xyz.elidom.sys.SysConstants;

/**
 * LMS 쿼리
 * 
 * @author shortstop
 */
@Component
public class LmsQueryStore extends AbstractQueryStore {

	@Override
	public void initQueryStore(String databaseType) {
		this.databaseType = databaseType;
		this.basePath = "operato/logis/lms/query/" + this.databaseType + SysConstants.SLASH;
		this.defaultBasePath = "operato/logis/lms/query/ansi/";
	}
}