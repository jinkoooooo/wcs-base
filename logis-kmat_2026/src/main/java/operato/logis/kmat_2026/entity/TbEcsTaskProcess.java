package operato.logis.kmat_2026.entity;


import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;

import java.util.Date;

@Table(name = "tb_ecs_task_process", idStrategy = GenerationRule.UUID)
public class TbEcsTaskProcess extends xyz.elidom.orm.entity.basic.ElidomStampHook {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 256856481675802057L;

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;

	@Column (name = "req_type", length = 50)
	private String reqType;

	@Column (name = "req_cod", length = 50)
	private String reqCod;

	@Column (name = "req_time", length = 50)
	private Date reqTime;

	@Column (name = "order_id", length = 50)
	private String orderId;

	@Column (name = "task_id", length = 50)
	private String taskId;

	@Column (name = "current_position_cod", length = 50)
	private String currentPositionCod;

	@Column (name = "to_position_cod", length = 50)
	private String toPositionCod;

	@Column (name = "status_list", length = 50)
	private String statusList;

	@Column (name = "event_type", length = 50)
	private String eventType;

	@Column (name = "equip_id", length = 50)
	private String equipId;

	@Column (name = "task_status", length = 50)
	private Integer taskStatus;

	@Column (name = "cbk_status", length = 3)
	private String cbkStatus;

	@Column(name = "cbk_status_desc", length = 200)
	private String cbkStatusDesc;

	@Column (name = "equip_status", length = 50)
	private String equipStatus;

	@Column (name = "equip_type", length = 50)
	private String equipType;

	@Column (name = "request_yn", length = 1)
	private Boolean requestYn;

	@Column (name = "error_code", length = 200)
	private String errorCode;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getReqType() {
		return reqType;
	}

	public void setReqType(String reqType) {
		this.reqType = reqType;
	}

	public String getReqCod() {
		return reqCod;
	}

	public void setReqCod(String reqCod) {
		this.reqCod = reqCod;
	}

	public Date getReqTime() {
		return reqTime;
	}

	public void setReqTime(Date reqTime) {
		this.reqTime = reqTime;
	}

	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public String getTaskId() {
		return taskId;
	}

	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}

	public String getCurrentPositionCod() {
		return currentPositionCod;
	}

	public void setCurrentPositionCod(String currentPositionCod) {
		this.currentPositionCod = currentPositionCod;
	}

	public String getToPositionCod() {
		return toPositionCod;
	}

	public void setToPositionCod(String toPositionCod) {
		this.toPositionCod = toPositionCod;
	}

	public String getStatusList() {
		return statusList;
	}

	public void setStatusList(String statusList) {
		this.statusList = statusList;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public String getEquipId() {
		return equipId;
	}

	public void setEquipId(String equipId) {
		this.equipId = equipId;
	}

	public Integer getTaskStatus() {
		return taskStatus;
	}

	public void setTaskStatus(Integer taskStatus) {
		this.taskStatus = taskStatus;
	}

	public String getCbkStatus() {
		return cbkStatus;
	}

	public void setCbkStatus(String cbkStatus) {
		this.cbkStatus = cbkStatus;
	}

	public String getCbkStatusDesc() {
		return cbkStatusDesc;
	}

	public void setCbkStatusDesc(String cbkStatusDesc) {
		this.cbkStatusDesc = cbkStatusDesc;
	}

	public String getEquipStatus() {
		return equipStatus;
	}

	public void setEquipStatus(String equipStatus) {
		this.equipStatus = equipStatus;
	}

	public Boolean getRequestYn() {
		return requestYn;
	}

	public void setRequestYn(Boolean requestYn) {
		this.requestYn = requestYn;
	}

	public String getEquipType() {
		return equipType;
	}

	public void setEquipType(String equipType) {
		this.equipType = equipType;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}
}
