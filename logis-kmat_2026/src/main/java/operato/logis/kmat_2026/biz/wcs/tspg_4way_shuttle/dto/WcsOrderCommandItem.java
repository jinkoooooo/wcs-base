package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WcsOrderCommandItem {

    @JsonProperty("lineNo")
    private Integer lineNo;

    @JsonProperty("skuCode")
    private String skuCode;

    @JsonProperty("lotNo")
    private String lotNo;

    @JsonProperty("qty")
    private Integer qty;

    @JsonProperty("uom")
    private String uom;
    private String rawAttr;
}