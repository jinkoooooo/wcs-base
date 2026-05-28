package operato.logis.lms.entity.pm;

import java.util.Date; 
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;

@Table(name = "tb_pm_project_detail_step", idStrategy = GenerationRule.UUID)
public class TbPmProjectDetailStep extends xyz.elidom.orm.entity.basic.ElidomStampHook {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 867982650338796514L;

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;

	@Column (name = "project_main_id", nullable = false, length = 40)
	private String projectMainId;

	@Column (name = "step_cd", nullable = false)
	private Integer stepCd;

	@Column (name = "step_desc", length = 500)
	private String stepDesc;

	@Column (name = "start_date")
	private Date startDate;

	@Column (name = "end_date")
	private Date endDate;

	@Column (name = "duration_days")
	private Integer durationDays;

	@Column (name = "dev1_name", length = 200)
	private String dev1Name;

	@Column (name = "dev2_name", length = 200)
	private String dev2Name;

	@Column (name = "dev3_name", length = 200)
	private String dev3Name;

	@Column (name = "dev4_name", length = 1000)
	private String dev4Name;

	@Column (name = "remark", length = 200)
	private String remark;

	@Column (name = "use_yn", length = 10)
	private String useYn;
  
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getProjectMainId() {
		return projectMainId;
	}

	public void setProjectMainId(String projectMainId) {
		this.projectMainId = projectMainId;
	}

	public Integer getStepCd() {
		return stepCd;
	}

	public void setStepCd(Integer stepCd) {
		this.stepCd = stepCd;
	}

	public String getStepDesc() {
		return stepDesc;
	}

	public void setStepDesc(String stepDesc) {
		this.stepDesc = stepDesc;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public Integer getDurationDays() {
		return durationDays;
	}

	public void setDurationDays(Integer durationDays) {
		this.durationDays = durationDays;
	}

	public String getDev1Name() {
		return dev1Name;
	}

	public void setDev1Name(String dev1Name) {
		this.dev1Name = dev1Name;
	}

	public String getDev2Name() {
		return dev2Name;
	}

	public void setDev2Name(String dev2Name) {
		this.dev2Name = dev2Name;
	}

	public String getDev3Name() {
		return dev3Name;
	}

	public void setDev3Name(String dev3Name) {
		this.dev3Name = dev3Name;
	}

	public String getDev4Name() {
		return dev4Name;
	}

	public void setDev4Name(String dev4Name) {
		this.dev4Name = dev4Name;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public String getUseYn() {
		return useYn;
	}

	public void setUseYn(String useYn) {
		this.useYn = useYn;
	}	
}
