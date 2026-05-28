package operato.logis.connector.sap.mapper;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import operato.logis.connector.consts.ConnectorConfigConstants;
import operato.logis.connector.core.loader.MappingLoader;
import operato.logis.connector.core.mapper.BaseMappingRegistry;

@Component
public class SapMappingRegistry extends BaseMappingRegistry<SapMappingMetaData> {
	public SapMappingRegistry(@Value("${" + ConnectorConfigConstants.SAP_MAPPING_PROPERTY + ":" + ConnectorConfigConstants.SAP_MAPPING_DEFAULT + "}") String extDir) {
		super(new MappingLoader<>(extDir, ConnectorConfigConstants.SAP_MAPPING_CLASSPATH, SapMappingMetaData.class), SapMappingRegistry::makeKey);
	}

	private static String makeKey(SapMappingMetaData cfg) {
		return cfg.getFunctionName();
	}

	public Optional<SapMappingMetaData> get(String functionName) {
		return getByKey(functionName);
	}
}