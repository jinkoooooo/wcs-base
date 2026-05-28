/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.def.basic;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import xyz.elidom.sys.system.service.def.IServiceApi;
import xyz.elidom.sys.system.service.def.IServiceDef;
import xyz.elidom.util.ValueUtil;

/**
 * 기본 IServiceDef 구현 
 * 
 * @author shortstop
 */
public class ServiceDef implements IServiceDef {

	private String module;
	private String id;
	private String name;
	private String description;
	@JsonIgnore
	private transient int apiCount;
	private String beanClassName;
	@JsonIgnore
	private transient Class<?> beanClass;
	@JsonIgnore
	private transient Object bean;
	private List<IServiceApi> apiList;
	
	@Override
	public String getModule() {
		return this.module;
	}

	@Override
	public void setModule(String module) {
		this.module = module;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public Object getBean() {
		return this.bean;
	}

	@Override
	public void setBean(Object bean) {
		this.bean = bean;
	}
	
	@Override
	public Class<?> getBeanClass() {
		return this.beanClass;
	}

	@Override
	public void setBeanClass(Class<?> beanClass) {
		this.beanClass = beanClass;
	}	

	@Override
	public String getBeanClassName() {
		return this.beanClassName;
	}

	@Override
	public void setBeanClassName(String beanClassName) {
		this.beanClassName = beanClassName;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public List<IServiceApi> getApiList() {
		return this.apiList;
	}

	@Override
	public void setApiList(List<IServiceApi> apiList) {
		this.apiList = apiList;
	}

	@Override
	public void addApi(IServiceApi api) {
		if(this.apiList == null) {
			this.apiList = new ArrayList<IServiceApi>();
		}
		
		this.apiList.add(api);
	}

	@Override
	public int getApiCount() {
		return this.apiCount;
	}

	@Override
	public void setApiCount(int apiCount) {
		this.apiCount = apiCount;
	}

	@Override
	public boolean isApiScannedAll() {
		if(this.apiCount > 0 && (this.apiList == null || this.apiList.isEmpty()))
			return false;
		
		return this.apiList.size() == this.apiCount;
	}
	
	@Override
	public IServiceApi getApiById(String apiId) {
		return this.getApiBy("id", apiId);
	}	

	@Override
	public IServiceApi getApiByMethod(String methodName) {
		return this.getApiBy("methodName", methodName);
	}
	
	private IServiceApi getApiBy(String fieldName, String fieldValue) {
		if(ValueUtil.isEmpty(this.apiList)) {
			return null;
		}
		
		IServiceApi foundApi = null;
				
		for(IServiceApi api : apiList) {
			Object value = null;
			
			if(fieldName.equals("id")) {
				value = api.getId();
			} else if(fieldName.equals("methodName")) {
				value = api.getMethod().getName();
			}
			
			if(ValueUtil.isEqual(fieldValue, value)) {
				foundApi = api;
				break;
			}
		}
		
		return foundApi;		
	}

}
