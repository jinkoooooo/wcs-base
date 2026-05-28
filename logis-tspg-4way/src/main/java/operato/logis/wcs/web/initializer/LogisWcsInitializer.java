package operato.logis.wcs.web.initializer;

import operato.logis.wcs.config.ModuleProperties;
import operato.logis.wcs.query.WcsQueryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * WCS 모듈 부팅 시 프레임워크 초기화 (서비스 스캔·쿼리 스토어 로딩).
 */
@Component
public class LogisWcsInitializer {

    private static final Logger logger = LoggerFactory.getLogger(LogisWcsInitializer.class);

    private final IServiceFinder restFinder;
    private final IEntityFieldCache entityFieldCache;
    private final ModuleProperties module;
    private final ModuleConfigSet configSet;
    private final IQueryManager queryManager;
    private final WcsQueryStore wcsQueryStore;

    public LogisWcsInitializer(@Qualifier("rest") IServiceFinder restFinder,
                               IEntityFieldCache entityFieldCache,
                               ModuleProperties module,
                               ModuleConfigSet configSet,
                               IQueryManager queryManager,
                               WcsQueryStore wcsQueryStore) {
        this.restFinder = restFinder;
        this.entityFieldCache = entityFieldCache;
        this.module = module;
        this.configSet = configSet;
        this.queryManager = queryManager;
        this.wcsQueryStore = wcsQueryStore;
    }

    /** ContextRefreshed 시 모듈 설정 등록 + 서비스 스캔. */
    @EventListener({ ContextRefreshedEvent.class })
    public void refresh(ContextRefreshedEvent event) {
        logger.info("[ Init ][ Module ] refresh start - module={}", this.module.getName());

        this.configSet.addConfig(this.module.getName(), this.module);
        this.scanServices();

        logger.info("[ Init ][ Module ] refresh completed - module={}", this.module.getName());
    }

    /** ApplicationReady 시 쿼리 스토어 초기화. */
    @EventListener({ApplicationReadyEvent.class})
    void ready(ApplicationReadyEvent event) {
        logger.info("[ Init ][ Module ] ready start - module={}", this.module.getName());

        this.initQueryStores();

        logger.info("[ Init ][ Module ] ready completed - module={}", this.module.getName());
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
        this.wcsQueryStore.initQueryStore(dbType);
    }
}