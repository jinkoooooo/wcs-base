package operato.logis.samsung.dto.ai.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SkuIoStatusDto {
    private String itemCode;
    private String itemName;

    private Integer todayInboundQty;
    private Integer todayOutboundTargetQty;
    private Integer todayOutboundPassQty;
    private Integer todayOutboundNgQty;

    private Integer rejectQty;

    private LocalDateTime lastProcessedAt;
    private String lastStatus;
}