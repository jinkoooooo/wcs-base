/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.anythings.sec.web.initializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import xyz.anythings.sec.config.AnySecModuleProperties;
import xyz.elidom.sec.system.auth.SecAuthProvider;
import xyz.elidom.sys.config.ModuleConfigSet;
import xyz.elidom.sys.system.auth.AuthProviderFactory;
import xyz.elidom.sys.system.service.api.IEntityFieldCache;
import xyz.elidom.sys.system.service.api.IServiceFinder;

/**
 * Sec 모듈 Startup시 Framework 초기화 클래스 
 * 
 * @author shortstop
 */
@Component
public class AnythingsSecInitializer {
	/**
	 * Logger
	 */
	private Logger logger = LoggerFactory.getLogger(AnythingsSecInitializer.class);
	
	@Autowired
	private ModuleConfigSet configSet;
	
	@Autowired
	private AnySecModuleProperties module;
	
	@Autowired
	private AuthProviderFactory authProviderFactory;
	
	@Autowired
	@Qualifier("rest")
	private IServiceFinder restFinder;
	
	@Autowired
	private IEntityFieldCache entityFieldCache;	
	
	@EventListener({ ContextRefreshedEvent.class })
	public void ready(ContextRefreshedEvent event) {
		this.logger.info("Anythings Sec module initializing ready...");
		
		this.configSet.addConfig(this.module.getName(), this.module);
		
		this.scanServices();
	}
	
	@EventListener({ApplicationReadyEvent.class})
	void contextRefreshedEvent(ApplicationReadyEvent event) {
		this.logger.info("Anythings Sec module initializing started...");
		
		// Auth Provider 설정 
		this.authProviderFactory.setAuthProvider(new SecAuthProvider());
		
		this.logger.info("Anythings Sec module initializing finished");
	}
	
	/**
	 * 모듈 서비스 스캔
	 */
	private void scanServices() {
		this.entityFieldCache.scanEntityFieldsByBasePackage(this.module.getBasePackage());
		this.restFinder.scanServicesByPackage(this.module.getName(), this.module.getBasePackage());
	}
}