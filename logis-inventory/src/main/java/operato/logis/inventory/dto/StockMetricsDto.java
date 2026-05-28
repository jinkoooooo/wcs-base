package operato.logis.inventory.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockMetricsDto {

    private String stockId;

    private Integer totalWeight;

    private Integer totalHeight;
}
