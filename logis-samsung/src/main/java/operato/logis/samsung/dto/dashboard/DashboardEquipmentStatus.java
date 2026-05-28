package operato.logis.samsung.dto.dashboard;

import lombok.Getter;
import lombok.Setter;
import operato.logis.samsung.entity.wcs.TbMwUnitHeartbeat;

@Getter
@Setter
public class DashboardEquipmentStatus {

    private String unitType;

    private String unitCode;

    private String status;

    private String msg;

    public static DashboardEquipmentStatus fromEquipmentStatus(TbMwUnitHeartbeat unit) {
        DashboardEquipmentStatus dashboardEquipmentStatus = new DashboardEquipmentStatus();
        dashboardEquipmentStatus.setUnitType(unit.getUnitType());
        dashboardEquipmentStatus.setUnitCode(unit.getUnitCode());
        if (Integer.parseInt(unit.getStatus()) <= 1) {
            dashboardEquipmentStatus.setStatus("0");
        } else {
            dashboardEquipmentStatus.setStatus("9");
        }
        dashboardEquipmentStatus.setMsg(unit.getMsg());
        return dashboardEquipmentStatus;
    }
}
