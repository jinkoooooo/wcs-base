/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.api;

import java.util.List;

import xyz.elidom.sys.system.service.def.IServiceApi;
import xyz.elidom.sys.system.service.def.IServiceDef;

/**
 * Service Finder 
 * 
 * @author shortstop
 */
public interface IServiceFinder {
	
	/**
	 * [실행]을 위해 요청 URL로 서비스 API를 찾아 리턴 
	 * 
	 * @param fullUrl
	 * @return
	 */
	public IServiceApi getServiceApi(String fullUrl);
		
	/**
	 * [API 명세]를 위해 요청 URL로 서비스 정의 상세 리턴 
	 * 
	 * @param serviceId
	 * @return
	 */
	public IServiceDef getServiceDetail(String serviceId);
	
	/**
	 * [API 명세]를 위해 요청 URL로 서비스 API를 찾아 리턴 
	 * 
	 * @param apiId
	 * @return
	 */
	public IServiceApi getServiceApiDetail(String apiId);	
	
	/**
	 * [API 명세]를 위해 모듈명으로 서비스 찾아 리턴 
	 * 
	 * @param module
	 * @return
	 */
	public List<IServiceDef> getServicesByModule(String module);
	
	/**
	 * module명으로 base package로 서비스 스캔
	 * 
	 * @param module
	 * @param basePackage
	 */
	public void scanServicesByPackage(String module, String basePackage);
	
}
