package operato.logis.connector.hokusho.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import operato.logis.connector.api.dto.CommonApiResponse;
import operato.logis.connector.api.service.ExternalApiService;
import operato.logis.connector.hokusho.dto.HokushoCommandTaskRequest;
import operato.logis.connector.hokusho.dto.HokushoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class HokushoEcsCommandTaskService {

    private final ExternalApiService externalApiService;

    @Value("${hokusho.ecs.base-url}")
    private String baseUrl;

    /**
     * 호쿠쇼 ECS로 commandTask 요청 전송
     */
    public Mono<HokushoResponse>  sendCommandTask(HokushoCommandTaskRequest request) {
        String url = baseUrl + "/commandTask";

        log.info("[HOKUSHO] [{}] Sending commandTask => {}", url, request);

        return externalApiService.post(url, request, CommonApiResponse.class)
                .map(response -> {
                    log.info("[HOKUSHO] Response code={}, message={}", response.getCode(), response.getMessage());
                    HokushoResponse result = new HokushoResponse();
                    result.setCode(String.valueOf(response.getCode()));
                    result.setMessage(response.getMessage());
                    result.setCommandId(request.getCommandId());
                    return result;
                })
                .doOnError(e -> log.error("[HOKUSHO] commandTask failed: {}", e.getMessage()));
    }
}