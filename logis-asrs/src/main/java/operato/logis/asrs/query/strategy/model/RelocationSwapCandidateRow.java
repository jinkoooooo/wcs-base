package operato.logis.asrs.query.strategy.model;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 재배치 swap 후보 DTO.
 *
 * <p>
 * target location에 이미 재고가 있으나, 현재 후보보다 우선순위가 낮아
 * swap 대상으로 고려 가능한 경우 조회한다.
 * </p>
 */
@Getter
@Setter
@ToString
public class RelocationSwapCandidateRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private String locationId;
    private String locationCode;
    private String locationGrade;
    private Integer sortSeq;
    private String frontPriorityYn;

    private String stockUnitId;
    private String stockUnitNo;

    private String itemId;
    private String itemCode;
    private String itemName;
    private String itemGrade;

    private Integer qty;
    private Integer reservedQty;
}