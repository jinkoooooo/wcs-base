package operato.logis.connector.http.connector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.client.WebClient;

import operato.logis.connector.core.connector.IConnector;
import operato.logis.connector.core.event.EventDirection;
import operato.logis.connector.core.event.IntegrationEvent;
import operato.logis.connector.http.mapper.HttpMappingMetaData;
import operato.logis.connector.http.mapper.HttpMappingRegistry;
import operato.logis.connector.http.webclient.WebClientFactory;
import reactor.core.publisher.Mono;
import xyz.anythings.sys.rest.DynamicControllerSupport;
import xyz.elidom.sys.model.BaseResponse;

@Component
public class HttpConnector extends DynamicControllerSupport implements IConnector<Map<String, Object>> {

	private final WebClientFactory webClientFactory;
	private final HttpMappingRegistry mappingRegistry;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public HttpConnector(WebClientFactory webClientFactory, HttpMappingRegistry mappingRegistry) {
		this.webClientFactory = webClientFactory;
		this.mappingRegistry = mappingRegistry;
	}

	/* 1) 커넥터 지원 여부 판별 */
	@Override
	public boolean supports(String system, EventDirection dir) {
		return "ECS".equalsIgnoreCase(system) || "RCS".equalsIgnoreCase(system) || "MAX".equalsIgnoreCase(system);
	}

	/* 2) 실제 처리 */
	@Override
	public Mono<BaseResponse> handle(IntegrationEvent<Map<String, Object>> event) {
		return (event.getDirection() == EventDirection.SEND) ? handleSend(event) : handleReceive(event);
	}

	/* 2-1) SEND: 외부 시스템 호출 */
	private Mono<BaseResponse> handleSend(IntegrationEvent<Map<String, Object>> event) {

		HttpMappingMetaData meta = mappingRegistry.get(event.getSystem().name(), event.getDomainId())
				.orElseThrow(() -> new IllegalArgumentException("No MappingMetaData"));

		HttpMappingMetaData.MappingDetail detail = meta.getMappings().stream()
				.filter(m -> m.getEndpoint().equals(event.getEndpoint())).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("No MappingDetail for " + event.getEndpoint()));

		Map<String, Object> mappedPayload = applyFieldMapping(event.getPayload(), detail.getFieldMappings());

		WebClient client = webClientFactory.getClient(event.getSystem(), event.getDomainId());

		return client.post().uri(meta.getBaseUrl() + event.getEndpoint()).bodyValue(mappedPayload).retrieve()
				.bodyToMono(String.class).map(body -> new BaseResponse(true, "SUCCESS", body))
				.doOnNext(r -> logger.info("HTTP 전송 성공 url={}, domainId={}", event.getEndpoint(), event.getDomainId()))
				.onErrorResume(e -> {
					logger.error("HTTP 전송 실패 : {}", e.getMessage());
					return Mono.just(new BaseResponse(false, e.getMessage()));
				});
	}

	/* 2-2) RECEIVE: 내부 이벤트 발행 */
	private Mono<BaseResponse> handleReceive(IntegrationEvent<Map<String, Object>> event) {

		HttpMappingMetaData meta = mappingRegistry.get(event.getSystem().name(), event.getDomainId())
				.orElseThrow(() -> new IllegalArgumentException("No MappingMetaData"));

		HttpMappingMetaData.MappingDetail detail = meta.getMappings().stream()
				.filter(m -> m.getEndpoint().equals(event.getEndpoint())).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("No MappingDetail for " + event.getEndpoint()));

		Map<String, Object> mappedPayload = applyFieldMapping(event.getPayload(), detail.getFieldMappings());

		logger.info("DeviceProcessRestEvent 발행 path={}", detail.getEventPath());

		return null;
	}

	/* 필드 매핑 유틸 */
	private Map<String, Object> applyFieldMapping(Map<String, Object> payload,
			List<HttpMappingMetaData.FieldMapping> mappings) {
		Map<String, Object> result = new HashMap<>();
		for (HttpMappingMetaData.FieldMapping m : mappings) {
			Object v = payload.get(m.getSource());
			if (v != null)
				result.put(m.getTarget(), v);
		}
		return result;
	}
}