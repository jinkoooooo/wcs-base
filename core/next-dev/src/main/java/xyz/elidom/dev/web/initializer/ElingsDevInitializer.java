/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.dev.web.initializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import xyz.elidom.dev.config.DevModuleProperties;
import xyz.elidom.sys.config.ModuleConfigSet;
import xyz.elidom.sys.system.config.module.IModuleProperties;
import xyz.elidom.sys.system.service.api.IEntityFieldCache;
import xyz.elidom.sys.system.service.api.IServiceFinder;

/**
 * Elings Dev Startup시 Framework 초기화 클래스 
 * 
 * @author shortstop
 */
@Component
public class ElingsDevInitializer {

	/**
	 * Logger
	 */
	private Logger logger = LoggerFactory.getLogger(ElingsDevInitializer.class);
	
	@Autowired
	@Qualifier("rest")
	private IServiceFinder restFinder;
	
	@Autowired
	private IEntityFieldCache entityFieldCache;
		
	@EventListener({ ContextRefreshedEvent.class })
	public void ready(ContextRefreshedEvent event) {
		logger.info("Dev module initializing ready...");
		IModuleProperties module = event.getApplicationContext().getBean(DevModuleProperties.class);
		ModuleConfigSet configSet = event.getApplicationContext().getBean(ModuleConfigSet.class);
		configSet.addConfig(module.getName(), module);
		this.scanServices(module);		
	}
	
	@EventListener({ApplicationReadyEvent.class})
    void contextRefreshedEvent(ApplicationReadyEvent event) {
		logger.info("Dev module initializing started...");
		logger.info("Dev module initializing finished");
    }
	
	/**
	 * 모듈 서비스 스캔 
	 * 
	 * @param module
	 */
	private void scanServices(IModuleProperties module) {
		this.entityFieldCache.scanEntityFieldsByBasePackage(module.getBasePackage());
		this.restFinder.scanServicesByPackage(module.getName(), module.getBasePackage());
	}
}