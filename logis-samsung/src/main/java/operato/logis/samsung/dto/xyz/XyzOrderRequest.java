package operato.logis.samsung.dto.xyz;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import operato.logis.samsung.entity.xyz.TbMwIfXyzOrder;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class XyzOrderRequest {

    @JsonProperty("order_type")
    private Integer orderType;

    @JsonProperty("order_items")
    private List<OrderItemRequest> orderItems;

    @JsonProperty("customized_data")
    private CustomizedDataRequest customizedData;

    public static XyzOrderRequest fromXyzOrder(TbMwIfXyzOrder order, Integer isSingleSku) {
        OrderItemRequest itemRequest = OrderItemRequest.fromXyzOrder(order);
        List<OrderItemRequest> orderItems = Collections.singletonList(itemRequest);
        CustomizedDataRequest customizedData = CustomizedDataRequest.fromXyzOrder(order, isSingleSku);

        return new XyzOrderRequest(
                order.getOrderType(),
                orderItems,
                customizedData
        );
    }
}