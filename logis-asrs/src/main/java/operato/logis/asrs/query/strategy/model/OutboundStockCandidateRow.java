package operato.logis.asrs.query.strategy.model;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 출고 재고 선택 후보 DTO.
 */
@Getter
@Setter
@ToString
public class OutboundStockCandidateRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private String stockUnitId;
    private String stockUnitNo;
    private String itemId;
    private String itemCode;
    private String itemName;

    private Integer qty;
    private Integer reservedQty;
    private Integer availableQty;

    private String locationId;
    private String locationCode;
    private String locationGrade;
    private Integer sortSeq;
    private String frontPriorityYn;

    private String lotId;
    private String lotNo;
}