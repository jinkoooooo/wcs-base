package operato.logis.connector.initializer;

import operato.logis.connector.config.ModuleProperties;
import operato.logis.connector.sap.query.store.ConnectorSapQueryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import xyz.elidom.orm.IQueryManager;
import xyz.elidom.sys.config.ModuleConfigSet;
import xyz.elidom.sys.system.service.api.IEntityFieldCache;
import xyz.elidom.sys.system.service.api.IServiceFinder;

/**
 * Operato Logis Connector Startup시 Framework 초기화 클래스
 * 
 * @author yang
 */
@Component
public class OperatoLogisConnectorInitializer {

	/**
	 * Logger
	 */
	private Logger logger = LoggerFactory.getLogger(OperatoLogisConnectorInitializer.class);
	
	@Autowired
	@Qualifier("rest")
	private IServiceFinder restFinder;
	
	@Autowired
	private IEntityFieldCache entityFieldCache;
	
	@Autowired
	private ModuleProperties module;
	
	@Autowired
	private ModuleConfigSet configSet;
	
	@Autowired
	private IQueryManager queryManager;

	@Autowired(required = false)
	private ConnectorSapQueryStore connectorSapQueryStore;

	@Value("${connector.sap.enabled:false}")
	private boolean sapEnabled;
	
	@EventListener({ ContextRefreshedEvent.class })
	public void refresh(ContextRefreshedEvent event) {
		this.logger.info("Operato Logistics Connector module refreshing...");
		
		this.configSet.addConfig(this.module.getName(), this.module);
		this.scanServices();

		this.logger.info("Operato Logistics Connector module refreshed!");
	}
	
	@EventListener({ApplicationReadyEvent.class})
	void ready(ApplicationReadyEvent event) {
		this.logger.info("Operato Logistics Connector module initializing...");
		
		this.initQueryStores();
		
		this.logger.info("Operato Logistics Connector module initialized!");
	}
	
	/**
	 * 모듈 서비스 스캔 
	 */
	private void scanServices() {
		this.entityFieldCache.scanEntityFieldsByBasePackage(this.module.getBasePackage());
		this.restFinder.scanServicesByPackage(this.module.getName(), this.module.getBasePackage());
	}
	
	/**
	 * SAP 쿼리 스토어 초기화
	 */
	private void initQueryStores() {
		if (sapEnabled) {

			String dbType = this.queryManager.getDbType();
			this.connectorSapQueryStore.initQueryStore(dbType);
		}
	}
	
}
