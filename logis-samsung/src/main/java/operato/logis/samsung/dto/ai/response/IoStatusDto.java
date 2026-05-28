package operato.logis.samsung.dto.ai.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class IoStatusDto {
    private LocalDate targetDate;

    private Integer inboundJobCount;
    private Integer inboundCompletedItemQty;
    private Integer inboundNgItemQty;

    private Integer outboundOrderCount;
    private Integer outboundCompletedQty;
    private Integer outboundNgQty;

    private Integer abnormalUnitCount;
    private Integer todayErrorCount;
}