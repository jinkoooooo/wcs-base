/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.api.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import xyz.elidom.sys.system.service.api.IServiceCache;
import xyz.elidom.sys.system.service.def.IServiceDef;
import xyz.elidom.util.ValueUtil;

/**
 * REST Service API Cache - TODO XML 선언으로 수정 
 * 
 * @author shortstop
 */
@Component
@Qualifier("rest")
public class RestServiceCache implements IServiceCache {

	/**
	 * Cache
	 */
	private Map<String, IServiceDef> cache = new ConcurrentHashMap<String, IServiceDef>();

	@Override
	public Map<String, IServiceDef> getCache() {
		return this.cache;
	}
	
	@Override
	public boolean isExistService(String serviceId) {
		return this.cache.containsKey(serviceId);
	}
	
	@Override
	public IServiceDef getService(String serviceId) {
		return this.cache.containsKey(serviceId) ? this.cache.get(serviceId) : null;
	}

	@Override
	public void setService(IServiceDef serviceDef) {
		this.cache.put(serviceDef.getId(), serviceDef);
	}

	@Override
	public List<IServiceDef> getServiceList(String module) {
		List<IServiceDef> serviceList = new ArrayList<IServiceDef>();
		Iterator<IServiceDef> svcIter = this.cache.values().iterator();
		while(svcIter.hasNext()) {
			IServiceDef svc = svcIter.next();
			if(ValueUtil.isEmpty(module) || ValueUtil.isEqual(svc.getModule(), module)) {
				serviceList.add(svc);
			}
		}
		
		Collections.sort(serviceList, new ServiceSorter());
		return serviceList;
	}
	
	 /**
	  * Service Sorter 
	  * 
	  * @author shortstop
	  *
	  */
	static class ServiceSorter implements Comparator<IServiceDef> {
		@Override
		public int compare(IServiceDef svc1, IServiceDef svc2) {
			return svc1.getId().compareTo(svc2.getId());
		}
	}
}
