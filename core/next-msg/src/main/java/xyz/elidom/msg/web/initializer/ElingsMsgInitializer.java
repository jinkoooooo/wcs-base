/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.msg.web.initializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import xyz.elidom.msg.config.MsgModuleProperties;
import xyz.elidom.msg.system.msg.MsgMessageTranslator;
import xyz.elidom.msg.system.msg.MsgTermTranslator;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.config.ModuleConfigSet;
import xyz.elidom.sys.system.service.api.IEntityFieldCache;
import xyz.elidom.sys.system.service.api.IServiceFinder;
import xyz.elidom.sys.util.MessageUtil;
import xyz.elidom.util.BeanUtil;

/**
 * Elings Msg 모듈 Startup시 Framework 초기화 클래스 
 * 
 * @author shortstop
 */
@Component
public class ElingsMsgInitializer {

	/**
	 * Logger
	 */
	private Logger logger = LoggerFactory.getLogger(ElingsMsgInitializer.class);
	
	@Autowired
	@Qualifier("rest")
	private IServiceFinder restFinder;
	
	@Autowired
	private IEntityFieldCache entityFieldCache;
	
	@Autowired
	private MsgModuleProperties module;
	
	@Autowired
	private ModuleConfigSet configSet;
	
	
	@EventListener({ ContextRefreshedEvent.class })
	public void ready(ContextRefreshedEvent event) {
		this.logger.info("Msg module initializing ready...");
		this.configSet.addConfig(this.module.getName(), this.module);
		this.scanServices();
	}
	
	@EventListener({ApplicationReadyEvent.class})
    void contextRefreshedEvent(ApplicationReadyEvent event) {
		this.logger.info("Msg module initializing started...");
		
		System.setProperty(SysConstants.SERVER_STARTED, "true");
		
		this.msgSettings();
		
		this.logger.info("Msg module initializing finished");
    }
	
	/**
	 * 메시지 모듈에서의 설정 
	 */
	private void msgSettings() {
		// 수정1. 메시지, 용어 미리 로딩하지 않음 - 화면에서 호출시 로딩
		/**
		 * 메시지 로딩
		 */
		//BeanUtil.get(MessageController.class).all();
		
		/**
		 * 용어 로딩
		 */
		//BeanUtil.get(TerminologyController.class).all();
		
		/**
		 * 기본 메시지 번역기 설정 
		 */
		MessageUtil.setMsgTranslator(BeanUtil.get(MsgMessageTranslator.class));
		
		/**
		 * 기본 용어 번역기 설정 
		 */
		MessageUtil.setTermTranslator(BeanUtil.get(MsgTermTranslator.class));
	}	
	
	/**
	 * 모듈 서비스 스캔 
	 */
	private void scanServices() {
		this.entityFieldCache.scanEntityFieldsByBasePackage(this.module.getBasePackage());
		this.restFinder.scanServicesByPackage(this.module.getName(), this.module.getBasePackage());
	}

}