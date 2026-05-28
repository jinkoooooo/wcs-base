package operato.logis.samsung.service.ai.handler;

import operato.logis.samsung.consts.ai.AiIntentType;
import operato.logis.samsung.dto.ai.internal.AiHandlerResult;
import operato.logis.samsung.dto.ai.internal.AiParsedCommand;
import operato.logis.samsung.dto.ai.response.SerialProgressDto;
import operato.logis.samsung.query.ai.AiSerialStatusQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class SerialProgressStatusHandler implements AiCommandHandler {

    private final AiSerialStatusQuery aiSerialStatusQuery;

    @Override
    public boolean supports(AiIntentType intentType) {
        return intentType == AiIntentType.SERIAL_PROGRESS_STATUS;
    }

    @Override
    public AiHandlerResult handle(AiParsedCommand command) {
        if (command.getSerialNo() == null || command.getSerialNo().isBlank()) {
            throw new IllegalArgumentException("시리얼 코드가 없습니다. 예: 시리얼 ABC123 현재 진행 상황 알려줘");
        }

        SerialProgressDto dto = aiSerialStatusQuery.getSerialProgress(command.getSerialNo(), command.getDomainId());

        String summary = String.format(
                "시리얼 %s 현재 상태는 %s, 현재 위치는 %s / %s 입니다.",
                dto.getSerialNo(),
                dto.getTrackingDesc() == null ? "상태미확인" : dto.getTrackingDesc(),
                dto.getCurrentLineId() == null ? "-" : dto.getCurrentLineId(),
                dto.getCurrentEquipId() == null ? "-" : dto.getCurrentEquipId()
        );

        return AiHandlerResult.builder()
                .summary(summary)
                .data(dto)
                .sourceTables(Arrays.asList(
                        "tb_mw_box",
                        "tb_mw_box_track",
                        "tb_mw_box_conveyor_info"
                ))
                .build();
    }
}