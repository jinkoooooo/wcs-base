package operato.logis.samsung.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import operato.logis.samsung.query.ai.AiChatHistoryQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatHistoryService {

    private final AiChatHistoryQuery aiChatHistoryQuery;
    private final ObjectMapper objectMapper;

    public void saveSuccess(String question, String intentType, String userId, Long domainId, Object request, Object response) {
        save(question, intentType, userId, domainId, request, response, "Y", null);
    }

    public void saveFail(String question, String intentType, String userId, Long domainId, Object request, String errorMsg) {
        save(question, intentType, userId, domainId, request, null, "N", errorMsg);
    }

    private void save(String question, String intentType, String userId, Long domainId,
                      Object request, Object response, String successYn, String errorMsg) {
        try {
            aiChatHistoryQuery.insertHistory(
                    UUID.randomUUID().toString(),
                    question,
                    intentType,
                    userId,
                    domainId,
                    request == null ? null : objectMapper.writeValueAsString(request),
                    response == null ? null : objectMapper.writeValueAsString(response),
                    successYn,
                    errorMsg
            );
        } catch (Exception e) {
            log.error("AI chat history save failed", e);
        }
    }
}