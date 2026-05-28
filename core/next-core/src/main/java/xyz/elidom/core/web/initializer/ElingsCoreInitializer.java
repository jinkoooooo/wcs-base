/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.core.web.initializer;

import java.util.List;

import jakarta.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import xyz.elidom.core.config.CoreModuleProperties;
import xyz.elidom.core.entity.DataSrc;
import xyz.elidom.core.rest.DataSrcController;
import xyz.elidom.dbist.dml.Filter;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.config.ModuleConfigSet;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.rest.DomainController;
import xyz.elidom.sys.system.config.module.IModuleProperties;
import xyz.elidom.sys.system.context.DomainContext;
import xyz.elidom.sys.system.service.api.IEntityFieldCache;
import xyz.elidom.sys.system.service.api.IServiceFinder;
import xyz.elidom.sys.util.SysValueUtil;
import xyz.elidom.util.BeanUtil;

/**
 * Elings Core 모듈 Startup시 Framework 초기화 클래스 
 * 
 * @author shortstop
 */
@Component
public class ElingsCoreInitializer {

	/**
	 * Logger
	 */
	private Logger logger = LoggerFactory.getLogger(ElingsCoreInitializer.class);
	
	@Resource
	public Environment env;
	
	@Autowired
	@Qualifier("rest")
	private IServiceFinder restFinder;
	
	@Autowired
	private IEntityFieldCache entityFieldCache;	
	
	@EventListener({ ContextRefreshedEvent.class })
	public void ready(ContextRefreshedEvent event) {
		logger.info("Core module initializing ready...");
		IModuleProperties module = event.getApplicationContext().getBean(CoreModuleProperties.class);
		ModuleConfigSet configSet = event.getApplicationContext().getBean(ModuleConfigSet.class);
		configSet.addConfig(module.getName(), module);
		this.scanServices(module);
	}
	
	@EventListener({ApplicationReadyEvent.class})
    void contextRefreshedEvent(ApplicationReadyEvent event) {
		
		logger.info("Core module initializing started...");
		
		/**
		 * 메인 데이터소스가 아닌 3rd party 데이터소스 연결 시도 (비동기) 
		 */
		//ThreadUtil.doAsynch(() -> this.connectDataSrc());
		
		// 비동기로 실행할 경우 다른 initializer에서 실행하는 currentDomain이 섞일수 있어서 initializer내에서는 동기로 실행을 원칙으로 해야 할 것 같음.
		this.connectDataSrc();
				
		logger.info("Core module initializing finished");
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
	
	/**
	 * 메인 데이터소스가 아닌 3rd party 데이터소스 연결 시도 
	 */
	private void connectDataSrc() {
		// 모든 도메인 조회
		List<Domain> domainList = BeanUtil.get(DomainController.class).domainList();
		IQueryManager queryManager = BeanUtil.get(IQueryManager.class);
		DataSrcController dsCtrl = BeanUtil.get(DataSrcController.class);
		
		// 도메인 별 실행
		for(Domain domain : domainList) {
			DomainContext.setCurrentDomain(domain);
			try {
				this.initByDomain(domain, queryManager, dsCtrl);
			} finally {
				DomainContext.unsetAll();
			}
		}
	}
	
	/**
	 * 도메인 별 데이터소스 조회 후 초기 접속 처리
	 *  
	 * @param domain
	 * @param queryManager
	 * @param dsCtrl
	 */
	private void initByDomain(Domain domain, IQueryManager queryManager, DataSrcController dsCtrl) {
		Query query = new Query();
		query.addSelect(OrmConstants.ENTITY_FIELD_DOMAIN_ID, OrmConstants.ENTITY_FIELD_ID, OrmConstants.ENTITY_FIELD_NAME, OrmConstants.ENTITY_FIELD_STATUS);
		query.addFilter(OrmConstants.ENTITY_FIELD_DOMAIN_ID, domain.getId());
		query.addFilter(new Filter(OrmConstants.ENTITY_FIELD_STATUS, "noteq", DataSrc.STATUS_CLOSED));
		List<DataSrc> dataSrcList = queryManager.selectList(DataSrc.class, query);
		
		if(SysValueUtil.isNotEmpty(dataSrcList)) {
			for(DataSrc ds : dataSrcList) {
				try {
					logger.info("Connecting datasource [" + ds.getName() + "]");
					dsCtrl.initPool(ds.getId());
					logger.info("Connected datasource [" + ds.getName() + "]");
				} catch(Exception e) {
					logger.info("Failed to connect datasource [" + ds.getName() + "]", e);
					ds.setStatus(DataSrc.STATUS_ERROR);
					queryManager.update(ds, OrmConstants.ENTITY_FIELD_STATUS, OrmConstants.ENTITY_FIELD_UPDATED_AT);
				}
			}
		}
	}
}