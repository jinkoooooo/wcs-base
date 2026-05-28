package operato.logis.samsung.service.ai.handler;

import operato.logis.samsung.consts.ai.AiIntentType;
import operato.logis.samsung.dto.ai.internal.AiHandlerResult;
import operato.logis.samsung.dto.ai.internal.AiParsedCommand;

public interface AiCommandHandler {
    boolean supports(AiIntentType intentType);
    AiHandlerResult handle(AiParsedCommand command);
}