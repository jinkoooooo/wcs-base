package xyz.anyware.wcs.query.store;

import org.springframework.stereotype.Component;

import xyz.anythings.sys.service.AbstractQueryStore;
import xyz.elidom.sys.SysConstants;

/**
 * DPS 할당 관련 쿼리 스토어
 * 
 * @author shortstop
 */
@Component
public class VueQueryStore extends AbstractQueryStore {

	@Override
	public void initQueryStore(String databaseType) {
		this.databaseType = databaseType;

		this.basePath = "xyz/anyware/wcs/query/" + this.databaseType + SysConstants.SLASH;
		this.defaultBasePath = "xyz/anyware/wcs/query/ansi/";
	}

	/**
	 * VueMenu List 조회
	 * 
	 * @return
	 */
	public String getSystemRouterMenuQuery() {
		return this.getQueryByPath("system/SystemRouterMenu");
	}

	public String getFormColumnQuery() {
		return this.getQueryByPath("system/FormColumn");
	}

	public String getMenuColumnQuery() {
		return this.getQueryByPath("system/MenuColumn");
	}

	public String getSearchColumnQuery() {
		return this.getQueryByPath("system/SearchColumn");
	}

	public String getSystemMenuListQuery() {
		return this.getQueryByPath("system/SystemMenuList");
	}

	public String getToastMenuColumnQuery() {
		return this.getQueryByPath("system/ToastMenuColumn");
	}
}
