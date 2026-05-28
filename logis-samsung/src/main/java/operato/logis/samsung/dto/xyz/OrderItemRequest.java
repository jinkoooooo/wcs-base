package operato.logis.samsung.dto.xyz;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import operato.logis.samsung.entity.xyz.TbMwIfXyzOrder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequest {

    private String from;

    private String to;

    @JsonProperty("target_num")
    private Integer targetNum;

    private Integer priority;

    @JsonProperty("sku_info")
    private SkuInfoRequest skuInfo;

    public static OrderItemRequest fromXyzOrder(TbMwIfXyzOrder order) {
        return new OrderItemRequest(
                order.getStartPointCd(),
                order.getEndPointCd(),
                order.getTargetNum(),
                0,
                SkuInfoRequest.fromXyzOrder(order)
        );
    }
}