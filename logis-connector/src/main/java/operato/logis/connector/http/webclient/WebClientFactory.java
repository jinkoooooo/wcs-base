package operato.logis.connector.http.webclient;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import operato.logis.connector.core.event.SystemType;
import operato.logis.connector.http.mapper.HttpMappingMetaData;
import operato.logis.connector.http.mapper.HttpMappingRegistry;

/**
 * 시스템과 테넌시 조합에 맞는 WebClient를 생성하고 캐싱하는 역할을 담당. WebClient는 매번 새로 생성하지 않고,
 * system+tenancy 조합별로 재사용한다. BaseUrl은 MappingRegistry에 등록된 JSON 메타 정보를 기준으로
 * 설정한다.
 */
@Component
public class WebClientFactory {

	@Autowired
	private HttpMappingRegistry mappingRegistry; // 매핑 레지스트리 (system+tenancy 기반으로 정보 조회)
	private final Map<String, WebClient> clientMap = new HashMap<>(); // system+tenancy 별 WebClient 캐시 저장소

	/**
	 * system과 tenancy를 기반으로 WebClient를 가져온다. 없으면 MappingRegistry에서 정보를 조회해서 새로 만든다.
	 *
	 * @param system   외부 시스템 종류 (예: ECS, SAP, RCS)
	 * @param domainId domainId 구분자 (예: 6, 7)
	 * @return WebClient 객체
	 */
	public WebClient getClient(SystemType system, long domainId) {
		String key = (system.name() + "_" + domainId).toLowerCase(); // 키 생성: 예) ecs_branda

		// 이미 생성된 WebClient가 있으면 바로 반환
		if (clientMap.containsKey(key)) {
			return clientMap.get(key);
		}

		// 매핑 레지스트리에서 system+tenancy 조합으로 매핑 메타데이터 조회
		HttpMappingMetaData meta = mappingRegistry.get(system.name(), domainId)
				.orElseThrow(() -> new IllegalArgumentException(
						"No mapping metadata found for system: " + system + ", tenancy: " + domainId));

		String baseUrl = meta.getBaseUrl();
		if (baseUrl == null) {
			throw new IllegalArgumentException("No baseUrl found for system: " + system + ", tenancy: " + domainId);
		}

		// WebClient 새로 생성
		WebClient client = WebClient.builder().baseUrl(baseUrl) // baseUrl 설정
				.build();

		clientMap.put(key, client); // 캐시에 저장
		return client;
	}
}