package operato.logis.kmat_2026.entity;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;

@Table(name = "tb_ecs_loc_mst", idStrategy = GenerationRule.UUID)
public class TbEcsLocMst extends xyz.elidom.orm.entity.basic.ElidomStampHook {
    /**
     * SerialVersion UID
     */
    private static final long serialVersionUID = 787792703432350429L;

    @PrimaryKey
    @Column (name = "id", nullable = false, length = 40)
    private String id;

    @Column (name = "lc_id", nullable = false, length = 20)
    private String lcId;

    @Column (name = "group_cd", nullable = false, length = 50)
    private String groupCd;

    @Column (name = "location_cd", nullable = false, length = 50)
    private String locationCd;

    @Column (name = "transfer_equip_cd", length = 50)
    private String transferEquipCd;

    @Column (name = "location_status", length = 50)
    private String locationStatus;

    @Column (name = "location_use_yn", length = 4)
    private Integer locationUseYn;

    @Column (name = "locked_yn", length = 1)
    private Integer lockedYn;

    @Column (name = "location_nm", length = 50)
    private String locationNm;

    @Column (name = "location_item_info", length = 50)
    private String locationItemInfo;

    @Column (name = "lock_order_id", length = 50)
    private String lockOrderId;

    @Column (name = "zone_cd", length = 50)
    private String zoneCd;

    @Column (name = "location_seq")
    private Integer locationSeq;

    @Column (name = "location_role", length = 40)
    private String locationRole;

    @Column (name = "location_floor", length = 50)
    private String locationFloor;

    @Column (name = "pod_cd", length = 30)
    private String podCd;

    @Column(name = "position_x")
    private Double positionX;

    @Column(name = "position_y")
    private Double positionY;

    @Column(name = "position_z")
    private Double positionZ;

    @Column(name = "equip_id",length = 10)
    private String equipId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLcId() {
        return lcId;
    }

    public void setLcId(String lcId) {
        this.lcId = lcId;
    }

    public String getGroupCd() {
        return groupCd;
    }

    public void setGroupCd(String groupCd) {
        this.groupCd = groupCd;
    }

    public String getLocationCd() {
        return locationCd;
    }

    public void setLocationCd(String locationCd) {
        this.locationCd = locationCd;
    }

    public String getTransferEquipCd() {
        return transferEquipCd;
    }

    public void setTransferEquipCd(String transferEquipCd) {
        this.transferEquipCd = transferEquipCd;
    }

    public String getLocationStatus() {
        return locationStatus;
    }

    public void setLocationStatus(String locationStatus) {
        this.locationStatus = locationStatus;
    }

    public Integer getLocationUseYn() {
        return locationUseYn;
    }

    public void setLocationUseYn(Integer locationUseYn) {
        this.locationUseYn = locationUseYn;
    }

    public String getLocationNm() {
        return locationNm;
    }

    public void setLocationNm(String locationNm) {
        this.locationNm = locationNm;
    }

    public String getLocationItemInfo() {
        return locationItemInfo;
    }

    public void setLocationItemInfo(String locationItemInfo) {
        this.locationItemInfo = locationItemInfo;
    }

    public String getZoneCd() {
        return zoneCd;
    }

    public void setZoneCd(String zoneCd) {
        this.zoneCd = zoneCd;
    }

    public Integer getLocationSeq() {
        return locationSeq;
    }

    public void setLocationSeq(Integer locationSeq) {
        this.locationSeq = locationSeq;
    }

    public String getLocationRole() {
        return locationRole;
    }

    public void setLocationRole(String locationRole) {
        this.locationRole = locationRole;
    }

    public String getLocationFloor() {
        return locationFloor;
    }

    public void setLocationFloor(String locationFloor) {
        this.locationFloor = locationFloor;
    }

    public String getPodCd() {
        return podCd;
    }

    public void setPodCd(String podCd) {
        this.podCd = podCd;
    }

    public Integer getLockedYn() {
        return lockedYn;
    }

    public void setLockedYn(Integer lockedYn) {
        this.lockedYn = lockedYn;
    }

    public Double getPositionX() {
        return positionX;
    }

    public void setPositionX(Double positionX) {
        this.positionX = positionX;
    }

    public Double getPositionY() {
        return positionY;
    }

    public void setPositionY(Double positionY) {
        this.positionY = positionY;
    }

    public Double getPositionZ() {
        return positionZ;
    }

    public void setPositionZ(Double positionZ) {
        this.positionZ = positionZ;
    }

    public String getEquipId() {
        return equipId;
    }

    public void setEquipId(String equipId) {
        this.equipId = equipId;
    }

    public String getLockOrderId() {
        return lockOrderId;
    }

    public void setLockOrderId(String lockOrderId) {
        this.lockOrderId = lockOrderId;
    }
}
