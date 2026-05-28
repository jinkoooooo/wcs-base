package operato.logis.samsung.dto.ai.response;

import operato.logis.samsung.consts.ai.AiIntentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponse {
    private boolean success;
    private String question;
    private AiIntentType intentType;
    private String summary;
    private Object data;
    private List<AiGuideStepDto> guideSteps;
    private List<String> sourceTables;
    private String errorMessage;
    private LocalDateTime generatedAt;
}