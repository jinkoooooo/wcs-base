package operato.logis.connector.sineva.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import xyz.elidom.exception.server.ElidomRuntimeException;

import java.util.Map;

/**
 * Sineva HTTP 호출 전담 클래스.
 *
 * 역할:
 * 1. HTTP 요청 전송
 * 2. 응답 JSON 파싱
 * 3. 공통 예외 처리
 * 4. Sineva business code(예: code=400) 처리
 *
 * 주의:
 * - 외부에서는 endpoint, body 규칙을 몰라도 되도록
 *   이 클래스는 SinevaFacade 뒤에 숨겨서 사용한다.
 */
@Component
public class SinevaHttpClient {

    private static final Logger logger = LoggerFactory.getLogger(SinevaHttpClient.class);

    /** Jackson JSON 처리기 */
    private final ObjectMapper objectMapper;

    /**
     * Spring 기본 HTTP 클라이언트.
     *
     * 현재는 단순성과 가독성을 위해 RestTemplate 사용.
     * 향후 timeout / interceptors / pooling 필요 시 Bean 분리 가능.
     */
    private final RestTemplate restTemplate = new RestTemplate();

    public SinevaHttpClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 공통 HTTP 호출 메서드.
     *
     * @param method   HTTP Method (POST, GET 등)
     * @param url      서버 base url (예: http://127.0.0.1:8080)
     * @param endpoint ECS endpoint
     * @param body     요청 바디
     * @return 응답 JSON
     */
    public JsonNode call(HttpMethod method, String url, String endpoint, Map<String, Object> body) {
        final String requestUrl = url + endpoint;

        try {
            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            final HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            final ResponseEntity<String> response = restTemplate.exchange(
                    requestUrl,
                    method,
                    entity,
                    String.class
            );

            // HTTP status 검증
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ElidomRuntimeException("Sineva 호출 실패: " + response.getStatusCode());
            }

            final String responseBody = response.getBody();

            // 응답 body 존재 여부 확인
            if (responseBody == null || responseBody.isBlank()) {
                throw new ElidomRuntimeException("Sineva 응답이 비어 있습니다.");
            }

            final JsonNode json = objectMapper.readTree(responseBody);

            // Sineva 비즈니스 오류 코드 처리
            if ("400".equals(json.path("code").asText())) {
                final String message = json.path("msg").asText("Sineva business error");
                throw new ElidomRuntimeException(message);
            }

            logger.info("[SINEVA] method={}, url={}, requestBody={}, response={}",
                    method, requestUrl, body, json);

            return json;

        } catch (Exception e) {
            logger.error("[SINEVA] 호출 실패 method={}, url={}, body={}, error={}",
                    method, requestUrl, body, e.getMessage(), e);

            throw new ElidomRuntimeException("Sineva API 호출 실패");
        }
    }
}