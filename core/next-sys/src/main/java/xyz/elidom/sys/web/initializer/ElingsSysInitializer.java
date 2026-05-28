/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.web.initializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import xyz.elidom.sys.SysConfigConstants;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.config.ModuleConfigSet;
import xyz.elidom.sys.config.SysModuleProperties;
import xyz.elidom.sys.msg.basic.BasicEntityTranslator;
import xyz.elidom.sys.msg.basic.BasicMessageTranslator;
import xyz.elidom.sys.msg.basic.BasicTermTranslator;
import xyz.elidom.sys.system.auth.AuthProviderFactory;
import xyz.elidom.sys.system.auth.unauth.UnauthProvider;
import xyz.elidom.sys.system.engine.TemplateEngineManager;
import xyz.elidom.sys.system.engine.excel.ExcelTemplateEngine;
import xyz.elidom.sys.system.engine.velocity.VelocityTemplateEngine;
import xyz.elidom.sys.system.service.api.IEntityFieldCache;
import xyz.elidom.sys.system.service.api.IServiceFinder;
import xyz.elidom.sys.util.MessageUtil;
import xyz.elidom.sys.util.SettingUtil;
import xyz.elidom.sys.util.SysValueUtil;
import xyz.elidom.util.BeanUtil;

/**
 * Elings Sys 모듈 Startup시 Framework 초기화 클래스 
 * 
 * @author shortstop
 */
@Component
public class ElingsSysInitializer {

	/**
	 * Logger
	 */
	private Logger logger = LoggerFactory.getLogger(ElingsSysInitializer.class);
	
	@Resource
	public Environment env;
	
	@Autowired
	private ModuleConfigSet configSet;
	
	@Autowired
	private SysModuleProperties module;
	
	@Autowired
	@Qualifier("rest")
	private IServiceFinder restFinder;
	
	@Autowired
	private IEntityFieldCache entityFieldCache;
	
	@Autowired
	private TemplateEngineManager templateEngineMgr;
	
	@Autowired
	private AuthProviderFactory authProviderFactory;
	
	@EventListener({ ContextRefreshedEvent.class })
	public void ready(ContextRefreshedEvent event) {
		this.logger.info("Sys module initializing ready...");
		this.configSet.addConfig(this.module.getName(), this.module);
		this.scanServices();
	}
	
	@EventListener({ApplicationReadyEvent.class})
    void contextRefreshedEvent(ApplicationReadyEvent event) {
		this.logger.info("Sys module initializing started...");
		
		this.sysSettings();
		
		this.logger.info("Sys module initializing finished");
    }
	
	/**
	 * system 모듈 settings
	 */
	private void sysSettings() {
		/**
		 * 엔티티 저장 시 Validation Check를 할 지 여부를 Properties 파일로 부터 읽어서 설정한다.
		 */
		boolean entityValidateBeforeSave = SysValueUtil.toBoolean(this.env.getProperty(SysConfigConstants.DBIST_VALIDATE_BEFORE_SAVE, "false"));
		SettingUtil.setEntityValidateBeforeSave(entityValidateBeforeSave);
		
		/**
		 * Velocity Template Engine 등록 
		 */
		VelocityTemplateEngine velocityEngine = BeanUtil.get(VelocityTemplateEngine.class);
		this.templateEngineMgr.addTemplateEngine("excel", velocityEngine);
		
		/**
		 * Excel Template Engine 등록
		 */
		ExcelTemplateEngine excelEngine = BeanUtil.get(ExcelTemplateEngine.class);
		this.templateEngineMgr.addTemplateEngine("basic", excelEngine);
		
		/**
		 * Server가 정상적으로 수행되면 Ready 상태를 true 변경
		 */
		System.setProperty(SysConstants.SERVER_STARTED, SysConstants.TRUE_STRING);
		
		/**
		 * 기본적으로 인증을 지원하지 않는 AuthProvider를 설정 - 인증 로직을 변경하는 요소
		 */
		this.authProviderFactory.setAuthProvider(new UnauthProvider());
		
		/**
		 * MessageUtil에 기본 메시지 번역기 설정 
		 */
		MessageUtil.setMsgTranslator(BeanUtil.get(BasicMessageTranslator.class));
		/**
		 * MessageUtil에 기본 용어 번역기 설정 
		 */
		MessageUtil.setTermTranslator(BeanUtil.get(BasicTermTranslator.class));
		/**
		 * MessageUtil에 기본 엔티티 기반 번역기 설정 
		 */
		MessageUtil.setEntityTranslator(BeanUtil.get(BasicEntityTranslator.class));

	}
	
	/**
	 * 모듈 서비스 스캔
	 */
	private void scanServices() {
		this.entityFieldCache.scanEntityFieldsByBasePackage(this.module.getBasePackage());
		this.restFinder.scanServicesByPackage(this.module.getName(), this.module.getBasePackage());
	}
	
}