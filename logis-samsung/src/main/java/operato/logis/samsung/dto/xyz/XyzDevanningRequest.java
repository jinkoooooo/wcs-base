package operato.logis.samsung.dto.xyz;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class XyzDevanningRequest {

    // 최상위 "name": "container_1" 매핑
    private String name;

    // "raw_data" 객체 매핑
    @JsonProperty("raw_data")
    private RawData rawData;

    @Getter
    @Setter
    public static class RawData {
        @JsonProperty("order_type")
        private Integer orderType;

        @JsonProperty("sku_input_type")
        private String skuInputType;

        // "items" 배열 매핑
        private List<Item> items;

        @JsonProperty("truck_inner_dimension")
        private TruckInnerDimension truckInnerDimension;

        @JsonProperty("cross_beam_dimension")
        private CrossBeamDimension crossBeamDimension;
    }

    @Getter
    @Setter
    public static class Item {
        private String barcode;

        @JsonProperty("barcode_direction")
        private Integer barcodeDirection;

        private Integer height;
        private Integer id;
        private Integer length;
        private String name;
        private Integer weight;
        private Integer width;
    }

    @Getter
    @Setter
    public static class TruckInnerDimension {
        private Integer height;
        private Integer length;
        private Integer width;
    }

    @Getter
    @Setter
    public static class CrossBeamDimension {
        private Integer width;
        private Integer height;
    }
}