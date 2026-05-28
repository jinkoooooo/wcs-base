package operato.logis.samsung.rest.ai;

import operato.logis.samsung.dto.ai.request.AiChatRequest;
import operato.logis.samsung.dto.ai.response.AiChatResponse;
import operato.logis.samsung.service.ai.AiChatOrchestratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rest/ai/chat")
public class AiChatController {

    private final AiChatOrchestratorService aiChatOrchestratorService;

    @PostMapping("/ask")
    public ResponseEntity<AiChatResponse> ask(@RequestBody AiChatRequest request) {
        return ResponseEntity.ok(aiChatOrchestratorService.ask(request));
    }
}