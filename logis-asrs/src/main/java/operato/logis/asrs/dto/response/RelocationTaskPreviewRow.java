package operato.logis.asrs.dto.response;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 재배치 Task 미리보기 행 DTO.
 */
@Getter
@Setter
@ToString
public class RelocationTaskPreviewRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private String stockUnitId;
    private String stockUnitNo;

    private String itemId;
    private String itemCode;
    private String itemName;

    private String fromLocationId;
    private String fromLocationCode;
    private String fromLocationGrade;

    private String toLocationId;
    private String toLocationCode;
    private String toLocationGrade;

    private String itemGrade;
    private String taskType;
    private Integer priorityNo;

    private String reason;
}