package xyz.elidom.sys.system.aop;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import xyz.elidom.sys.SysConfigConstants;
import xyz.elidom.sys.model.ServiceLogInfo;
import xyz.elidom.sys.web.HttpRequestWrapper;
import xyz.elidom.util.ValueUtil;

@Service
public class RestServiceLogger {

	@Autowired
	Environment env;

	@Autowired
	private RequestMappingHandlerMapping requestMappingHandlerMapping;

	// Service 실행 로그 기능 활성화 여부.
	Boolean isEnabled;

	// 실행 결과값에 대한 출력 여부 설정
	Boolean withResult;

	// Service Log를 생성 할 URL List
	List<String> availableUrlList;

	// Service Log 제외 대상 URL List
	List<String> exceptUrlList;

	/**
	 * Logger
	 */
	private Logger logger = LoggerFactory.getLogger(RestServiceLogger.class);

	public ServiceLogInfo getServiceLogInfo(ProceedingJoinPoint joinPoint) {
		try {
			// Service 실행 로그 기능 활성화되어 있지 않으면, 이력을 생성하지 않음.
			boolean isEnabled = this.isServiceLogEnabled();
			if (!isEnabled)
				return null;

			// Request 정보가 존재하지 않을 경우, 이력을 생성하지 않음.
			HttpServletRequest request = this.getHttpRequest();
			if (request == null)
				return null;

			// Service Log를 생성 할 URL에 포함 되어 있지 않는다면, 이력을 생성하지 않음. 단, 값이 존재하지 않을 경우는 모든 이력 생성.
			boolean isAvailable = this.isAvailableUrl(request.getRequestURI());
			if (!isAvailable)
				return null;

			// 서비스 정보 추출.
			ServiceLogInfo serviceLogInfo = this.parseServiceLogInfo(request);

			// 실행 클래스 정보가 동일하지 않을 경우, 이력을 생성하지 않음.
			String requestClassName = serviceLogInfo.getClassName();
			String invokeClassName = joinPoint.getTarget().getClass().getName();
			if (ValueUtil.isNotEqual(requestClassName, invokeClassName))
				return null;

			// 실행 메소드 정보가 동일하지 않을 경우, 이력을 생성하지 않음.
			String requestMethodName = serviceLogInfo.getMethodName();
			String invokeMethodName = joinPoint.getSignature().getName();
			if (ValueUtil.isNotEqual(requestMethodName, invokeMethodName))
				return null;

			return serviceLogInfo;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Print Request Info.
	 * 
	 * @param request
	 */
	public void doPrint(ServiceLogInfo serviceLogInfo) {
		if (ValueUtil.isEmpty(serviceLogInfo))
			return;

		String line = this.getLine("=");
		StringJoiner requestInfos = new StringJoiner("\n");

		String invokeApi = null;
		String className = serviceLogInfo.getClassName();
		String methodName = serviceLogInfo.getMethodName();

		try {
//			ClassPool classPool = ClassPool.getDefault();
//			CtClass ctClass = classPool.get(className);
//
//			CtMethod methodX = ctClass.getDeclaredMethod(methodName);
//			int xlineNumber = methodX.getMethodInfo().getLineNumber(0);
//
//			invokeApi = className + "." + methodName + "(" + ctClass.getSimpleName() + ".java" + ":" + xlineNumber + ")";
		    
			
			// 1. 클래스 이름으로 Class<?> 객체 가져오기
		    Class<?> clazz = Class.forName(className);

		    // 2. 지정된 메서드 찾기
		    // - 메서드 파라미터가 없는 경우
		    Method methodX = clazz.getDeclaredMethod(methodName);
		    methodX.setAccessible(true); // private 메서드일 경우 접근 가능하도록 설정

		    // 3. 메서드의 첫 번째 라인 번호는 Reflection으로 직접 접근 불가
		    // 이를 위해 디버그 정보를 포함한 bytecode 분석 라이브러리가 필요
		    // 예: StackTrace로 추정하거나 ASM 등을 이용할 수 있음

		    // 4. StackTrace를 통해 라인 번호 추정 (디버그 정보가 포함된 클래스 파일이어야 가능)
		    int xlineNumber = -1;
		    for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
		        if (element.getClassName().equals(className) && element.getMethodName().equals(methodName)) {
		            xlineNumber = element.getLineNumber();
		            break;
		        }
		    }

		    // 5. invokeApi 생성
		    invokeApi = className + "." + methodName + "(" + clazz.getSimpleName() + ".java" + ":" + (xlineNumber != -1 ? xlineNumber : "Unknown") + ")";
		    System.out.println("Invoke API: " + invokeApi);
		} catch (Exception e) {
			invokeApi = className + "." + methodName + "()";
		}

		/*
		 * Request Info.
		 */
		requestInfos.add("");
		requestInfos.add(line);
		requestInfos.add("# Request Info").add(line);
		requestInfos.add(" - User        : " + serviceLogInfo.getUserId());
		requestInfos.add(" - IP Address  : " + serviceLogInfo.getIpAddress());
		requestInfos.add(" - URL Pattern : " + serviceLogInfo.getRequestUrlPattern());
		requestInfos.add(" - Request URI : " + serviceLogInfo.getRequestUri());
		requestInfos.add(" - Http Method : " + serviceLogInfo.getHttpMethod());
		requestInfos.add(" - Invoke API  : " + invokeApi);
		requestInfos.add(line);

		/*
		 * Path Variables
		 */
		Map<String, String> pathVariableMap = serviceLogInfo.getPathVariables();
		if (pathVariableMap != null && !pathVariableMap.isEmpty()) {
			// Handler Info.
			requestInfos.add("# Path Variables").add(line);
			pathVariableMap.forEach((k, v) -> requestInfos.add(" - " + k + " : " + v));
			requestInfos.add(line);
		}

		/*
		 * Request Params
		 */
		Map<String, String> requestParamMap = serviceLogInfo.getRequestParams();
		if (requestParamMap != null && !requestParamMap.isEmpty()) {
			// Handler Info.
			requestInfos.add("# Request Params").add(line);
			requestParamMap.forEach((k, v) -> requestInfos.add(" - " + k + " : " + v));
			requestInfos.add(line);
		}

		/*
		 * Request Body
		 */
		String requestBody = serviceLogInfo.getRequestBody();
		if (ValueUtil.isNotEmpty(requestBody)) {
			// Handler Info.
			requestInfos.add("# Request Body").add(line);
			requestInfos.add(requestBody);
			requestInfos.add(line);
		}

		/*
		 * Response
		 */
		if (this.withResultEnabled()) {
			String response = serviceLogInfo.getResultValue();
			if (ValueUtil.isNotEmpty(response)) {
				// Handler Info.
				requestInfos.add("# Response ").add(line);
				requestInfos.add(response);
				requestInfos.add(line);
			}
		}

		requestInfos.add("");

		logger.debug(requestInfos.toString());
	}

	/**
	 * HttpRequest 정보 가져오기 실행.
	 * 
	 * @return
	 */
	private HttpServletRequest getHttpRequest() {
		HttpServletRequest request = null;
		try {
			ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
			request = servletRequestAttributes.getRequest();

			String httpMethod = request.getMethod();
			if (httpMethod == null || httpMethod.equals(RequestMethod.OPTIONS.name()))
				return null;
		} catch (Exception e) {
		}
		return request;
	}

	/**
	 * Request를 통해 Pattern 분석 후, Service 정보 추출.
	 * 
	 * @param request
	 * @return
	 */
	private ServiceLogInfo parseServiceLogInfo(HttpServletRequest request) {
		RequestMappingInfo info = requestMappingInfoMap.get(request.getRequestURI());
		info = ValueUtil.checkValue(info, this.getRequestMappingInfo(request));
		if (ValueUtil.isEmpty(info)) {
			return null;
		}

		HandlerMethod handlerMethod = requestMappingHandlerMapping.getHandlerMethods().get(info);
		if (handlerMethod == null)
			return null;

		ServiceLogInfo serviceInfo = new ServiceLogInfo();
		StringJoiner requestUrlPattern = new StringJoiner(",");

		// Set URL Pattern
		Set<String> patterns = info.getPatternsCondition().getPatterns();
		for (String pattern : patterns) {
			PathMatcher pathMatcher = requestMappingHandlerMapping.getPathMatcher();
			Map<String, String> pathVariables = pathMatcher.extractUriTemplateVariables(pattern, request.getRequestURI());

			serviceInfo.setPathVariables(pathVariables);
			requestUrlPattern.add(pattern);
		}

		// Set ServiceInfo
		serviceInfo.setRequestUrlPattern(requestUrlPattern.toString());
		serviceInfo.setRequestUri(request.getRequestURI());
		serviceInfo.setClassName(handlerMethod.getBeanType().getName());
		serviceInfo.setMethodName(handlerMethod.getMethod().getName());
		serviceInfo.setHttpMethod(request.getMethod());
		serviceInfo.setUserId(ValueUtil.checkValue(request.getRemoteUser(), "anonymous"));
		serviceInfo.setIpAddress(request.getRemoteAddr());
		serviceInfo.setRequestParams(this.getRequestParams(request));

		// Set Reqeust Body
		if (request instanceof HttpRequestWrapper) {
			serviceInfo.setRequestBody(this.getRequestBody(request));
		}

		return serviceInfo;
	}

	private Map<String, RequestMappingInfo> requestMappingInfoMap = new HashMap<String, RequestMappingInfo>();

	/**
	 * Rquest에 해당하는 RequestMappingInfo 가져오기 실행.
	 * 
	 * @param request
	 * @return
	 */
	private RequestMappingInfo getRequestMappingInfo(HttpServletRequest request) {
		String uri = request.getRequestURI();

		RequestMappingInfo requestMappingInfo = requestMappingInfoMap.get(uri);
		if (ValueUtil.isNotEmpty(requestMappingInfo))
			return requestMappingInfo;

		Map<RequestMappingInfo, HandlerMethod> requestMappingInfoList = requestMappingHandlerMapping.getHandlerMethods();
		Set<RequestMappingInfo> mappingInfos = requestMappingInfoList.keySet();

		for (RequestMappingInfo info : mappingInfos) {
			RequestMappingInfo matchingCondition = info.getMatchingCondition(request);
			if (matchingCondition != null) {
				requestMappingInfo = info;
				requestMappingInfoMap.put(uri, info);
				break;
			}
		}

		return requestMappingInfo;
	}

	/**
	 * Service Log를 생성 할 URL 여부 확인.
	 * 
	 * @param requestUrl
	 * @return
	 */
	private boolean isAvailableUrl(String requestUrl) {
		/*
		 * Log 생성에서 제외 할 URL List
		 */
		List<String> exceptUrlList = this.getExceptUrlList();
		if (exceptUrlList.size() > 0) {
			for (String url : exceptUrlList) {
				if (url.startsWith(requestUrl))
					return false;
			}
		}

		/*
		 * Log를 생성 할 URL List
		 */
		List<String> availableUrlList = this.getAvailableUrlList();
		if (availableUrlList.isEmpty())
			return true;

		for (String url : availableUrlList) {
			if (url.startsWith(requestUrl))
				return true;
		}

		return false;
	}

	/**
	 * Service Log를 생성 할 URL 목록 가져오기 실행.
	 * 
	 * @return
	 */
	private List<String> getAvailableUrlList() {
		if (this.availableUrlList == null) {
			String logUrls = env.getProperty(SysConfigConstants.SERVICE_LOG_AVAILABLE_URLS);
			if (ValueUtil.isNotEmpty(logUrls)) {
				String[] urls = StringUtils.tokenizeToStringArray(logUrls, ",");
				availableUrlList = Arrays.asList(urls);
			} else {
				availableUrlList = new ArrayList<String>();
			}
		}
		return availableUrlList;
	}

	/**
	 * Service Log 시 제외 할 URL 목록 가져오기 실행.
	 * 
	 * @return
	 */
	private List<String> getExceptUrlList() {
		if (this.exceptUrlList == null) {
			String logUrls = env.getProperty(SysConfigConstants.SERVICE_LOG_EXCEPT_URLS, "/rest/users, /rest/request_auths");
			if (ValueUtil.isNotEmpty(logUrls)) {
				String[] urls = StringUtils.tokenizeToStringArray(logUrls, ",");
				exceptUrlList = Arrays.asList(urls);
			} else {
				exceptUrlList = new ArrayList<String>();
			}
		}
		return exceptUrlList;
	}

	/**
	 * Get Request Param Values
	 * 
	 * @param request
	 * @return
	 */
	private Map<String, String> getRequestParams(HttpServletRequest request) {
		Map<String, String[]> requestParams = request.getParameterMap();
		if (requestParams == null || requestParams.isEmpty()) {
			return null;
		}

		Map<String, String> requestParamMap = new HashMap<String, String>();

		Set<String> requestParamKeys = requestParams.keySet();
		for (String key : requestParamKeys) {
			StringBuilder appender = new StringBuilder();

			String[] values = requestParams.get(key);
			for (int i = 0; i < values.length; i++) {
				appender.append(i < 1 ? values[i] : " || " + values[i]);
			}
			requestParamMap.put(key, appender.toString());
		}

		return requestParamMap;
	}

	/**
	 * Get Request Body Values
	 * 
	 * @param request
	 * @return
	 */
	private String getRequestBody(HttpServletRequest request) {
		BufferedReader bufferedReader = null;

		try {
			ServletInputStream inputStream = request.getInputStream();
			if (inputStream == null || inputStream.isFinished())
				return null;

			StringBuilder stringBuilder = new StringBuilder();
			bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

			char[] charBuffer = new char[256];
			int bytesRead = -1;

			while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
				stringBuilder.append(charBuffer, 0, bytesRead);
			}

			String value = stringBuilder.toString();
			if (value.isEmpty()) {
				return null;
			}

			return stringBuilder.toString();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}

		return null;
	}

	/**
	 * 로그 출력시 표시될 구분 Line
	 * 
	 * @param character
	 * @return
	 */
	private String getLine(String character) {
		int size = 150;

		StringBuilder line = new StringBuilder();
		for (int i = 0; i < size; i++)
			line.append(character);

		return line.toString();
	}

	/**
	 * Service 실행 로그 기능 활성화 여부.
	 * 
	 * @return
	 */
	private boolean isServiceLogEnabled() {
		if (this.isEnabled == null) {
			this.isEnabled = ValueUtil.toBoolean(env.getProperty(SysConfigConstants.SERVICE_LOG_PRINT_ENABLE), false);
		}
		return this.isEnabled;
	}

	/**
	 * 실행 결과값에 대한 출력 여부 설정.
	 * 
	 * @return
	 */
	private boolean withResultEnabled() {
		if (this.withResult == null) {
			this.withResult = ValueUtil.toBoolean(env.getProperty(SysConfigConstants.SERVICE_LOG_PRINT_WITH_RESULT), true);
		}
		return this.withResult;
	}
}
