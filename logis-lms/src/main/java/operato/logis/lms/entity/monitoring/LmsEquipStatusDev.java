package operato.logis.lms.entity.monitoring;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.*;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.entity.basic.TimeStampHook;

import java.io.Serial;
import java.util.Date;

@Getter
@Setter
@Table(name = "lms_equipment_status_dev", idStrategy = GenerationRule.UUID, notnullFields = "equip_id,lc_id,current_status,status_updated_at,data_updated_at", uniqueFields = "id",
        indexes = { @Index(name = "ix_lms_equipment_status_dev_0", columnList = "equip_id,lc_id", unique = true) })
public class LmsEquipStatusDev extends TimeStampHook {

    /**
     * SerialVersion UID
     */
    @Serial
    private static final long serialVersionUID = 8289239481500221798L;

    @PrimaryKey
    //@Sequence(name = "lms_equipment_status_dev_id_seq")
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "equip_id", nullable = false, length = 36)
    private String equipId;

    @Column(name = "lc_id", nullable = false, length = 36)
    private String lcId;

    @Column(name = "line_id", length = 36)
    private String lineId;

    @Column(name = "order_id", length = 50)
    private String orderId;

    @Column(name = "current_status", nullable = false, length = OrmConstants.FIELD_SIZE_STATUS)
    private String currentStatus;

    @Column(name = "pre_status", length = OrmConstants.FIELD_SIZE_STATUS)
    private String preStatus;

    @Column(name = "err_cd", length = 20)
    private String errCd;

    @Column(name = "err_msg", type = ColumnType.TEXT)
    private String errMsg;

    @Column(name = "sensor_value")
    private Integer sensorValue;

    @Column(name = "sensor_unit", length = 20)
    private String sensorUnit;

    @Column(name = "operating_cnt")
    private Integer operatingCnt;

    @Column(name = "err_cnt")
    private Integer errCnt;

    @Column(name = "status_updated_at", nullable = false, type = ColumnType.DATETIME)
    private Date statusUpdatedAt;

    @Column(name = "data_updated_at", nullable = false, type = ColumnType.DATETIME)
    private Date dataUpdatedAt;

    @Column(name = "source_system", length = 50)
    private String sourceSystem;

    public LmsEquipStatusDev() {}

    public String getId() { return id; }

    public String getEquipId() { return equipId; }

    public String getLcId() { return lcId; }

    public String getLineId() { return lineId; }

    public String getOrderId() { return orderId; }

    public String getCurrentStatus() { return currentStatus; }

    public String getPreStatus() { return preStatus; }

    public String getErrCd() { return errCd; }

    public String getErrMsg() { return errMsg; }

    public Integer getSensorValue() { return sensorValue; }

    public String getSensorUnit() { return sensorUnit; }

    public Integer getOperatingCnt() { return operatingCnt; }

    public Integer getErrCnt() { return errCnt; }

    public Date getStatusUpdatedAt() { return statusUpdatedAt; }

    public Date getDataUpdatedAt() { return dataUpdatedAt; }

    public String getSourceSystem() { return sourceSystem; }

    public void setId(String id) { this.id = id; }

    public void setEquipId(String equipId) { this.equipId = equipId; }

    public void setLcId(String lcId) { this.lcId = lcId; }

    public void setLineId(String lineId) { this.lineId = lineId; }

    public void setOrderId(String orderId) { this.orderId = orderId; }

    public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }

    public void setPreStatus(String preStatus) { this.preStatus = preStatus; }

    public void setErrCd(String errCd) { this.errCd = errCd; }

    public void setErrMsg(String errMsg) { this.errMsg = errMsg; }

    public void setSensorValue(Integer sensorValue) { this.sensorValue = sensorValue; }

    public void setSensorUnit(String sensorUnit) { this.sensorUnit = sensorUnit; }

    public void setOperatingCnt(Integer operatingCnt) { this.operatingCnt = operatingCnt; }

    public void setErrCnt(Integer errCnt) { this.errCnt = errCnt; }

    public void setStatusUpdatedAt(Date statusUpdatedAt) { this.statusUpdatedAt = statusUpdatedAt; }

    public void setDataUpdatedAt(Date dataUpdatedAt) { this.dataUpdatedAt = dataUpdatedAt; }

    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }
}
