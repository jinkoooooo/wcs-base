/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.base.web.initializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import xyz.elidom.base.config.BaseModuleProperties;
import xyz.elidom.base.query.SysQueryStore;
import xyz.elidom.base.system.msg.EntityBasedTranslator;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.sys.config.ModuleConfigSet;
import xyz.elidom.sys.system.service.api.IEntityFieldCache;
import xyz.elidom.sys.system.service.api.IServiceFinder;
import xyz.elidom.sys.util.MessageUtil;
import xyz.elidom.util.BeanUtil;

/**
 * Base 모듈 Startup시 Framework 초기화 클래스 
 * 
 * @author shortstop
 */
@Component
public class ElingsBaseInitializer {

	/**
	 * Logger
	 */
	private Logger logger = LoggerFactory.getLogger(ElingsBaseInitializer.class);
	
	@Autowired
	private BaseModuleProperties module;
	
	@Autowired
	private ModuleConfigSet configSet;

	@Autowired
	@Qualifier("rest")
	private IServiceFinder restFinder;
	
	@Autowired
	private IEntityFieldCache entityFieldCache;

	@Autowired
	private SysQueryStore sysQueryStore;

	@Autowired
	private IQueryManager queryManager;
	
	@EventListener({ ContextRefreshedEvent.class })
	public void ready(ContextRefreshedEvent event) {
		this.logger.info("Base module initializing ready...");
		this.configSet.addConfig(this.module.getName(), this.module);

		String dbType = this.queryManager.getDbType();
		sysQueryStore.initQueryStore(dbType);

		this.scanServices();		
	}
	
	@EventListener({ApplicationReadyEvent.class})
    void contextRefreshedEvent(ApplicationReadyEvent event) {
		logger.info("Base module initializing started...");
		
		/**
		 * MessageUtil에 기본 엔티티 기반 번역기 설정 
		 */
		MessageUtil.setEntityTranslator(BeanUtil.get(EntityBasedTranslator.class));
		
		logger.info("Base module initializing finished");
    }
	
	/**
	 * 모듈 서비스 스캔 
	 */
	private void scanServices() {
		this.entityFieldCache.scanEntityFieldsByBasePackage(this.module.getBasePackage());
		this.restFinder.scanServicesByPackage(this.module.getName(), this.module.getBasePackage());
	}
}