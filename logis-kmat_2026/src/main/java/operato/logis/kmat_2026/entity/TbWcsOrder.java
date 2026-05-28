package operato.logis.kmat_2026.entity;

import xyz.elidom.dbist.annotation.*;

import java.util.Date;

@Table(name = "tb_wcs_order", idStrategy = GenerationRule.UUID,uniqueFields = "lcId,orderId", indexes = {
        @Index(name = "ix_tb_wcs_order_col_0", columnList = "lc_id,order_id", unique = true)})
public class TbWcsOrder extends xyz.elidom.orm.entity.basic.ElidomStampHook{

    /**
     * SerialVersion UID
     */
    private static final long serialVersionUID = 365604915082340716L;

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column (name = "lc_id", nullable = false, length = 20)
    private String lcId;

    @Column (name = "order_id", nullable = false, length = 30)
    private String orderId;

    @Column (name = "task_id", nullable = false, length = 30)
    private String taskId;

    @Column (name = "from_side", length = 50)
    private String fromSide;

    @Column (name = "to_side", length = 3000)
    private String toSide;

    @Column (name = "status_list", length = 1000)
    private String statusList;

    @Column (name = "task_type", length = 50)
    private String taskType;

    @Column (name = "equip_type", length = 50)
    private String equipType;

    @Column (name = "command_type", length = 50)
    private String commandType;

    @Column (name = "current_step", length = 10)
    private Integer currentStep;

    @Column (name = "cbk_status", length = 3)
    private String cbkStatus;

    @Column (name = "process_status", length = 10)
    private Integer processStatus;

    @Column (name = "accept_datetime", nullable = false, type = ColumnType.DATETIME)
    private Date acceptDatetime;

    @Column (name = "current_position_cod", length = 50)
    private String currentPositionCod;

    @Column (name = "from_position_cod", length = 50)
    private String fromPositionCod;

    @Column (name = "to_position_cod", length = 50)
    private String toPositionCod;

    @Column (name = "priority", length = 50)
    private Integer priority;

    @Column (name = "equip_id", length = 50)
    private String equipId;

    @Column (name = "pod_cd", length = 20)
    private String podCd;

    @Column (name = "comp_datetime", type = ColumnType.DATETIME)
    private Date compDatetime;

    @Column (name = "data_transmit_status", nullable = false, length = 1)
    private Integer dataTransmitStatus;

    @Column (name = "remark", length = 255)
    private String remark;

    @Column (name = "completed", length = 1)
    private Boolean completed;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "error_datetime", type = ColumnType.DATETIME)
    private Date errorDatetime;

    @Column (name = "attribute_1", length = 255)
    private String attribute1;

    @Column (name = "attribute_2", length = 255)
    private String attribute2;

    @Column (name = "attribute_3", length = 255)
    private String attribute3;

    @Column (name = "attribute_4", length = 255)
    private String attribute4;

    public String getLcId() {
        return lcId;
    }

    public void setLcId(String lcId) {
        this.lcId = lcId;
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

    public String getFromSide() {
        return fromSide;
    }

    public void setFromSide(String fromSide) {
        this.fromSide = fromSide;
    }

    public String getToSide() {
        return toSide;
    }

    public void setToSide(String toSide) {
        this.toSide = toSide;
    }

    public String getEquipType() {
        return equipType;
    }

    public void setEquipType(String equipType) {
        this.equipType = equipType;
    }

    public String getCommandType() {
        return commandType;
    }

    public void setCommandType(String commandType) {
        this.commandType = commandType;
    }

    public Integer getProcessStatus() {
        return processStatus;
    }

    public void setProcessStatus(Integer processStatus) {
        this.processStatus = processStatus;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getAttribute1() {
        return attribute1;
    }

    public void setAttribute1(String attribute1) {
        this.attribute1 = attribute1;
    }

    public String getAttribute2() {
        return attribute2;
    }

    public void setAttribute2(String attribute2) {
        this.attribute2 = attribute2;
    }

    public String getAttribute3() {
        return attribute3;
    }

    public void setAttribute3(String attribute3) {
        this.attribute3 = attribute3;
    }

    public String getAttribute4() {
        return attribute4;
    }

    public void setAttribute4(String attribute4) {
        this.attribute4 = attribute4;
    }

    public Date getAcceptDatetime() {
        return acceptDatetime;
    }

    public void setAcceptDatetime(Date acceptDatetime) {
        this.acceptDatetime = acceptDatetime;
    }

    public Date getCompDatetime() {
        return compDatetime;
    }

    public void setCompDatetime(Date compDatetime) {
        this.compDatetime = compDatetime;
    }

    public Integer getDataTransmitStatus() {
        return dataTransmitStatus;
    }

    public void setDataTransmitStatus(Integer dataTransmitStatus) {
        this.dataTransmitStatus = dataTransmitStatus;
    }

    public Integer getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(Integer currentStep) {
        this.currentStep = currentStep;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public String getCurrentPositionCod() {
        return currentPositionCod;
    }

    public void setCurrentPositionCod(String currentPositionCod) {
        this.currentPositionCod = currentPositionCod;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getFromPositionCod() {
        return fromPositionCod;
    }

    public void setFromPositionCod(String fromPositionCod) {
        this.fromPositionCod = fromPositionCod;
    }

    public String getToPositionCod() {
        return toPositionCod;
    }

    public void setToPositionCod(String toPositionCod) {
        this.toPositionCod = toPositionCod;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getEquipId() {
        return equipId;
    }

    public void setEquipId(String equipId) {
        this.equipId = equipId;
    }

    public String getStatusList() {
        return statusList;
    }

    public void setStatusList(String statusList) {
        this.statusList = statusList;
    }

    public String getCbkStatus() {
        return cbkStatus;
    }

    public void setCbkStatus(String cbkStatus) {
        this.cbkStatus = cbkStatus;
    }

    public String getPodCd() {
        return podCd;
    }

    public void setPodCd(String podCd) {
        this.podCd = podCd;
    }

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

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Date getErrorDatetime() {
        return errorDatetime;
    }

    public void setErrorDatetime(Date errorDatetime) {
        this.errorDatetime = errorDatetime;
    }
}
