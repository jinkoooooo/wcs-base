package operato.logis.asrs.entity;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;

@Table(name = "tb_ac_crane", idStrategy = GenerationRule.UUID)
public class TbAcCrane extends xyz.elidom.orm.entity.basic.ElidomStampHook {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 588341897436031845L;

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;

	@Column (name = "area_id", nullable = false, length = 40)
	private String areaId;

	@Column (name = "crane_code", nullable = false, length = 30)
	private String craneCode;

	@Column (name = "crane_name", nullable = false, length = 100)
	private String craneName;

	@Column (name = "aisle_from", nullable = false)
	private Integer aisleFrom;

	@Column (name = "aisle_to", nullable = false)
	private Integer aisleTo;

	@Column (name = "side_scope", nullable = false, length = 20)
	private String sideScope;

	@Column (name = "status_use_yn", nullable = false, length = 10)
	private String statusUseYn;
  
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getAreaId() {
		return areaId;
	}

	public void setAreaId(String areaId) {
		this.areaId = areaId;
	}

	public String getCraneCode() {
		return craneCode;
	}

	public void setCraneCode(String craneCode) {
		this.craneCode = craneCode;
	}

	public String getCraneName() {
		return craneName;
	}

	public void setCraneName(String craneName) {
		this.craneName = craneName;
	}

	public Integer getAisleFrom() {
		return aisleFrom;
	}

	public void setAisleFrom(Integer aisleFrom) {
		this.aisleFrom = aisleFrom;
	}

	public Integer getAisleTo() {
		return aisleTo;
	}

	public void setAisleTo(Integer aisleTo) {
		this.aisleTo = aisleTo;
	}

	public String getSideScope() {
		return sideScope;
	}

	public void setSideScope(String sideScope) {
		this.sideScope = sideScope;
	}

	public String getStatusUseYn() {
		return statusUseYn;
	}

	public void setStatusUseYn(String statusUseYn) {
		this.statusUseYn = statusUseYn;
	}	
}
