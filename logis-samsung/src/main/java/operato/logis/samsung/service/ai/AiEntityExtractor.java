package operato.logis.samsung.service.ai;

import operato.logis.samsung.dto.ai.internal.AiParsedCommand;
import operato.logis.samsung.dto.ai.request.AiChatRequest;
import operato.logis.samsung.utils.ai.AiDateParser;
import operato.logis.samsung.utils.ai.AiRegexUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class AiEntityExtractor {

    private final AiDateParser aiDateParser;

    public AiParsedCommand extract(AiChatRequest request, String normalizedQuestion) {
        LocalDate parsedDate = request.getTargetDate() != null
                ? request.getTargetDate()
                : aiDateParser.parse(normalizedQuestion);

        return AiParsedCommand.builder()
                .originalQuestion(request.getQuestion())
                .normalizedQuestion(normalizedQuestion)
                .lowerQuestion(normalizedQuestion.toLowerCase())
                .userId(request.getUserId())
                .domainId(request.getDomainId())
                .contextLineId(request.getContextLineId())
                .contextEquipId(request.getContextEquipId())
                .targetDate(parsedDate)
                .skuCode(AiRegexUtils.extractSkuCode(normalizedQuestion))
                .serialNo(AiRegexUtils.extractSerialNo(normalizedQuestion))
                .errorCode(AiRegexUtils.extractErrorCode(normalizedQuestion))
                .build();
    }
}