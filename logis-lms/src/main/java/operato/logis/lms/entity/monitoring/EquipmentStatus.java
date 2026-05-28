package operato.logis.lms.entity.monitoring;

import xyz.elidom.dbist.annotation.*;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.entity.basic.AbstractStamp;
import xyz.elidom.orm.entity.basic.IEntityHook;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.util.DateUtil;
import xyz.elidom.util.ValueUtil;

import java.io.Serial;
import java.util.Date;

@Table(name = "equipment_status", idStrategy = GenerationRule.NONE, notnullFields = "equipmentName,equipmentType")
public class EquipmentStatus extends AbstractStamp implements IEntityHook {

    // TODO: Serial 추가

    @PrimaryKey
    @Column(name = "equipment_id", nullable = false, length = 20)
    private String equipmentId;

    @Column(name = "equipment_status", length = OrmConstants.FIELD_SIZE_STATUS)
    private String equipmentStatus;

    @Column(name = "equipment_name", nullable = false, length = OrmConstants.FIELD_SIZE_STATUS)
    private String equipmentName;

    @Column(name = "equipment_type", nullable = false, length = OrmConstants.FIELD_SIZE_STATUS)
    private String equipmentType;

    @Column(name = "equipment_mode", length = OrmConstants.FIELD_SIZE_STATUS)
    private String equipmentMode;

    @Column(name = "equipment_description", length = 100)
    private String equipmentDescription;

    @Column(name = "location", length = 30)
    private String location;

    @Column(name = "reg_id", length = 20)
    private String regId;

    @Column(name = "reg_dt")
    private Date regDt;

    @Column(name = "upd_id", length = 20)
    private String updId;

    @Column(name = "upd_dt")
    private Date updDt;

    public EquipmentStatus(String equipmentId, String equipmentStatus, String equipmentName, String equipmentType, String equipmentMode, String equipmentDescription, String location, String regId, Date regDt, String updId, Date updDt) {
        this.equipmentId = equipmentId;
        this.equipmentStatus = equipmentStatus;
        this.equipmentName = equipmentName;
        this.equipmentType = equipmentType;
        this.equipmentMode = equipmentMode;
        this.equipmentDescription = equipmentDescription;
        this.location = location;
        this.regId = regId;
        this.regDt = regDt;
        this.updId = updId;
        this.updDt = updDt;
    }

    public String getEquipmentId() { return equipmentId; }

    public String getEquipmentStatus() { return equipmentStatus; }

    public String getEquipmentName() { return equipmentName; }

    public String getEquipmentType() { return equipmentType; }

    public String getEquipmentMode() { return equipmentMode; }

    public String getEquipmentDescription() { return equipmentDescription; }

    public String getLocation() { return location; }

    public String getRegId() { return regId; }

    public Date getRegDt() { return regDt; }

    public String getUpdId() { return updId; }

    public Date getUpdDt() { return updDt; }

    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }

    public void setEquipmentStatus(String equipmentStatus) { this.equipmentStatus = equipmentStatus; }

    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }

    public void setEquipmentType(String equipmentType) { this.equipmentType = equipmentType; }

    public void setEquipmentMode(String equipmentMode) { this.equipmentMode = equipmentMode; }

    public void setEquipmentDescription(String equipmentDescription) { this.equipmentDescription = equipmentDescription; }

    public void setLocation(String location) { this.location = location; }

    public void setRegId(String regId) { this.regId = regId; }

    public void setRegDt(Date regDt) { this.regDt = regDt; }

    public void setUpdId(String updId) { this.updId = updId; }

    public void setUpdDt(Date updDt) { this.updDt = updDt; }

    @Override
    public void beforeCreate() {
        this._setDefault_(true, false);
        this._setId_();
        this.validationCheck(OrmConstants.CUD_FLAG_CREATE);
    }

    @Override
    public void afterCreate() {

    }

    @Override
    public void beforeUpdate() {

    }

    @Override
    public void afterUpdate() {

    }

    @Override
    public void beforeDelete() {

    }

    @Override
    public void afterDelete() {

    }

    @Override
    public void beforeFind() {

    }

    @Override
    public void afterFind() {

    }

    @Override
    public void beforeSearch() {

    }

    @Override
    public void afterSearch() {

    }

    public void _setDefault_(boolean createFlag, boolean updateFlag) {
        if (!(createFlag || updateFlag))
            return;

        Date now = DateUtil.getDate();

        if (createFlag && ValueUtil.isEmpty(this.regDt)) {
            this.setRegDt(now);
            this.setUpdDt(now);
        }


        if (updateFlag) {
            this.setUpdDt(now);
        }

        // 필요시 reg_id, upd_id 로직 추가
        //if (createFlag && ValueUtil.isEmpty(this.regId) && User.currentUser() != null) {
        //    this.setRegId(User.currentUser().getId());
        //    this.setUpdId(User.currentUser().getId());
        //}
        //
        //if (updateFlag && User.currentUser() != null) {
        //    this.setRegId(User.currentUser().getId());
        //}
    }
}