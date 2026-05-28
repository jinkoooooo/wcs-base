package operato.logis.samsung.service.ai.handler;

import operato.logis.samsung.consts.ai.AiIntentType;
import operato.logis.samsung.dto.ai.internal.AiHandlerResult;
import operato.logis.samsung.dto.ai.internal.AiParsedCommand;
import operato.logis.samsung.dto.ai.response.AiGuideStepDto;
import operato.logis.samsung.dto.ai.response.DailyErrorStatusDto;
import operato.logis.samsung.dto.ai.response.UnresolvedErrorDto;
import operato.logis.samsung.query.ai.AiDailyErrorQuery;
import operato.logis.samsung.query.ai.AiErrorGuideQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DailyErrorStatusGuideHandler implements AiCommandHandler {

    private final AiDailyErrorQuery aiDailyErrorQuery;
    private final AiErrorGuideQuery aiErrorGuideQuery;

    @Override
    public boolean supports(AiIntentType intentType) {
        return intentType == AiIntentType.DAILY_ERROR_STATUS_GUIDE;
    }

    @Override
    public AiHandlerResult handle(AiParsedCommand command) {
        LocalDate targetDate = command.getTargetDate() != null ? command.getTargetDate() : LocalDate.now();
        DailyErrorStatusDto dto = aiDailyErrorQuery.getDailyErrorStatus(targetDate, command.getDomainId());

        List<AiGuideStepDto> mergedGuideSteps = new ArrayList<>();

        if (dto.getUnresolvedErrors() != null) {
            int stepNo = 1;
            for (UnresolvedErrorDto unresolved : dto.getUnresolvedErrors()) {
                List<AiGuideStepDto> steps = aiErrorGuideQuery.getGuideStepsOnly(unresolved.getErrorCode(), command.getDomainId());
                if (steps != null && !steps.isEmpty()) {
                    for (AiGuideStepDto step : steps) {
                        mergedGuideSteps.add(
                                AiGuideStepDto.builder()
                                        .stepNo(stepNo++)
                                        .title("[" + unresolved.getErrorCode() + "] " + step.getTitle())
                                        .description(step.getDescription())
                                        .build()
                        );
                    }
                }
            }
        }

        String summary = String.format(
                "%s 기준 총 에러 %d건, 현재 비정상 설비 %d건, 미조치 추정 %d건입니다.",
                targetDate,
                dto.getTotalErrorCount() == null ? 0 : dto.getTotalErrorCount(),
                dto.getAbnormalUnitCount() == null ? 0 : dto.getAbnormalUnitCount(),
                dto.getUnresolvedErrors() == null ? 0 : dto.getUnresolvedErrors().size()
        );

        return AiHandlerResult.builder()
                .summary(summary)
                .data(dto)
                .guideSteps(mergedGuideSteps)
                .sourceTables(Arrays.asList(
                        "tb_mw_unit_error_log",
                        "tb_mw_unit_heartbeat",
                        "tb_mw_error_guide"
                ))
                .build();
    }
}