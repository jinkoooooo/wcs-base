package operato.logis.asrs.entity;

import java.util.Date; 
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;

@Table(name = "tb_ac_strategy_run", idStrategy = GenerationRule.UUID)
public class TbAcStrategyRun extends xyz.elidom.orm.entity.basic.ElidomStampHook {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 136023919730403848L;

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;

	@Column (name = "strategy_set_id", nullable = false, length = 40)
	private String strategySetId;

	@Column (name = "run_type", nullable = false, length = 20)
	private String runType;

	@Column (name = "run_status_code", nullable = false, length = 20)
	private String runStatusCode;

	@Column (name = "run_started_at", nullable = false, type = xyz.elidom.dbist.annotation.ColumnType.DATETIME)
	private Date runStartedAt;

	@Column (name = "run_ended_at", type = xyz.elidom.dbist.annotation.ColumnType.DATETIME)
	private Date runEndedAt;

	@Column (name = "result_summary_json", length = 4000)
	private String resultSummaryJson;

	@Column (name = "triggered_by", nullable = false, length = 32)
	private String triggeredBy;
  
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getStrategySetId() {
		return strategySetId;
	}

	public void setStrategySetId(String strategySetId) {
		this.strategySetId = strategySetId;
	}

	public String getRunType() {
		return runType;
	}

	public void setRunType(String runType) {
		this.runType = runType;
	}

	public String getRunStatusCode() {
		return runStatusCode;
	}

	public void setRunStatusCode(String runStatusCode) {
		this.runStatusCode = runStatusCode;
	}

	public Date getRunStartedAt() {
		return runStartedAt;
	}

	public void setRunStartedAt(Date runStartedAt) {
		this.runStartedAt = runStartedAt;
	}

	public Date getRunEndedAt() {
		return runEndedAt;
	}

	public void setRunEndedAt(Date runEndedAt) {
		this.runEndedAt = runEndedAt;
	}

	public String getResultSummaryJson() {
		return resultSummaryJson;
	}

	public void setResultSummaryJson(String resultSummaryJson) {
		this.resultSummaryJson = resultSummaryJson;
	}

	public String getTriggeredBy() {
		return triggeredBy;
	}

	public void setTriggeredBy(String triggeredBy) {
		this.triggeredBy = triggeredBy;
	}	
}
