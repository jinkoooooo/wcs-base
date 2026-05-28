package operato.logis.asrs.entity;

import java.util.Date; 
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;

@Table(name = "tb_ac_relocation_task", idStrategy = GenerationRule.UUID)
public class TbAcRelocationTask extends xyz.elidom.orm.entity.basic.ElidomStampHook {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 207743520817746201L;

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;

	@Column (name = "strategy_run_id", nullable = false, length = 40)
	private String strategyRunId;

	@Column (name = "task_type", nullable = false, length = 20)
	private String taskType;

	@Column (name = "priority", nullable = false)
	private Integer priority;

	@Column (name = "item_id", nullable = false, length = 40)
	private String itemId;

	@Column (name = "stock_unit_id", nullable = false, length = 40)
	private String stockUnitId;

	@Column (name = "source_location_id", nullable = false, length = 40)
	private String sourceLocationId;

	@Column (name = "target_location_id", nullable = false, length = 40)
	private String targetLocationId;

	@Column (name = "reason_code", nullable = false, length = 30)
	private String reasonCode;

	@Column (name = "reason_detail_json", length = 4000)
	private String reasonDetailJson;

	@Column (name = "task_status_code", nullable = false, length = 20)
	private String taskStatusCode;

	@Column (name = "confirmed_at", type = xyz.elidom.dbist.annotation.ColumnType.DATETIME)
	private Date confirmedAt;
  
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getStrategyRunId() {
		return strategyRunId;
	}

	public void setStrategyRunId(String strategyRunId) {
		this.strategyRunId = strategyRunId;
	}

	public String getTaskType() {
		return taskType;
	}

	public void setTaskType(String taskType) {
		this.taskType = taskType;
	}

	public Integer getPriority() {
		return priority;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	public String getItemId() {
		return itemId;
	}

	public void setItemId(String itemId) {
		this.itemId = itemId;
	}

	public String getStockUnitId() {
		return stockUnitId;
	}

	public void setStockUnitId(String stockUnitId) {
		this.stockUnitId = stockUnitId;
	}

	public String getSourceLocationId() {
		return sourceLocationId;
	}

	public void setSourceLocationId(String sourceLocationId) {
		this.sourceLocationId = sourceLocationId;
	}

	public String getTargetLocationId() {
		return targetLocationId;
	}

	public void setTargetLocationId(String targetLocationId) {
		this.targetLocationId = targetLocationId;
	}

	public String getReasonCode() {
		return reasonCode;
	}

	public void setReasonCode(String reasonCode) {
		this.reasonCode = reasonCode;
	}

	public String getReasonDetailJson() {
		return reasonDetailJson;
	}

	public void setReasonDetailJson(String reasonDetailJson) {
		this.reasonDetailJson = reasonDetailJson;
	}

	public String getTaskStatusCode() {
		return taskStatusCode;
	}

	public void setTaskStatusCode(String taskStatusCode) {
		this.taskStatusCode = taskStatusCode;
	}

	public Date getConfirmedAt() {
		return confirmedAt;
	}

	public void setConfirmedAt(Date confirmedAt) {
		this.confirmedAt = confirmedAt;
	}	
}
