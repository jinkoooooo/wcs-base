/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.api.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ClassUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import xyz.elidom.exception.client.ElidomServiceNotFoundException;
import xyz.elidom.sys.SysMessageConstants;
import xyz.elidom.sys.config.ModuleConfigSet;
import xyz.elidom.sys.system.service.api.IServiceCache;
import xyz.elidom.sys.system.service.api.IServiceRule;
import xyz.elidom.sys.system.service.def.IServiceDef;
import xyz.elidom.util.ClassUtil;
import xyz.elidom.util.ValueUtil;

@Component
@Qualifier("rest")
public class RestServiceRule implements IServiceRule {

	@Autowired
	@Qualifier("rest")
	private IServiceCache restSvcCache;
	
	@Autowired
	private ModuleConfigSet moduleConfigSet;
	
	@Override
	public String getServiceUrlRule() {
		return "rest/{resource_name}/{optional_id}";
	}

	@Override
	public boolean isValidUrl(String fullUrl) {
		if(!fullUrl.startsWith("rest/")) {
			return false;
		}
		
		String[] fullUrlArr = fullUrl.split("/");
		
		if(!(fullUrlArr.length >= 2 && fullUrlArr.length <= 3)) {
			throw new ElidomServiceNotFoundException(SysMessageConstants.NOT_FOUND_URL, "Service Not Found By URL({0}).", ValueUtil.toList(fullUrl));
		}
		
		if(fullUrl.indexOf("/") != 4) {
			return false;
		}
		
		return true;
	}
	
	@Override
	public String extractServiceUrl(String fullUrl) {
		// fullUrl에서 '/'로 구분해서 첫번째(rest) 다음 구분자가 아이디   
		String[] fullUrlArr = fullUrl.split("/");
		int fullUrlLength = fullUrlArr.length;

		if(fullUrlLength >= 2 && fullUrlLength <= 3) {
			return "/" + fullUrlArr[1];			
		} else {
			throw new ElidomServiceNotFoundException(SysMessageConstants.INVALID_SERVICE_URL, "Invalid Restful Service URL({0}).", ValueUtil.toList(fullUrl));
		}
	}
	
	/*@Override
	public String extractMethodName(String fullUrl) {
		return fullUrl.substring(fullUrl.lastIndexOf("/") + 1);
	}*/	
	
	@Override
	public Class<?> findClassByUrl(String fullUrl) {
		String serviceId = this.extractServiceUrl(fullUrl);
		IServiceDef serviceDef = this.restSvcCache.getService(serviceId);
		
		if(serviceDef != null) {
			return serviceDef.getBeanClass();
			
		} else {
			String className = this.urlToClassName(serviceId);
			// 각 모듈 서비스 베이스 패키지를 얻어서 모듈 베이스 패키지 + class_name으로 Class 찾기 시도
			Class<?> serviceClass = null;
			List<String> svcPkgList = this.moduleConfigSet.getScanServicePackages();
			
			for (String path : svcPkgList) {
				try {
					String restPath = path.replace(".service", ".rest");
					serviceClass = ClassUtils.getClass(restPath + "." + className);
					break;
				} catch (Exception e) {
					continue;
				}
			}
			
			if(serviceClass == null) {
				throw new ElidomServiceNotFoundException(SysMessageConstants.NOT_FOUND_URL, "Service Not Found By URL({0}).", ValueUtil.toList(fullUrl));
			}
			
			return serviceClass;
		}
	}

	@Override
	public String findUrlByClassName(String className) {
		// 서비스 클래스에서 service 패키지 하위의 서브 패키지를 찾아서 URL로 리턴 
		// (ex: xyz.elidom.base.rest.sample.SampleService ==> service/sample)
		String[] classNameArr = className.split("\\.");
		boolean start = false;
		int pkgCount = classNameArr.length;
		StringBuffer url = new StringBuffer();
		
		for(int i = 0 ; i < pkgCount - 1 ; i++) {
			if(start == false && classNameArr[i].equals("rest")) {
				start = true;
			}
			
			if(start) {
				if(url.length() > 0) {
					url.append("/");
				}
				
				url.append(StringUtils.replace(classNameArr[i], "_", "-"));
			}
		}
		
		return url.toString();
	}

	@Override
	public String findUrlByClass(Class<?> clazz) {
		return this.findUrlByClassName(clazz.getName());
	}

	@Override
	public String findApiUrlByMethod(Class<?> serviceClass, Method method) {
		String serviceUrl = this.findUrlByClass(serviceClass);
		boolean isMemberMethod = false;
		
		Annotation apiAnn = AnnotationUtils.findAnnotation(method, RequestMapping.class);		
		if(apiAnn != null) {
			Map<String, Object> apiAnnAttrs = AnnotationUtils.getAnnotationAttributes(apiAnn);
			// 서비스 명을 annotation으로 부터 추출한다.
			String urlMapping = (String)apiAnnAttrs.get("value");
			isMemberMethod = urlMapping.endsWith("{id}");
		}

		return serviceUrl + (isMemberMethod ? "" : "/{id}");
	}
	
	@Override
	public String findApiUrlByMethodName(String className, String methodName) {
		Class<?> serviceClass = ClassUtil.forName(className);
		Method method = this.findServiceMethod(serviceClass, methodName);
		return this.findApiUrlByMethod(serviceClass, method);
	}
	
	@Override
	public String findWebSocketUrl(Class<?> clazz, String apiUrl) {
		return apiUrl;
	}

	@Override
	public Method findMethodByUrl(String fullUrl) {
		throw new ElidomServiceNotFoundException(SysMessageConstants.NOT_SUPPORTED_METHOD, "Not supported method");
	}
	
	@Override
	public Method findMethodByUrl(String fullUrl, String httpMethod) {
		Class<?> serviceClass = this.findClassByUrl(fullUrl);
		List<Method> methodList = this.findServiceMethods(serviceClass);
		
		for(Method method : methodList) {
			Annotation apiAnn = AnnotationUtils.findAnnotation(method, RequestMapping.class);
			if(apiAnn != null) {
				// 서비스 명을 annotation으로 부터 추출한다.
				Map<String, Object> apiAnnAttrs = AnnotationUtils.getAnnotationAttributes(apiAnn);
				String mHttpMethod = (String)apiAnnAttrs.get("method");
				String mUrlMapping = (String)apiAnnAttrs.get("value");
				
				if(mUrlMapping.equals(fullUrl) && httpMethod.equalsIgnoreCase(mHttpMethod)) {
					return method;
				}
			}
		}
		
		return null;
	}

	/**
	 * 서비스 URL (ex : rest/sample)을 package path(ex : rest.SampleService)로 변환한다. 
	 * 
	 * @param serviceUrl
	 * @return
	 */
	protected String urlToClassName(String serviceUrl) {
		Class<?> serviceClass = null;
		String className = serviceUrl.replaceAll("/", ".");
		List<String> svcPkgList = this.moduleConfigSet.getScanServicePackages();
		
		for (String path : svcPkgList) {
			try {
				String restPath = path.replace(".service", ".rest");
				serviceClass = ClassUtils.getClass(restPath + className);
				break;
			} catch (Exception e) {
				continue;
			}
		}
		
		if(serviceClass == null) {
			throw new ElidomServiceNotFoundException(SysMessageConstants.INVALID_SERVICE_URL, "Invalid Restful Service URL({0}).", ValueUtil.toList(serviceUrl));
		}
		
		return className.toString();
	}
	
	@Override
	public Method findServiceMethod(Class<?> clazz, String methodName) {
		Method[] methods = clazz.getMethods();
		for (Method m : methods) {
			if (!Modifier.isStatic(m.getModifiers()) && m.getName().equalsIgnoreCase(methodName)) {
				return m;
			}
		}
		
		return null;
	}

	@Override
	public List<Method> findServiceMethods(Class<?> clazz) {
		List<Method> list = new ArrayList<Method>();
		Method[] methods = clazz.getMethods();
		
		for(int i = 0 ; i < methods.length ; i++) {
			Method method = methods[i];
			if(method.getModifiers() == Modifier.PUBLIC && method.getModifiers() != Modifier.STATIC && method.isAnnotationPresent(RequestMapping.class)) {
				list.add(method);
			}
		}
		
		return list;
	}
	
}
