/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.api;

import java.util.List;
import java.util.Map;

import xyz.elidom.sys.system.service.def.IServiceDef;

/**
 * 서비스를 Caching 하는 인터페이스 
 * 
 * @author shortstop
 */
public interface IServiceCache {
	
	/**
	 * Cache를 리턴 
	 * 
	 * @return
	 */
	public Map<String, IServiceDef> getCache();
	
	/**
	 * serviceId가 존재하는지 체크 
	 * 
	 * @param serviceId
	 * @return
	 */
	public boolean isExistService(String serviceId);

	/**
	 * serviceId로 Cache에서 서비스를 찾아 리턴 
	 *  
	 * @param serviceId
	 * @return
	 */
	public IServiceDef getService(String serviceId);
	
	/**
	 * 서비스를 Cache에 추가 
	 * 
	 * @param serviceDef
	 */
	public void setService(IServiceDef serviceDef);
	
	/**
	 * 모듈별 서비스 리스트를 리턴 
	 * 
	 * @param module
	 * @return
	 */
	public List<IServiceDef> getServiceList(String module);
}
