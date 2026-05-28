package operato.logis.connector.http.mapper;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import operato.logis.connector.consts.ConnectorConfigConstants;
import operato.logis.connector.core.loader.MappingLoader;
import operato.logis.connector.core.mapper.BaseMappingRegistry;

@Component
public class HttpMappingRegistry extends BaseMappingRegistry<HttpMappingMetaData> {
	public HttpMappingRegistry(@Value("${" + ConnectorConfigConstants.HTTP_MAPPING_PROPERTY + ":" + ConnectorConfigConstants.HTTP_MAPPING_DEFAULT + "}") String extDir) {
		super(new MappingLoader<>(extDir, ConnectorConfigConstants.HTTP_MAPPING_CLASSPATH, HttpMappingMetaData.class), HttpMappingRegistry::makeKey);
	}

	private static String makeKey(HttpMappingMetaData meta) {
		return meta.getSystem().toLowerCase() + ":" + meta.getDomainId();
	}

	public Optional<HttpMappingMetaData> get(String system, long domainId) {
		String key = system.toLowerCase() + ":" + domainId;
		return getByKey(key);
	}
}