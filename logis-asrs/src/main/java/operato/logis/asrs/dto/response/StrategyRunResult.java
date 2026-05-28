package operato.logis.asrs.dto.response;

import java.io.Serializable;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 전략 실행 결과 DTO.
 */
@Getter
@Setter
@ToString
public class StrategyRunResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String areaCode;
    private String strategyCode;
    private Boolean previewOnly;

    private Integer candidateCount;
    private Integer taskCount;

    /** 실제 저장된 run id 또는 preview 시 null */
    private String strategyRunId;

    private List<RelocationTaskPreviewRow> tasks;
}