package operato.logis.asrs.query.strategy.model;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 재배치 대상 현재고 후보 DTO.
 */
@Getter
@Setter
@ToString
public class RelocationStockCandidateRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private String stockUnitId;
    private String stockUnitNo;

    private String itemId;
    private String itemCode;
    private String itemName;
    private String itemCategoryId;

    private Integer qty;
    private Integer reservedQty;

    private String stockStatusCode;
    private String activeYn;
    private String holdYn;

    private String currentLocationId;
    private String currentLocationCode;
    private String currentLocationGrade;
    private Integer currentSortSeq;
    private String currentFrontPriorityYn;

    private String itemGrade;
    private Integer itemGradeRank;
    private Integer locationGradeRank;

    /** 명일 수요 수량 */
    private Integer demandTomorrowQty;
}