package operato.logis.samsung.dto.ai.internal;

import operato.logis.samsung.dto.ai.response.AiGuideStepDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiHandlerResult {
    private String summary;
    private Object data;
    private List<AiGuideStepDto> guideSteps;
    private List<String> sourceTables;
}