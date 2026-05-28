package operato.logis.asrs.dto.response;

import java.io.Serializable;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import operato.logis.asrs.query.strategy.model.OutboundStockCandidateRow;

/**
 * 출고 재고 추천 결과 DTO.
 */
@Getter
@Setter
@ToString
public class OutboundLocationSelectResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String areaCode;
    private String itemCode;
    private Integer requiredQty;
    private Integer candidateCount;

    private List<OutboundStockCandidateRow> stocks;
}