package operato.logis.samsung.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import operato.logis.samsung.entity.wcs.TbMwUnitErrorLog;

import java.util.List;

@Getter
@Setter
public class DashboardMain {

    private List<DashboardInboundDelivery> dashboardInboundDeliveryList;

    private DashboardInboundStatus dashboardInboundStatus;

    private List<DashboardChute> dashboardChute;

    private List<DashboardEquipmentStatus> dashboardEquipmentStatus;

    private List<DashboardInboundTracking> dashboardInboundTrackingList;

    private List<TbMwUnitErrorLog> dashboardErrorLogList;

    @JsonProperty("dashboard_uph_list")
    private List<DashboardUPH> dashboardUPHSList;
}