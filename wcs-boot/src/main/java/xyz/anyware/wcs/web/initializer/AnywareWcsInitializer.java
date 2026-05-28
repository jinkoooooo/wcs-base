/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.anyware.wcs.web.initializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import xyz.anyware.wcs.config.ModuleProperties;
import xyz.anyware.wcs.query.store.VueQueryStore;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.sys.config.ModuleConfigSet;
import xyz.elidom.sys.system.config.module.IModuleProperties;
import xyz.elidom.sys.system.service.api.IEntityFieldCache;
import xyz.elidom.sys.system.service.api.IServiceFinder;

/**
 * Anyware WCS 모듈 Startup시 Framework 초기화 클래스
 * 
 * @author yang
 */
@Component
public class AnywareWcsInitializer {

	/**
	 * Logger
	 */
	private Logger logger = LoggerFactory.getLogger(AnywareWcsInitializer.class);

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
	private VueQueryStore vueQueryStore;


	@EventListener({ ContextRefreshedEvent.class })
	public void refresh(ContextRefreshedEvent event) {
		this.logger.info("Anyware WCS module refreshing...");
		
		this.setupApplicationModule();
		
		this.logger.info("Anyware WCS module refreshed!");
	}

	@EventListener({ ApplicationReadyEvent.class })
	void ready(ApplicationReadyEvent event) {
		this.logger.info("Anyware WCS module initializing...");
		
		this.setupApplicationModule();
		this.scanServices();
		this.initQueryStores();
		
		this.logger.info("Anyware WCS module initialized!");
	}
	
	/**
	 * 애플리케이션 메인 모듈 셋업
	 */
	private void setupApplicationModule() {
		IModuleProperties mainModule = this.configSet.getApplicationModule();
		if(mainModule == null) {
			this.configSet.addConfig(this.module.getName(), this.module);
			this.configSet.setApplicationModule(this.module.getName());
		}
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
		this.vueQueryStore.initQueryStore(dbType);
	}
}