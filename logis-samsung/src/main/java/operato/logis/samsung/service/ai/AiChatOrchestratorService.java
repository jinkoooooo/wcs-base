package operato.logis.samsung.service.ai;

import operato.logis.samsung.consts.ai.AiIntentType;
import operato.logis.samsung.dto.ai.internal.AiHandlerResult;
import operato.logis.samsung.dto.ai.internal.AiParsedCommand;
import operato.logis.samsung.dto.ai.request.AiChatRequest;
import operato.logis.samsung.dto.ai.response.AiChatResponse;
import operato.logis.samsung.service.ai.handler.AiCommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiChatOrchestratorService {

    private final AiTextNormalizer aiTextNormalizer;
    private final AiEntityExtractor aiEntityExtractor;
    private final AiIntentClassifier aiIntentClassifier;
    private final AiResponseFormatter aiResponseFormatter;
    private final AiChatHistoryService aiChatHistoryService;
    private final List<AiCommandHandler> handlers;

    public AiChatResponse ask(AiChatRequest request) {
        try {
            request.setDomainId(8L);
            String normalized = aiTextNormalizer.normalize(request.getQuestion());
            AiParsedCommand command = aiEntityExtractor.extract(request, normalized);

            final AiIntentType intentType = aiIntentClassifier.classify(command);
            command.setIntentType(intentType);

            if (intentType == AiIntentType.UNKNOWN) {
                AiChatResponse fail = aiResponseFormatter.fail(
                        request.getQuestion(),
                        intentType,
                        "질문 유형을 해석하지 못했습니다. 입출고 현황, SKU, 시리얼, 에러코드, 특정일자 에러현황 형태로 질문해 주세요."
                );
                aiChatHistoryService.saveFail(
                        request.getQuestion(),
                        intentType.name(),
                        request.getUserId(),
                        request.getDomainId(),
                        request,
                        fail.getErrorMessage()
                );
                return fail;
            }

            AiCommandHandler handler = handlers.stream()
                    .filter(h -> h.supports(intentType))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("지원 Handler 가 없습니다. intent=" + intentType));

            AiHandlerResult result = handler.handle(command);
            AiChatResponse response = aiResponseFormatter.success(request.getQuestion(), intentType, result);

            aiChatHistoryService.saveSuccess(
                    request.getQuestion(),
                    intentType.name(),
                    request.getUserId(),
                    request.getDomainId(),
                    request,
                    response
            );
            return response;

        } catch (Exception e) {
            AiChatResponse fail = aiResponseFormatter.fail(
                    request.getQuestion(),
                    AiIntentType.UNKNOWN,
                    e.getMessage()
            );
            aiChatHistoryService.saveFail(
                    request.getQuestion(),
                    AiIntentType.UNKNOWN.name(),
                    request.getUserId(),
                    request.getDomainId(),
                    request,
                    e.getMessage()
            );
            return fail;
        }
    }
}