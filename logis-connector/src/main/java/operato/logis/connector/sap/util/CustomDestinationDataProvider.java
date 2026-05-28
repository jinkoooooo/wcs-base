package operato.logis.connector.sap.util;

import com.sap.conn.jco.ext.DestinationDataProvider;
import com.sap.conn.jco.ext.DestinationDataEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class CustomDestinationDataProvider implements DestinationDataProvider {

    private final Map<String, Properties> destinations = new HashMap<>();

    public void addDestination(String name, Properties props) {
        destinations.put(name, props);
    }

    @Override
    public Properties getDestinationProperties(String destinationName) {
        return destinations.get(destinationName);
    }

    @Override
    public void setDestinationDataEventListener(DestinationDataEventListener eventListener) {
        // 사용하지 않음
    }

    @Override
    public boolean supportsEvents() {
        return false;
    }
}
