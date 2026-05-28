package operato.logis.lms.web.initializer;

import operato.logis.lms.config.ModuleProperties;
import operato.logis.lms.query.LmsQueryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.sys.config.ModuleConfigSet;
import xyz.elidom.sys.system.service.api.IEntityFieldCache;
import xyz.elidom.sys.system.service.api.IServiceFinder;

/**
 * Operato Logis WCS Startup시 Framework 초기화 클래스
 *
 * @author yang
 */
@Component
public class LogisLmsInitializer {

    /**
     * Logger
     */
    private Logger logger = LoggerFactory.getLogger(LogisLmsInitializer.class);

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

    @Autowired
    private LmsQueryStore lmsQueryStore;

    @EventListener({ ContextRefreshedEvent.class })
    public void refresh(ContextRefreshedEvent event) {
        this.logger.info("Logistics Monitoirng System module refreshing...");

        this.configSet.addConfig(this.module.getName(), this.module);
        this.scanServices();

        this.logger.info("Logistics Monitoring System module refreshed!");
    }

    @EventListener({ApplicationReadyEvent.class})
    void ready(ApplicationReadyEvent event) {
        this.logger.info("Logistics Monitoring System module initializing...");

        this.initQueryStores();

        this.logger.info("Logistics Monitoring System module initialized!");
    }

    /**
     * 모듈 서비스 스캔
     */
    private void scanServices() {
        this.entityFieldCache.scanEntityFieldsByBasePackage(this.module.getBasePackage());
        this.restFinder.scanServicesByPackage(this.module.getName(), this.module.getBasePackage());
    }

    /**
     * 쿼리 스토어 초기화
     */
    private void initQueryStores() {
        String dbType = this.queryManager.getDbType();
        this.lmsQueryStore.initQueryStore(dbType);
    }
}