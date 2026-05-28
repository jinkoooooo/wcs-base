package operato.logis.asrs.entity;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;

@Table(name = "tb_ac_storage_area", idStrategy = GenerationRule.UUID)
public class TbAcStorageArea extends xyz.elidom.orm.entity.basic.ElidomStampHook {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 919728891820629721L;

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;

	@Column (name = "center_id", nullable = false, length = 40)
	private String centerId;

	@Column (name = "area_code", nullable = false, length = 30)
	private String areaCode;

	@Column (name = "area_name", nullable = false, length = 100)
	private String areaName;

	@Column (name = "area_type", nullable = false, length = 30)
	private String areaType;

	@Column (name = "operation_profile_id", nullable = false, length = 40)
	private String operationProfileId;

	@Column (name = "description", length = 500)
	private String description;

	@Column (name = "active_yn", nullable = false, length = 10)
	private String activeYn;
  
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCenterId() {
		return centerId;
	}

	public void setCenterId(String centerId) {
		this.centerId = centerId;
	}

	public String getAreaCode() {
		return areaCode;
	}

	public void setAreaCode(String areaCode) {
		this.areaCode = areaCode;
	}

	public String getAreaName() {
		return areaName;
	}

	public void setAreaName(String areaName) {
		this.areaName = areaName;
	}

	public String getAreaType() {
		return areaType;
	}

	public void setAreaType(String areaType) {
		this.areaType = areaType;
	}

	public String getOperationProfileId() {
		return operationProfileId;
	}

	public void setOperationProfileId(String operationProfileId) {
		this.operationProfileId = operationProfileId;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getActiveYn() {
		return activeYn;
	}

	public void setActiveYn(String activeYn) {
		this.activeYn = activeYn;
	}	
}
