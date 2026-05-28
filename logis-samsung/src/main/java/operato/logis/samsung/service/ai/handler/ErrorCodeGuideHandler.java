package operato.logis.samsung.service.ai.handler;

import operato.logis.samsung.consts.ai.AiIntentType;
import operato.logis.samsung.dto.ai.internal.AiHandlerResult;
import operato.logis.samsung.dto.ai.internal.AiParsedCommand;
import operato.logis.samsung.dto.ai.response.ErrorGuideDto;
import operato.logis.samsung.query.ai.AiErrorGuideQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class ErrorCodeGuideHandler implements AiCommandHandler {

    private final AiErrorGuideQuery aiErrorGuideQuery;

    @Override
    public boolean supports(AiIntentType intentType) {
        return intentType == AiIntentType.ERROR_CODE_GUIDE;
    }

    @Override
    public AiHandlerResult handle(AiParsedCommand command) {
        if (command.getErrorCode() == null || command.getErrorCode().isBlank()) {
            throw new IllegalArgumentException("에러코드가 없습니다. 예: 에러코드 SC105 조치 가이드 알려줘");
        }

        ErrorGuideDto dto = aiErrorGuideQuery.getErrorGuide(command.getErrorCode(), command.getDomainId());

        String summary = String.format(
                "에러코드 %s 는 %s 이며, 최근 발생 %d건입니다. 주요 원인은 %s 입니다.",
                dto.getErrorCode(),
                dto.getErrorName() == null ? "정의없음" : dto.getErrorName(),
                dto.getRecentCount() == null ? 0 : dto.getRecentCount(),
                dto.getMainCause() == null ? "미정의" : dto.getMainCause()
        );

        return AiHandlerResult.builder()
                .summary(summary)
                .data(dto)
                .guideSteps(dto.getGuideSteps())
                .sourceTables(Arrays.asList(
                        "tb_mw_error_guide",
                        "tb_mw_unit_error_log"
                ))
                .build();
    }
}