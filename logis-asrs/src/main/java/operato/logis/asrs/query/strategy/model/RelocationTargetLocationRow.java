package operato.logis.asrs.query.strategy.model;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 재배치 목적지 로케이션 후보 DTO.
 */
@Getter
@Setter
@ToString
public class RelocationTargetLocationRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private String locationId;
    private String locationCode;
    private String locationGrade;
    private Integer sortSeq;
    private String frontPriorityYn;
}