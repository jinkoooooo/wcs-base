package operato.logis.samsung.dto.ai.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ErrorGuideDto {
    private String errorCode;
    private String unitType;
    private String errorName;
    private String errorDesc;
    private String mainCause;
    private String severity;
    private String manualRequiredYn;
    private Integer recentCount;
    private List<AiGuideStepDto> guideSteps;
}