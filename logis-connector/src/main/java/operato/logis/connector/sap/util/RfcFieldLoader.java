package operato.logis.connector.sap.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import operato.logis.connector.sap.mapper.SapMappingMetaData;
import operato.logis.connector.sap.mapper.SapMappingRegistry;

@Component
public class RfcFieldLoader {
	@Autowired
	private SapMappingRegistry sapRegistry;

    /**
     * 클래스패스에서 JSON을 읽어 SapRfcFieldConfig로 매핑
     */
    public SapMappingMetaData loadMetaData(String rfcName) throws RuntimeException {
    	return sapRegistry.get(rfcName)
	    .orElseThrow(() -> new RuntimeException("SAP config not found: " + rfcName));
    }
}
