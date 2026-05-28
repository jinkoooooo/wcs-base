package operato.logis.samsung.service.ai.handler;

import operato.logis.samsung.consts.ai.AiIntentType;
import operato.logis.samsung.dto.ai.internal.AiHandlerResult;
import operato.logis.samsung.dto.ai.internal.AiParsedCommand;
import operato.logis.samsung.dto.ai.response.SkuIoStatusDto;
import operato.logis.samsung.query.ai.AiSkuStatusQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class SkuIoStatusHandler implements AiCommandHandler {

    private final AiSkuStatusQuery aiSkuStatusQuery;

    @Override
    public boolean supports(AiIntentType intentType) {
        return intentType == AiIntentType.SKU_IO_STATUS;
    }

    @Override
    public AiHandlerResult handle(AiParsedCommand command) {
        if (command.getSkuCode() == null || command.getSkuCode().isBlank()) {
            throw new IllegalArgumentException("SKU 코드가 없습니다. 예: SKU123 입출고 현황 알려줘");
        }

        LocalDate targetDate = command.getTargetDate() != null ? command.getTargetDate() : LocalDate.now();
        SkuIoStatusDto dto = aiSkuStatusQuery.getSkuStatus(command.getSkuCode(), targetDate, command.getDomainId());

        String summary = String.format(
                "SKU %s 기준 %s, 당일 입고 %d건, 출고지시 %d건, 출고완료 %d건, 리젝 %d건입니다.",
                dto.getItemCode(),
                dto.getItemName() == null ? "품목명 미확인" : dto.getItemName(),
                nvl(dto.getTodayInboundQty()),
                nvl(dto.getTodayOutboundTargetQty()),
                nvl(dto.getTodayOutboundPassQty()),
                nvl(dto.getRejectQty())
        );

        return AiHandlerResult.builder()
                .summary(summary)
                .data(dto)
                .sourceTables(Arrays.asList(
                        "tb_mw_item_master",
                        "tb_mw_inbound_delivery",
                        "tb_mw_xyz_order",
                        "tb_mw_reject_box",
                        "tb_mw_box"
                ))
                .build();
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }
}