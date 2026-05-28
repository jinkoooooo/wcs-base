package operato.logis.asrs.entity;

import java.util.Date; 
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;

@Table(name = "tb_ac_item_grade_hist", idStrategy = GenerationRule.UUID)
public class TbAcItemGradeHist extends xyz.elidom.orm.entity.basic.ElidomStampHook {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 486248366013202199L;

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;

	@Column (name = "item_grade_id", nullable = false, length = 40)
	private String itemGradeId;

	@Column (name = "grade_policy_id", nullable = false, length = 40)
	private String gradePolicyId;

	@Column (name = "previous_grade", length = 10)
	private String previousGrade;

	@Column (name = "new_grade", nullable = false, length = 10)
	private String newGrade;

	@Column (name = "previous_score")
	private Integer previousScore;

	@Column (name = "new_score", nullable = false)
	private Integer newScore;

	@Column (name = "reason_json", length = 4000)
	private String reasonJson;

	@Column (name = "calculated_at", nullable = false, type = xyz.elidom.dbist.annotation.ColumnType.DATETIME)
	private Date calculatedAt;
  
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getItemGradeId() {
		return itemGradeId;
	}

	public void setItemGradeId(String itemGradeId) {
		this.itemGradeId = itemGradeId;
	}

	public String getGradePolicyId() {
		return gradePolicyId;
	}

	public void setGradePolicyId(String gradePolicyId) {
		this.gradePolicyId = gradePolicyId;
	}

	public String getPreviousGrade() {
		return previousGrade;
	}

	public void setPreviousGrade(String previousGrade) {
		this.previousGrade = previousGrade;
	}

	public String getNewGrade() {
		return newGrade;
	}

	public void setNewGrade(String newGrade) {
		this.newGrade = newGrade;
	}

	public Integer getPreviousScore() {
		return previousScore;
	}

	public void setPreviousScore(Integer previousScore) {
		this.previousScore = previousScore;
	}

	public Integer getNewScore() {
		return newScore;
	}

	public void setNewScore(Integer newScore) {
		this.newScore = newScore;
	}

	public String getReasonJson() {
		return reasonJson;
	}

	public void setReasonJson(String reasonJson) {
		this.reasonJson = reasonJson;
	}

	public Date getCalculatedAt() {
		return calculatedAt;
	}

	public void setCalculatedAt(Date calculatedAt) {
		this.calculatedAt = calculatedAt;
	}	
}
