package operato.logis.samsung.service.ai;

import operato.logis.samsung.consts.ai.AiIntentType;
import operato.logis.samsung.dto.ai.internal.AiHandlerResult;
import operato.logis.samsung.dto.ai.response.AiChatResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AiResponseFormatter {

    public AiChatResponse success(String question, AiIntentType intentType, AiHandlerResult result) {
        return AiChatResponse.builder()
                .success(true)
                .question(question)
                .intentType(intentType)
                .summary(result.getSummary())
                .data(result.getData())
                .guideSteps(result.getGuideSteps())
                .sourceTables(result.getSourceTables())
                .generatedAt(LocalDateTime.now())
                .build();
    }

    public AiChatResponse fail(String question, AiIntentType intentType, String errorMessage) {
        return AiChatResponse.builder()
                .success(false)
                .question(question)
                .intentType(intentType)
                .errorMessage(errorMessage)
                .generatedAt(LocalDateTime.now())
                .build();
    }
}