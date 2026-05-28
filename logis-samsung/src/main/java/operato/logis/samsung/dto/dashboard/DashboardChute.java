package operato.logis.samsung.dto.dashboard;

import lombok.Getter;
import lombok.Setter;
import operato.logis.samsung.entity.mw.TbMwChute;

@Getter
@Setter
public class DashboardChute {

    private String startPointCd;

    private String endPointCd;

    private String palletSequence;

    private String orderId;

    private String itemCode;

    private String itemName;

    private String palletCapacity;

    private int expectedQuantity;

    private int completedQuantity;

    private int ngQuantity;

    private int palletQty;

    private int itemWidth;

    private int itemHeight;

    private int itemLength;

    public static DashboardChute fromTbMwChute(TbMwChute chute) {
        DashboardChute dashboardChute = new DashboardChute();
        dashboardChute.setStartPointCd(chute.getStartPointCd());
        dashboardChute.setEndPointCd(chute.getEndPointCd());
        dashboardChute.setPalletSequence(chute.getPalletSequence());
        dashboardChute.setOrderId(chute.getOrderId());
        dashboardChute.setPalletQty(chute.getBoxQty());
        return dashboardChute;
    }
}
