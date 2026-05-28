package operato.logis.simulator.tspg.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderUpdateRequestDto {

    private String orderKey;

    private String status;
}
