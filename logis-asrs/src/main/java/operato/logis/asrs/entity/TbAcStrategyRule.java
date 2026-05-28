package operato.logis.asrs.entity;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;

@Table(name = "tb_ac_strategy_rule", idStrategy = GenerationRule.UUID)
public class TbAcStrategyRule extends xyz.elidom.orm.entity.basic.ElidomStampHook {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 714847656838503174L;

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;

	@Column (name = "strategy_set_id", nullable = false, length = 40)
	private String strategySetId;

	@Column (name = "rule_type", nullable = false, length = 30)
	private String ruleType;

	@Column (name = "priority_no", nullable = false)
	private Integer priorityNo;

	@Column (name = "enabled_yn", nullable = false, length = 10)
	private String enabledYn;

	@Column (name = "condition_json", nullable = false, length = 4000)
	private String conditionJson;

	@Column (name = "action_json", nullable = false, length = 4000)
	private String actionJson;

	@Column (name = "active_yn", nullable = false, length = 10)
	private String activeYn;
  
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

	public String getRuleType() {
		return ruleType;
	}

	public void setRuleType(String ruleType) {
		this.ruleType = ruleType;
	}

	public Integer getPriorityNo() {
		return priorityNo;
	}

	public void setPriorityNo(Integer priorityNo) {
		this.priorityNo = priorityNo;
	}

	public String getEnabledYn() {
		return enabledYn;
	}

	public void setEnabledYn(String enabledYn) {
		this.enabledYn = enabledYn;
	}

	public String getConditionJson() {
		return conditionJson;
	}

	public void setConditionJson(String conditionJson) {
		this.conditionJson = conditionJson;
	}

	public String getActionJson() {
		return actionJson;
	}

	public void setActionJson(String actionJson) {
		this.actionJson = actionJson;
	}

	public String getActiveYn() {
		return activeYn;
	}

	public void setActiveYn(String activeYn) {
		this.activeYn = activeYn;
	}	
}
