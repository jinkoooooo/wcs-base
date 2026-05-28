package operato.logis.lms.entity.monitoring;

import xyz.elidom.dbist.annotation.*;
import xyz.elidom.orm.entity.basic.TimeStampHook;

import java.util.Date;

@Table(name = "lms_alarm_status_dev", idStrategy = GenerationRule.UUID, uniqueFields = "id,alarmId",
        indexes = { @Index(name = "ix_lms_alarm_status_dev_0", columnList = "alarm_id", unique = true) })
public class LmsAlarmStatusDev extends TimeStampHook {
    /**
     * SerialVersion UID
     */
    private static final long serialVersionUID = 965222672703826886L;

    @PrimaryKey
    //@Sequence(name = "lms_alarm_status_dev_id_seq")
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "alarm_id", nullable = false, length = 36)
    private String alarmId;

    @Column(name = "equip_id", length = 36)
    private String equipId;

    @Column(name = "lc_id", nullable = false, length = 36)
    private String lcId;

    @Column(name = "line_id", length = 36)
    private String lineId;

    @Column(name = "order_id", length = 36)
    private String orderId;

    @Column(name = "alarm_type", nullable = false, length = 20)
    private String alarmType;

    @Column(name = "alarm_msg")
    private String alarmMsg;

    @Column(name = "is_cleared")
    private Boolean isCleared;

    @Column(name = "cleared_by", length = 100)
    private String clearedBy;

    @Column(name = "description")
    private String description;

    @Column(name = "source_system", length = 50)
    private String sourceSystem;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "occurred_at", nullable = false, type = xyz.elidom.dbist.annotation.ColumnType.DATETIME)
    private Date occurredAt;

    @Column(name = "cleared_at", nullable = false, type = xyz.elidom.dbist.annotation.ColumnType.DATETIME)
    private Date clearedAt;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAlarmId() {
        return alarmId;
    }

    public void setAlarmId(String alarmId) {
        this.alarmId = alarmId;
    }

    public String getEquipId() {
        return equipId;
    }

    public void setEquipId(String equipId) {
        this.equipId = equipId;
    }

    public String getLcId() {
        return lcId;
    }

    public void setLcId(String lcId) {
        this.lcId = lcId;
    }

    public String getLineId() {
        return lineId;
    }

    public void setLineId(String lineId) {
        this.lineId = lineId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getAlarmType() {
        return alarmType;
    }

    public void setAlarmType(String alarmType) {
        this.alarmType = alarmType;
    }

    public String getAlarmMsg() {
        return alarmMsg;
    }

    public void setAlarmMsg(String alarmMsg) {
        this.alarmMsg = alarmMsg;
    }

    public Boolean getIsCleared() {
        return isCleared;
    }

    public void setIsCleared(Boolean isCleared) {
        this.isCleared = isCleared;
    }

    public String getClearedBy() {
        return clearedBy;
    }

    public void setClearedBy(String clearedBy) {
        this.clearedBy = clearedBy;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Date getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Date occurredAt) {
        this.occurredAt = occurredAt;
    }

    public Date getClearedAt() {
        return clearedAt;
    }

    public void setClearedAt(Date clearedAt) {
        this.clearedAt = clearedAt;
    }

}
