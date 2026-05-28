package operato.logis.asrs.query.strategy.model;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 입고 로케이션 추천 후보 DTO.
 */
@Getter
@Setter
@ToString
public class InboundLocationCandidateRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private String locationId;
    private String locationCode;
    private String locationGrade;
    private Integer sortSeq;
    private String frontPriorityYn;
}