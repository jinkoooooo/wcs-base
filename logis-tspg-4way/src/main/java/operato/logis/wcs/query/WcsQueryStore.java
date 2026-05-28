package operato.logis.wcs.query;

import org.springframework.stereotype.Component;

import xyz.anythings.sys.service.AbstractQueryStore;
import xyz.elidom.sys.SysConstants;

/**
 * WCS XML 쿼리 스토어. DB 타입별 쿼리 base path 를 구성한다.
 */
@Component
public class WcsQueryStore extends AbstractQueryStore {

	/** DB 타입별 쿼리 경로 초기화. ansi 를 기본 fallback 으로 둔다. */
	@Override
	public void initQueryStore(String databaseType) {
		this.databaseType = databaseType;
		this.basePath = "operato/logis/wcs/query/" + this.databaseType + SysConstants.SLASH;
		this.defaultBasePath = "operato/logis/wcs/query/ansi/";
	}
}