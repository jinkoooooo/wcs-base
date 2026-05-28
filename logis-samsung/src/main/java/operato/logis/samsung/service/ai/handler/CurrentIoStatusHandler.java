package operato.logis.samsung.service.ai.handler;

import operato.logis.samsung.consts.ai.AiIntentType;
import operato.logis.samsung.dto.ai.internal.AiHandlerResult;
import operato.logis.samsung.dto.ai.internal.AiParsedCommand;
import operato.logis.samsung.dto.ai.response.IoStatusDto;
import operato.logis.samsung.query.ai.AiIoStatusQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class CurrentIoStatusHandler implements AiCommandHandler {

    private final AiIoStatusQuery aiIoStatusQuery;

    @Override
    public boolean supports(AiIntentType intentType) {
        return intentType == AiIntentType.CURRENT_IO_STATUS;
    }

    @Override
    public AiHandlerResult handle(AiParsedCommand command) {
        LocalDate targetDate = command.getTargetDate() != null ? command.getTargetDate() : LocalDate.now();

        IoStatusDto dto = aiIoStatusQuery.getCurrentIoStatus(targetDate, command.getDomainId());

        String summary = String.format(
                "%s 기준 입고 Job %d건, 입고완료 %d건, 출고 오더 %d건, 출고완료 %d건, 비정상 설비 %d건입니다.",
                targetDate,
                nvl(dto.getInboundJobCount()),
                nvl(dto.getInboundCompletedItemQty()),
                nvl(dto.getOutboundOrderCount()),
                nvl(dto.getOutboundCompletedQty()),
                nvl(dto.getAbnormalUnitCount())
        );

        return AiHandlerResult.builder()
                .summary(summary)
                .data(dto)
                .sourceTables(Arrays.asList(
                        "tb_mw_inbound_job",
                        "tb_mw_xyz_order",
                        "tb_mw_unit_heartbeat",
                        "tb_mw_unit_error_log"
                ))
                .build();
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }
}