package operato.logis.connector.initializer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "connector.sap", name = "enabled", havingValue = "true", matchIfMissing = false)
@ComponentScan("operato.logis.connector.sap")
public class SapModuleToggleConfiguration {
}
