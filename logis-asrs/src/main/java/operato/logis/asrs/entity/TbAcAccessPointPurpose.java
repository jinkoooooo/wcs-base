package operato.logis.asrs.entity;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;

@Table(name = "tb_ac_access_point_purpose", idStrategy = GenerationRule.UUID)
public class TbAcAccessPointPurpose extends xyz.elidom.orm.entity.basic.ElidomStampHook {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 736602218224651238L;

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;

	@Column (name = "access_point_id", nullable = false, length = 40)
	private String accessPointId;

	@Column (name = "purpose_code", nullable = false, length = 30)
	private String purposeCode;

	@Column (name = "priority_no", nullable = false)
	private Integer priorityNo;

	@Column (name = "active_yn", nullable = false, length = 1)
	private String activeYn;

	@Column (name = "description", length = 500)
	private String description;
  
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getAccessPointId() {
		return accessPointId;
	}

	public void setAccessPointId(String accessPointId) {
		this.accessPointId = accessPointId;
	}

	public String getPurposeCode() {
		return purposeCode;
	}

	public void setPurposeCode(String purposeCode) {
		this.purposeCode = purposeCode;
	}

	public Integer getPriorityNo() {
		return priorityNo;
	}

	public void setPriorityNo(Integer priorityNo) {
		this.priorityNo = priorityNo;
	}

	public String getActiveYn() {
		return activeYn;
	}

	public void setActiveYn(String activeYn) {
		this.activeYn = activeYn;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}	
}
