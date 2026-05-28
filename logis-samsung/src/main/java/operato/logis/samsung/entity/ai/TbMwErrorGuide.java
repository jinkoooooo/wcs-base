package operato.logis.samsung.entity.ai;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;

@Table(name = "tb_mw_error_guide", idStrategy = GenerationRule.UUID)
public class TbMwErrorGuide extends xyz.elidom.orm.entity.basic.ElidomStampHook {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 427179156451695813L;

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;

	@Column (name = "error_code", nullable = false, length = 30)
	private String errorCode;

	@Column (name = "action_steps_json", length = 5000)
	private String actionStepsJson;

	@Column (name = "unit_type", length = 30)
	private String unitType;

	@Column (name = "error_name", length = 200)
	private String errorName;

	@Column (name = "error_desc", length = 1000)
	private String errorDesc;

	@Column (name = "main_cause", length = 1000)
	private String mainCause;

	@Column (name = "severity", length = 20)
	private String severity;

	@Column (name = "manual_required_yn", length = 10)
	private String manualRequiredYn;

	@Column (name = "active_yn", length = 10)
	private String activeYn;
  
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	public String getActionStepsJson() {
		return actionStepsJson;
	}

	public void setActionStepsJson(String actionStepsJson) {
		this.actionStepsJson = actionStepsJson;
	}

	public String getUnitType() {
		return unitType;
	}

	public void setUnitType(String unitType) {
		this.unitType = unitType;
	}

	public String getErrorName() {
		return errorName;
	}

	public void setErrorName(String errorName) {
		this.errorName = errorName;
	}

	public String getErrorDesc() {
		return errorDesc;
	}

	public void setErrorDesc(String errorDesc) {
		this.errorDesc = errorDesc;
	}

	public String getMainCause() {
		return mainCause;
	}

	public void setMainCause(String mainCause) {
		this.mainCause = mainCause;
	}

	public String getSeverity() {
		return severity;
	}

	public void setSeverity(String severity) {
		this.severity = severity;
	}

	public String getManualRequiredYn() {
		return manualRequiredYn;
	}

	public void setManualRequiredYn(String manualRequiredYn) {
		this.manualRequiredYn = manualRequiredYn;
	}

	public String getActiveYn() {
		return activeYn;
	}

	public void setActiveYn(String activeYn) {
		this.activeYn = activeYn;
	}	
}
