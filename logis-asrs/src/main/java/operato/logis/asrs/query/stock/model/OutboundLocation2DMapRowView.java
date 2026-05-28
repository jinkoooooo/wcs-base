package operato.logis.asrs.query.stock.model;

import lombok.Data;

/**
 * 2D 맵 조회용 평탄 row 모델.
 *
 * 주의:
 * - location 1건 = row 1건
 * - 프론트 응답용 cell/depth 구조는 controller/service 에서 grouping
 */
@Data
public class OutboundLocation2DMapRowView {

    private String locationId;
    private String locationCode;

    private String areaCode;
    private Integer aisleNo;
    private String sideCode;
    private Integer bayNo;
    private Integer levelNo;
    private Integer depthNo;

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