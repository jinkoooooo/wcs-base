/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.def.basic;

import java.lang.reflect.Method;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import xyz.elidom.sys.system.service.def.FieldDef;
import xyz.elidom.sys.system.service.def.IServiceApi;
import xyz.elidom.sys.system.service.def.IServiceDef;

/**
 * IServiceApi 기본 구현 
 * 
 * @author shortstop
 */
public class ServiceApi implements IServiceApi {

	@JsonIgnore
	private transient IServiceDef serviceDef;
	private String serviceId;
	private String id;
	private String name;
	private String description;
	private String url;
	private String wsUrl;
	private String httpMethod;
	@JsonIgnore
	private transient Method method;
	@JsonIgnore
	private transient Class<?> inputClass;
	@JsonIgnore
	private transient Class<?> outputClass;
	private List<FieldDef> inputTypeList;
	private List<FieldDef> outputTypeList;
	
	@Override
	public IServiceDef getServiceDef() {
		return this.serviceDef;
	}

	@Override
	public void setServiceDef(IServiceDef serviceDef) {
		this.serviceDef = serviceDef;
		this.serviceId = serviceDef.getId();
	}
	
	@Override
	public String getServiceId() {
		return this.serviceId;
	}

	@Override
	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
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
	public String getUrl() {
		return this.url;
	}

	@Override
	public void setUrl(String url) {
		this.url = url;
	}

	@Override
	public String getWsUrl() {
		return this.wsUrl;
	}

	@Override
	public void setWsUrl(String wsUrl) {
		this.wsUrl = wsUrl;
	}

	@Override
	public String getHttpMethod() {
		return this.httpMethod;
	}

	@Override
	public void setHttpMethod(String httpMethod) {
		this.httpMethod = httpMethod;
	}

	@Override
	public Method getMethod() {
		return this.method;
	}

	@Override
	public void setMethod(Method method) {
		this.method = method;
	}

	@Override
	public void setInputClass(Class<?> clazz) {
		this.inputClass = clazz;
	}

	@Override
	public Class<?> getInputClass() {
		return this.inputClass;
	}

	@Override
	public void setOutputClass(Class<?> clazz) {
		this.outputClass = clazz;
	}

	@Override
	public Class<?> getOutputClass() {
		return this.outputClass;
	}

	@Override
	public List<FieldDef> getInputTypeList() {
		return this.inputTypeList;
	}

	@Override
	public void setInputTypeList(List<FieldDef> inputTypeList) {
		this.inputTypeList = inputTypeList;
	}

	@Override
	public List<FieldDef> getOutputTypeList() {
		return this.outputTypeList;
	}

	@Override
	public void setOutputTypeList(List<FieldDef> outputTypeList) {
		this.outputTypeList = outputTypeList;
	}

}
