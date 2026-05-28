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
public class CustomizedDataRequest {

    @JsonProperty("pallet_capacity")
    private String palletCapacity;

    @JsonProperty("material")
    private String material;

    @JsonProperty("barcode")
    private String barcode;

    @JsonProperty("is_single_sku")
    private Integer isSingleSku;

    public static CustomizedDataRequest fromXyzOrder(TbMwIfXyzOrder order, Integer isSingleSku) {
        return new CustomizedDataRequest(
                order.getPalletCapacity(),
                order.getMaterial(),
                order.getBarcode(),
                isSingleSku
        );
    }
}