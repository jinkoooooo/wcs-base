package operato.logis.samsung.dto.xyz;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import operato.logis.samsung.entity.xyz.TbMwIfXyzOrder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SkuInfoRequest {

    private String id;

    private Integer length;

    private Integer width;

    private Integer height;

    private Integer weight;

    public static SkuInfoRequest fromXyzOrder(TbMwIfXyzOrder order) {
        return new SkuInfoRequest(
                order.getTaskId(),
                order.getLength(),
                order.getWidth(),
                order.getHeight(),
                order.getWeight()
        );
    }
}