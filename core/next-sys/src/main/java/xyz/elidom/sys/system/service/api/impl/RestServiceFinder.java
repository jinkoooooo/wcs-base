/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.api.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ClassUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.sf.common.util.Closure;
import net.sf.common.util.SyncCtrlUtils;
import net.sf.common.util.ValueUtils;
import xyz.elidom.exception.client.ElidomServiceNotFoundException;
import xyz.elidom.exception.server.ElidomServiceException;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sys.system.service.api.IServiceCache;
import xyz.elidom.sys.system.service.api.IServiceFinder;
import xyz.elidom.sys.system.service.api.IServiceRule;
import xyz.elidom.sys.system.service.def.FieldDef;
import xyz.elidom.sys.system.service.def.IServiceApi;
import xyz.elidom.sys.system.service.def.IServiceDef;
import xyz.elidom.sys.system.service.def.ServiceDefFactory;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ValueUtil;

/**
 * 기본 서비스 룰 - 서비스 URL : rest/{class_name}/{method_name}
 * 
 * @author shortstop
 */
@Component
@Qualifier("rest")
public class RestServiceFinder implements IServiceFinder {
	
	@Autowired
	@Qualifier("rest")
	private IServiceCache restSvcCache;
	
	@Autowired
	@Qualifier("rest")
	private IServiceRule restSvcRule;
	
	/**
	 * serviceId와 methodName으로 API ID를 생성 
	 * 
	 * @param serviceId
	 * @param methodName
	 * @return
	 */
	private String createApiId(String serviceId, String methodName) {
		return serviceId + "_" + methodName;
	}
	
	/**
	 * apiId를 ServiceId와 Method 명으로 분리  
	 * 
	 * @param apiId
	 * @return
	 */
	private String[] divideApiId(String apiId) {
		return apiId.split("_");
	}	
	
	@Override
	public IServiceApi getServiceApi(String apiId) {
		String[] apiIdArr = this.divideApiId(apiId);
		String serviceId = apiIdArr[0];
		String methodName = apiIdArr[1];
		IServiceDef serviceDef = this.getServiceDef(serviceId);
		IServiceApi api = serviceDef.getApiById(apiId);
		
		if(api == null) {
			Method method = this.restSvcRule.findServiceMethod(serviceDef.getClass(), methodName);
			api = this.scanServiceApi(serviceDef, method);
		}		
		
		return api;		
	}
	
	/**
	 * 서비스를 Cache에서 찾고 없으면 스캔 - 스캔시 서비스(클래스) 레벨만 하고 API(메소드) 레벨은 스캔하지 않음 
	 *   
	 * @param serviceId
	 * @return
	 */
	protected IServiceDef getServiceDef(String serviceId) {
		IServiceDef serviceDef = this.restSvcCache.getService(serviceId);
		
		if(serviceDef == null) {
			serviceDef = this.scanService(serviceId);
		}
		
		return serviceDef;
	}
	
	@Override
	public IServiceDef getServiceDetail(String serviceId) {
		IServiceDef serviceDef = this.getServiceDef(serviceId);
		if(!serviceDef.isApiScannedAll()) {
			this.scanServiceApiAll(serviceDef);
		}
		
		return serviceDef;
	}	
		
	@Override
	public IServiceApi getServiceApiDetail(String apiId) {
		String[] apiIdArr = this.divideApiId(apiId); 
		String serviceId = apiIdArr[0];
		String methodName = apiIdArr[1];
		IServiceDef serviceDef = this.getServiceDef(serviceId);
		IServiceApi api = serviceDef.getApiById(apiId);
		
		if(api == null) {
			api = this.scanServiceApi(serviceDef, methodName);
		}
		
		// Synchroinzed
		if(ValueUtil.isEmpty(api.getInputTypeList())) {
			final IServiceApi svcApi = api;
			try {
				SyncCtrlUtils.wrap("API-INOUT", new Closure<Object, Exception>() {
					@Override
					public Object execute() throws Exception {
						scanServiceInputTypeList(svcApi);
						scanServiceOutputTypeList(svcApi);
						return null;
					}
				});
			} catch (Exception e) {
				throw new ElidomServiceException(e.getMessage(), e);
			}
		}
		
		return api;
	}	
	
	@Override
	public List<IServiceDef> getServicesByModule(String module) {
		return this.restSvcCache.getServiceList(module);
	}
	
	@Override
	public void scanServicesByPackage(String module, String basePackage) {
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
		basePackage += ".rest";
		
		// scan from base package
		for (BeanDefinition bd : scanner.findCandidateComponents(basePackage)) {
		    String beanName = bd.getBeanClassName();
			if(beanName.startsWith(basePackage) && beanName.contains(".rest.") && beanName.endsWith("Controller")) {
				this.scanService(module, beanName);
			} 
		}
	}
	
	/**
	 * serviceId (즉 서비스 클래스 명)으로 Service Scan
	 * 
	 * @param serviceId
	 * @return
	 */
	protected IServiceDef scanService(String serviceId) {
		String[] classNameArr = serviceId.split("\\.");
		String module = classNameArr[2];
		return this.scanService(module, serviceId);
	}
	
	/**
	 * serviceId (즉 서비스 클래스 명)으로 Service Scan
	 * 
	 * @param module
	 * @param serviceId
	 * @return
	 */
	protected IServiceDef scanService(final String module, final String serviceId) {
		try {
			return SyncCtrlUtils.wrap(this.getClass().getName(), new Closure<IServiceDef, Exception>() {
				@Override
				public IServiceDef execute() throws Exception {
					IServiceDef serviceDef = restSvcCache.getService(serviceId);
					if(serviceDef != null) {
						return serviceDef;
					}
					
					Class<?> serviceClass = ClassUtils.getClass(serviceId);
					Object bean = BeanUtil.get(serviceClass);
					serviceDef = ServiceDefFactory.newServiceDef();
					serviceDef.setModule(module);
					serviceDef.setId(serviceId);
					serviceDef.setBean(bean);
					serviceDef.setBeanClass(serviceClass);
					serviceDef.setBeanClassName(serviceId);			
					
					// 서비스 클래스의 RequestMapping Annotation으로 부터 메타 정보를 추출한다.
					Annotation reqMapAnn = AnnotationUtils.findAnnotation(serviceClass, RequestMapping.class);
					if(reqMapAnn != null) {
						Map<String, Object> reqMapAnnAttrs = AnnotationUtils.getAnnotationAttributes(reqMapAnn);
						String[] restUrl = (String[])reqMapAnnAttrs.get("value");
						if(restUrl != null && restUrl.length >= 1) {
							serviceDef.setName(restUrl[0]);
						}
					} 
					
					// 서비스 클래스의 ServiceDesc Annotation으로 부터 메타 정보를 추출한다. 
					Annotation svcDescAnn = AnnotationUtils.findAnnotation(serviceClass, ServiceDesc.class);
					if(svcDescAnn != null) {
						Map<String, Object> svcDescAnnAttrs = AnnotationUtils.getAnnotationAttributes(svcDescAnn);
						serviceDef.setDescription(svcDescAnnAttrs.containsKey("description") ? (String)svcDescAnnAttrs.get("description") : null);
					}
					
					List<Method> methodList = restSvcRule.findServiceMethods(serviceClass);
					serviceDef.setApiCount(ValueUtil.isEmpty(methodList) ? 0 : methodList.size());
					restSvcCache.setService(serviceDef);
					return serviceDef;
				}
			});
		} catch(Exception e) {
			throw new ElidomServiceNotFoundException(e.getMessage(), e);
		}
	}
	
	/**
	 * Service API List를 스캔하여 리턴 
	 * 
	 * @param service
	 * @return
	 */
	protected void scanServiceApiAll(IServiceDef serviceDef) {
		Class<?> svcCls = serviceDef.getBeanClass();
		List<Method> methodList = this.restSvcRule.findServiceMethods(svcCls);
		
		for(Method method : methodList) {
			if(serviceDef.getApiByMethod(method.getName()) == null) {
				this.scanServiceApi(serviceDef, method);
			}
		}
	}

	/**
	 * serviceDef, method로 ServiceApi 스캔 
	 * 
	 * @param serviceDef
	 * @param methodName
	 * @return
	 */
	protected IServiceApi scanServiceApi(IServiceDef serviceDef, String methodName) {
		Method method = this.restSvcRule.findServiceMethod(serviceDef.getBeanClass(), methodName);
		return this.scanServiceApi(serviceDef, method);
	}
	
	/**
	 * serviceDef, method로 ServiceApi 스캔 
	 * 
	 * @param serviceDef
	 * @param method
	 * @return
	 */
	protected IServiceApi scanServiceApi(final IServiceDef serviceDef, final Method method) {
		try {
			// Service Method로 부터 ApiDescriptor Annotation 및 attributes 찾기
			Annotation apiAnn = AnnotationUtils.findAnnotation(method, RequestMapping.class);
			final Map<String, Object> apiAnnAttrs = AnnotationUtils.getAnnotationAttributes(apiAnn);
			final String apiId = this.createApiId(serviceDef.getId(), method.getName());

			// Synchronized
			return SyncCtrlUtils.wrap("RESTAPI-INOUT", new Closure<IServiceApi, Exception>() {
				@Override
				public IServiceApi execute() throws Exception {
					IServiceApi api = serviceDef.getApiById(apiId);
					if(api != null) {
						return api;
					}
					
					RequestMethod[] httpMethodArr = (RequestMethod[])apiAnnAttrs.get("method");
					String httpMethod = ValueUtil.isEmpty(httpMethodArr) ? null : httpMethodArr[0].toString();
					String url = null;
					
					if(apiAnnAttrs.containsKey("value")) {
						String[] urlArr = (String[])apiAnnAttrs.get("value");
						if(ValueUtil.isEmpty(urlArr)) {
							url = serviceDef.getName();
						} else {
							url = ValueUtil.isEmpty(urlArr) ? null : urlArr[0];
						}
					}
					
					if(ValueUtil.isEmpty(url)) {
						url = serviceDef.getName();
					}
					
					if(!ValueUtil.isEqual(url, serviceDef.getName())) {
						url = serviceDef.getName() + url;
					}
					
					api = ServiceDefFactory.newServiceApi();
					// 그룹 아이디는 서비스 빈 이름으로 한다. 
					api.setServiceDef(serviceDef);
					api.setId(apiId);
					api.setName(method.getName());
					api.setUrl(url);
					api.setWsUrl(url);
					api.setHttpMethod(httpMethod);
					api.setMethod(method);
					api.setOutputClass(method.getReturnType());
					
					
					if(method.getParameterTypes().length == 1) {
						api.setInputClass(method.getParameterTypes()[0]);
					} else if(method.getParameterTypes().length == 2) {
						api.setInputClass(method.getParameterTypes()[1]);
					}
					
					// Service Method로 부터 ApiDescriptor Annotation 및 attributes 찾기
					Annotation apiAnn = AnnotationUtils.findAnnotation(method, ApiDesc.class);
					
					if(apiAnn != null) {
						Map<String, Object> apiAnnAttrs = AnnotationUtils.getAnnotationAttributes(apiAnn);
						// 서비스 설명을 annotation으로 부터 추출한다.
						api.setDescription(apiAnnAttrs.containsKey("description") ? (String)apiAnnAttrs.get("description") : api.getId());
					}					

					serviceDef.addApi(api);
					return api;
				}
			});
		} catch (Exception e) {
			throw new ElidomServiceException(e.getMessage(), e);
		}
	}
	
	/**
	 * 서비스 InputTypeList를 스캔하여 리턴 
	 * 
	 * @param api
	 * @return
	 */
	protected List<FieldDef> scanServiceInputTypeList(IServiceApi api) {
		List<FieldDef> inputList = api.getInputTypeList();
		if (!ValueUtil.isEmpty(inputList)) {
			return inputList;
		}

		inputList = new ArrayList<FieldDef>();
		Method method = api.getMethod();
		Parameter[] parameters = method.getParameters();
		
		for (Parameter parameter : parameters) {
			Class<?> inputType = parameter.getType();
			String parameterName = parameter.getName();
			
			if (inputType.isPrimitive() || inputType.getName().startsWith("java.lang.")) {
				FieldDef apiInput = new FieldDef();

				RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
				if (requestParam != null) {
					parameterName = ValueUtil.checkValue(requestParam.name(), parameterName);
				}

				apiInput.setName(parameterName);
				apiInput.setType(inputType.getName());
				inputList.add(apiInput);
				
			} else {
				FieldDef apiInput = new FieldDef();
				apiInput.setName(parameterName);
				apiInput.setType(inputType.getName());
				inputList.add(apiInput);
			}
		}
		
		if(!ValueUtils.isEmpty(inputList)) {
			api.setInputTypeList(inputList);
		}
		
		return inputList;
	}
	
	/**
	 * 서비스 OutputTypeList를 스캔하여 리턴
	 * 
	 * @param api
	 * @return
	 */
	protected List<FieldDef> scanServiceOutputTypeList(IServiceApi api) {
		List<FieldDef> outputList = api.getOutputTypeList();
		if(!ValueUtil.isEmpty(outputList)) {
			return outputList;
		}
		
		outputList = new ArrayList<FieldDef>();
		Method method = api.getMethod();
		
		if(method.getReturnType() != null) {
			Class<?> outputType = method.getReturnType();
			
			FieldDef apiOutput = new FieldDef();
			apiOutput.setName("Return Type");
			apiOutput.setType(outputType.getName());
			if(ValueUtil.isEqual(outputType.getName(), "void")) {
				apiOutput.setName("void");
			}
			outputList.add(apiOutput);
			
			/*// 1. Primitive Type이면 
			if(outputType.isPrimitive()) {
				FieldDef apiOutput = new FieldDef();
				apiOutput.setName("Return");
				apiOutput.setType(outputType.getName());
				outputList.add(apiOutput);
			
			// 2. Object Type이면 
			} else {
				Field[] fields = null;
				
				// IPageOutput 하위 클래스 라면 
				if(IPageOutput.class.isAssignableFrom(outputType)) {
					fields = this.mergeFields(PageOutput.class, ListOutput.class, BasicOutput.class);
					
				// IListOutput 하위 클래스 라면 
				} else if(IListOutput.class.isAssignableFrom(outputType)) {
					fields = this.mergeFields(ListOutput.class, BasicOutput.class);
					
				// IBasicOutput 하위 클래스 라면 
				} else if(IBasicOutput.class.isAssignableFrom(outputType)) {
					outputType = BasicOutput.class;
					fields = outputType.getDeclaredFields();
					
				// 기타 
				} else {
					fields = outputType.getDeclaredFields();
				}
				
				for(int i = 0 ; i < fields.length ; i++) {
					FieldDef apiOutput = new FieldDef();
					Field field = fields[i];
					apiOutput.setName(field.getName());
					apiOutput.setType(field.getType().getName());
					outputList.add(apiOutput);
				}				
			}*/
		}
		
		if(!ValueUtil.isEmpty(outputList)) {
			api.setOutputTypeList(outputList);
		}
		
		return outputList;
	}
	
	/**
	 * clazzes의 declared fields를 모두 찾아 리턴 
	 * 
	 * @param clazzes
	 * @return
	 */
	/*private Field[] mergeFields(Class<?>... clazzes) {
		int totalFieldCount = 0;
		List<Field[]> totalFieldList = new ArrayList<Field[]>();
		
		for(int i = 0 ; i < clazzes.length ; i++) {
			Field[] fields = clazzes[i].getDeclaredFields();
			totalFieldList.add(fields);
			totalFieldCount += fields.length;
		}
		
		Field[] results = new Field[totalFieldCount];
		int idx = 0;
		
		for(Field[] fields : totalFieldList) {
			for(int i = 0 ; i < fields.length ; i++) {
				results[idx] = fields[i];
				idx += 1;
			}
		}
		
		return results;
	}*/
}
