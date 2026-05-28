/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.api;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Service Rule : 서비스 URL과 서비스 클래스 및 메소드와 바인딩되는 룰을 정의한다. 
 * 
 * @author shortstop
 */
public interface IServiceRule {
	
	/**
	 * 서비스 URL 룰을 리턴한다. - ex) service/{service_class_name}/{method_name} 
	 * 
	 * @return
	 */
	public String getServiceUrlRule();
	
	/**
	 * fullUrl이 유효한 지 체크 
	 * 
	 * @param fullUrl
	 * @return
	 */
	public boolean isValidUrl(String fullUrl);
	
	/**
	 * 호출된 fullUrl로 부터 서비스 URL을 추출한다. 즉 fullUrl에서 url 고정 상수(ex : service)와 method 정보를 빼고 리턴한다. 
	 * 
	 * @param fullUrl
	 * @return
	 */
	public String extractServiceUrl(String fullUrl);
	
	/**
	 * 요청 fullUrl로 부터 메소드 명을 추출한다.
	 * 
	 * @param fullUrl
	 * @return
	 */
	//public String extractMethodName(String fullUrl);

	/**
	 * fullUrl로 부터 서비스 클래스를 찾는다. 
	 * 
	 * @param fullUrl
	 * @return
	 */
	public Class<?> findClassByUrl(String fullUrl);
	
	/**
	 * 서비스 클래스 이름으로 부터 url을 찾는다.
	 * 
	 * @param className
	 * @return
	 */
	public String findUrlByClassName(String className);
	
	/**
	 * 서비스 클래스로 부터 url을 찾는다.
	 * 
	 * @param clazz
	 * @return
	 */
	public String findUrlByClass(Class<?> clazz);
	
	/**
	 * 서비스 클래스, apiUrl로 web socket url을 찾는다.
	 * 
	 * @param clazz
	 * @param apiUrl
	 * @return
	 */
	public String findWebSocketUrl(Class<?> clazz, String apiUrl);
	
	/**
	 * 메소드로 API의 URL을 찾는다.
	 * 
	 * @param serviceClass
	 * @param method
	 * @return
	 */
	public String findApiUrlByMethod(Class<?> serviceClass, Method method);
		
	/**
	 * 서비스 클래스 명, 메소드 명으로 API의 URL을 찾는다.
	 * 
	 * @param className
	 * @param methodName
	 * @return
	 */
	public String findApiUrlByMethodName(String className, String methodName);	
		
	/**
	 * fullUrl로 메소드를 찾는다.
	 * 
	 * @param fullUrl
	 * @return
	 */
	public Method findMethodByUrl(String fullUrl);
	
	/**
	 * fullUrl, httpMethod로 메소드를 찾는다.
	 * 
	 * @param fullUrl
	 * @param httpMethod
	 * @return
	 */
	public Method findMethodByUrl(String fullUrl, String httpMethod);	
	
	/**
	 * 서비스 클래스의 메소드가 methodName인 메소드를 찾아 리턴 
	 * 
	 * @param clazz
	 * @param methodName
	 * @return
	 */
	public Method findServiceMethod(Class<?> serviceClass, String methodName);
	
	/**
	 * 서비스 클래스의 서비스 메소드를 모두 찾아 리턴 
	 * 
	 * @param serviceClass
	 * @return
	 */
	public List<Method> findServiceMethods(Class<?> serviceClass);
	
}
