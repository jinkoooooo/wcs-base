package operato.logis.asrs.dto.response;

import lombok.Data;

/**
 * 2D 맵 depth 상세 응답 DTO.
 */
@Data
public class OutboundLocation2DDepthResponse {

    private Integer depthNo;

    private String locationId;
    private String locationCode;

    private Boolean occupied;

    private String stockUnitId;
    private String stockUnitNo;

    private String itemId;
    private String itemCode;
    private String itemName;

    private Integer qty;
    private Integer reservedQty;
    private String lotNo;
    private String stockStatusCode;
    private String activeYn;
}