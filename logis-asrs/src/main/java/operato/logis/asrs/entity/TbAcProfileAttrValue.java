package operato.logis.asrs.entity;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;

@Table(name = "tb_ac_profile_attr_value", idStrategy = GenerationRule.UUID)
public class TbAcProfileAttrValue extends xyz.elidom.orm.entity.basic.ElidomStampHook {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 475127745975773913L;

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;

	@Column (name = "operation_profile_id", nullable = false, length = 40)
	private String operationProfileId;

	@Column (name = "attr_def_id", nullable = false, length = 40)
	private String attrDefId;

	@Column (name = "attr_value", nullable = false, length = 500)
	private String attrValue;

	@Column (name = "active_yn", nullable = false, length = 10)
	private String activeYn;
  
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getOperationProfileId() {
		return operationProfileId;
	}

	public void setOperationProfileId(String operationProfileId) {
		this.operationProfileId = operationProfileId;
	}

	public String getAttrDefId() {
		return attrDefId;
	}

	public void setAttrDefId(String attrDefId) {
		this.attrDefId = attrDefId;
	}

	public String getAttrValue() {
		return attrValue;
	}

	public void setAttrValue(String attrValue) {
		this.attrValue = attrValue;
	}

	public String getActiveYn() {
		return activeYn;
	}

	public void setActiveYn(String activeYn) {
		this.activeYn = activeYn;
	}	
}
