package operato.logis.asrs.entity;

import java.util.Date; 
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;

@Table(name = "tb_ac_grade_policy", idStrategy = GenerationRule.UUID)
public class TbAcGradePolicy extends xyz.elidom.orm.entity.basic.ElidomStampHook {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 323944158465861835L;

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;

	@Column (name = "policy_code", nullable = false, length = 30)
	private String policyCode;

	@Column (name = "policy_name", nullable = false, length = 100)
	private String policyName;

	@Column (name = "apply_mode", nullable = false, length = 20)
	private String applyMode;

	@Column (name = "grade_scheme", nullable = false, length = 20)
	private String gradeScheme;

	@Column (name = "score_formula_json", nullable = false, length = 4000)
	private String scoreFormulaJson;

	@Column (name = "effective_from")
	private Date effectiveFrom;

	@Column (name = "effective_to", nullable = false)
	private Date effectiveTo;

	@Column (name = "active_yn", nullable = false, length = 10)
	private String activeYn;
  
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getPolicyCode() {
		return policyCode;
	}

	public void setPolicyCode(String policyCode) {
		this.policyCode = policyCode;
	}

	public String getPolicyName() {
		return policyName;
	}

	public void setPolicyName(String policyName) {
		this.policyName = policyName;
	}

	public String getApplyMode() {
		return applyMode;
	}

	public void setApplyMode(String applyMode) {
		this.applyMode = applyMode;
	}

	public String getGradeScheme() {
		return gradeScheme;
	}

	public void setGradeScheme(String gradeScheme) {
		this.gradeScheme = gradeScheme;
	}

	public String getScoreFormulaJson() {
		return scoreFormulaJson;
	}

	public void setScoreFormulaJson(String scoreFormulaJson) {
		this.scoreFormulaJson = scoreFormulaJson;
	}

	public Date getEffectiveFrom() {
		return effectiveFrom;
	}

	public void setEffectiveFrom(Date effectiveFrom) {
		this.effectiveFrom = effectiveFrom;
	}

	public Date getEffectiveTo() {
		return effectiveTo;
	}

	public void setEffectiveTo(Date effectiveTo) {
		this.effectiveTo = effectiveTo;
	}

	public String getActiveYn() {
		return activeYn;
	}

	public void setActiveYn(String activeYn) {
		this.activeYn = activeYn;
	}	
}
