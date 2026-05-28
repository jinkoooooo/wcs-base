package operato.logis.samsung.dto.dashboard;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardInboundTracking {

    private String item_barcode;

    private Integer tracking_status;

    private Integer box_qty;
}