/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.def;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Service API 정의
 * 
 * @author shortstop
 */
public interface IServiceApi {

	/**
	 * 서비스 정의
	 * 
	 * @return
	 */
	public IServiceDef getServiceDef();
	
	/**
	 * 서비스 정의
	 * 
	 * @param serviceDef
	 */
	public void setServiceDef(IServiceDef serviceDef);
	
	/**
	 * Service Id
	 * 
	 * @return
	 */
	public String getServiceId();
	
	/**
	 * Service Id
	 * 
	 * @param serviceId
	 */
	public void setServiceId(String serviceId);
	
	/**
	 * API 아이디
	 * 
	 * @return
	 */
	public String getId();

	/**
	 * API 아이디
	 * 
	 * @param id
	 */
	public void setId(String id);
	
	/**
	 * API 명
	 * 
	 * @return
	 */
	public String getName();

	/**
	 * API 명
	 * 
	 * @param name
	 */
	public void setName(String name);
	
	/**
	 * API 설명
	 * 
	 * @return
	 */
	public String getDescription();
	
	/**
	 * API 설명
	 * 
	 * @param description
	 */
	public void setDescription(String description);
	
	/**
	 * HTTP 서비스 URL
	 * 
	 * @return
	 */
	public String getUrl();
	
	/**
	 * HTTP 서비스 URL
	 * 
	 * @param url
	 */
	public void setUrl(String url);
	
	/**
	 * Web Socket 서비스 URL
	 * 
	 * @return
	 */
	public String getWsUrl();
	
	/**
	 * Web Socket 서비스 URL
	 * 
	 * @param wsUrl
	 */
	public void setWsUrl(String wsUrl);
	
	/**
	 * HTTP METHOD
	 * 
	 * @return
	 */
	public String getHttpMethod();
	
	/**
	 * HTTP METHOD
	 * 
	 * @param httpMethod
	 */
	public void setHttpMethod(String httpMethod);
	
	/**
	 * Service Method to Invoke
	 * 
	 * @return
	 */
	public Method getMethod();
	
	/**
	 * Service Method to Invoke
	 * 
	 * @param method
	 */
	public void setMethod(Method method);
	
	/**
	 * Input Class를 설정한다.
	 * 
	 * @param clazz
	 */
	public void setInputClass(Class<?> clazz);
	
	/**
	 * Input Class를 리턴한다.
	 * 
	 * @return
	 */
	public Class<?> getInputClass();
	
	/**
	 * Output Class를 설정한다.
	 * 
	 * @param clazz
	 */
	public void setOutputClass(Class<?> clazz);
	
	/**
	 * Output Class를 리턴한다.
	 * 
	 * @return
	 */
	public Class<?> getOutputClass();
	
	/**
	 * Input Type Definition List
	 * 
	 * @return
	 */
	public List<FieldDef> getInputTypeList();
	
	/**
	 * Input Type Definition List
	 * 
	 * @param inputTypeList
	 */
	public void setInputTypeList(List<FieldDef> inputTypeList);
	
	/**
	 * Output Type Definition List
	 * 
	 * @return
	 */
	public List<FieldDef> getOutputTypeList();
	
	/**
	 * Output Type Definition List
	 * 
	 * @param outputTypeList
	 */
	public void setOutputTypeList(List<FieldDef> outputTypeList);
	
}
