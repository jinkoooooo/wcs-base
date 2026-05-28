package operato.logis.asrs.dto.response;

import java.io.Serializable;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import operato.logis.asrs.query.strategy.model.InboundLocationCandidateRow;

/**
 * 입고 로케이션 추천 결과 DTO.
 */
@Getter
@Setter
@ToString
public class InboundLocationSelectResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String areaCode;
    private String itemCode;
    private Integer candidateCount;

    private List<InboundLocationCandidateRow> locations;
}