package operato.logis.asrs.entity;

import java.util.Date; 
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;

@Table(name = "tb_ac_item_grade", idStrategy = GenerationRule.UUID)
public class TbAcItemGrade extends xyz.elidom.orm.entity.basic.ElidomStampHook {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 664897295680597126L;

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;

	@Column (name = "area_id", nullable = false, length = 40)
	private String areaId;

	@Column (name = "item_id", nullable = false, length = 40)
	private String itemId;

	@Column (name = "grade_policy_id", nullable = false, length = 40)
	private String gradePolicyId;

	@Column (name = "manual_seed_grade", nullable = false, length = 10)
	private String manualSeedGrade;

	@Column (name = "manual_seed_score", nullable = false)
	private Integer manualSeedScore;

	@Column (name = "learned_score", nullable = false)
	private Integer learnedScore;

	@Column (name = "final_score", nullable = false)
	private Integer finalScore;

	@Column (name = "current_grade", nullable = false, length = 10)
	private String currentGrade;

	@Column (name = "last_calculated_at", nullable = false, type = xyz.elidom.dbist.annotation.ColumnType.DATETIME)
	private Date lastCalculatedAt;
  
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

	public String getItemId() {
		return itemId;
	}

	public void setItemId(String itemId) {
		this.itemId = itemId;
	}

	public String getGradePolicyId() {
		return gradePolicyId;
	}

	public void setGradePolicyId(String gradePolicyId) {
		this.gradePolicyId = gradePolicyId;
	}

	public String getManualSeedGrade() {
		return manualSeedGrade;
	}

	public void setManualSeedGrade(String manualSeedGrade) {
		this.manualSeedGrade = manualSeedGrade;
	}

	public Integer getManualSeedScore() {
		return manualSeedScore;
	}

	public void setManualSeedScore(Integer manualSeedScore) {
		this.manualSeedScore = manualSeedScore;
	}

	public Integer getLearnedScore() {
		return learnedScore;
	}

	public void setLearnedScore(Integer learnedScore) {
		this.learnedScore = learnedScore;
	}

	public Integer getFinalScore() {
		return finalScore;
	}

	public void setFinalScore(Integer finalScore) {
		this.finalScore = finalScore;
	}

	public String getCurrentGrade() {
		return currentGrade;
	}

	public void setCurrentGrade(String currentGrade) {
		this.currentGrade = currentGrade;
	}

	public Date getLastCalculatedAt() {
		return lastCalculatedAt;
	}

	public void setLastCalculatedAt(Date lastCalculatedAt) {
		this.lastCalculatedAt = lastCalculatedAt;
	}	
}
