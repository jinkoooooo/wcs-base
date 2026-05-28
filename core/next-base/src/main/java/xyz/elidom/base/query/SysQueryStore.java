package xyz.elidom.base.query;

import org.springframework.stereotype.Component;
import xyz.elidom.sys.SysConstants;

/**
 * 출고용 쿼리 스토어
 * 
 * @author shortstop
 */
@Component
public class SysQueryStore extends AbstractSysQueryStore {
	@Override
	public void initQueryStore(String databaseType) {
		this.databaseType = databaseType;
		this.basePath = "xyz/elidom/base/query/" + this.databaseType + SysConstants.SLASH;
		this.defaultBasePath = "xyz/elidom/base/query/postgresql/";
	}
	
	public String getInsertMenuQuery() {
		return this.getQueryByPath("provisioning/InsertMenu");
	}
	
	public String getUpdateMenuParentIdQuery() {
	    return this.getQueryByPath("provisioning/UpdateMenuParentId");
	}
	
	public String getParentMenuListQuery() {
	    return this.getQueryByPath("provisioning/ParentMenuList");
	}
	
	public String getMenuColumnsInsertQuery() {
	    return this.getQueryByPath("provisioning/MenuColumnsInsert");
	}
	
	public String getMenuButtonsInsertQuery() {
	    return this.getQueryByPath("provisioning/MenuButtonsInsert");
	}
	
	public String getMenuDetailsInsertQuery() {
	    return this.getQueryByPath("provisioning/MenuDetailsInsert");
	}
	
	public String getMenuDetailColumnsInsertQuery() {
	    return this.getQueryByPath("provisioning/MenuDetailColumnsInsert");
	}
	
	public String getMenuDetailButtonsInsertQuery() {
	    return this.getQueryByPath("provisioning/MenuDetailButtonsInsert");
	}
	
	public String getPermissionsInsertQuery() {
	    return this.getQueryByPath("provisioning/PermissionsInsert");
	}
	
	public String getUserActiveFlagUpdateQuery() {
	    return this.getQueryByPath("provisioning/UserActiveFlagUpdate");
	}
	
	public String getInsertIndConfigSet() {
	    return this.getQueryByPath("provisioning/InsertIndConfigSet");
	}
	
	public String getInsertJobConfigSet() {
	    return this.getQueryByPath("provisioning/InsertJobConfigSet");
	}
	
	public String getInsertDeviceProfilesSet() {
	    return this.getQueryByPath("provisioning/InsertDeviceProfiles");
	}
	
	public String getInsertIndConfigs() {
	    return this.getQueryByPath("provisioning/InsertIndConfigs");
	}
	
	public String getInsertJobConfigs() {
	    return this.getQueryByPath("provisioning/InsertJobConfigs");
	}
	
	public String getInsertDeviceConfigs() {
	    return this.getQueryByPath("provisioning/InsertDeviceConfigs");
	}
	
	public String getQueryMenuDetail() {
		return this.getQueryByPath("provisioning/QueryMenuDetail");
	}
	
	public String getQueryMenuDetailColumn() {
		return this.getQueryByPath("provisioning/QueryMenuDetailColumn");
	}
	
	public String getQueryMenuDetailButton() {
		return this.getQueryByPath("provisioning/QueryMenuDetailButton");
	}
	
	public String getGetMaxDomainId() {
		return this.getQueryByPath("provisioning/GetMaxDomainId");
	}
}