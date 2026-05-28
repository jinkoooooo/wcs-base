package operato.logis.samsung.dto.report.response;

import lombok.Data;

@Data
public class DailyHourlyUphRowDto {

    private String hourSlot;
    private String firstBoxStartAtInHour;
    private String lastBoxEndAtInHour;
    private Integer boxQtyInHour;
    private Integer okQty;
    private Integer ngQty;
    private Double effectiveUph;
}