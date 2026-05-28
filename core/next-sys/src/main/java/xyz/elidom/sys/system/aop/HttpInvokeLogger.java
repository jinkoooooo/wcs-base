package xyz.elidom.sys.system.aop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import xyz.elidom.sys.SysConfigConstants;
import xyz.elidom.sys.model.HttpInvokeInfo;
import xyz.elidom.sys.util.ExceptionUtil;
import xyz.elidom.util.FormatUtil;
import xyz.elidom.util.ValueUtil;

@Aspect
@Service
public class HttpInvokeLogger {

	@Autowired
	Environment env;

	// Service 실행 로그 기능 활성화 여부.
	Boolean isEnabled;

	// Send Log를 생성 할 URL List
	List<String> availableUrlList;

	// Send Log 제외 대상 URL List
	List<String> exceptUrlList;

	private Logger logger = LoggerFactory.getLogger(HttpInvokeLogger.class);

	@Around("execution(* xyz.elidom.sys.system.transport.sender.HttpSender.send*(..))")
	public Object filterAroundAccess(ProceedingJoinPoint joinPoint) throws Throwable {
		Object retVal = null;
		try {
			retVal = joinPoint.proceed();
		} catch (Exception e) {
			retVal = ExceptionUtil.getErrorStackTraceToString(e);
			throw e;
		} finally {
			this.doPrint(joinPoint.getSignature().getName(), joinPoint.getArgs(), retVal);
		}
		
		return retVal;
	}

	/**
	 * Print Http Send Info.
	 * 
	 * @param methodName
	 * @param args
	 * @param result
	 */
	private void doPrint(String methodName, Object[] args, Object result) {
		if (!this.isSendLogEnabled())
			return;

		HttpInvokeInfo httpInvokeInfo = this.getHttpSendInfo(methodName, args);
		if (httpInvokeInfo == null)
			return;

		// Service Log를 생성 할 URL에 포함 되어 있지 않는다면, 이력을 생성하지 않음. 단, 값이 존재하지 않을 경우는 모든 이력 생성.
		boolean isAvailable = this.isAvailableUrl(httpInvokeInfo.getRequestUri());
		if (!isAvailable)
			return;

		if (ValueUtil.isNotEmpty(result)) {
			httpInvokeInfo.setResult(result instanceof String ? ValueUtil.toString(result) : FormatUtil.toJsonString(result));
		}

		String line = this.getLine("=");
		StringJoiner requestInfos = new StringJoiner("\n");

		requestInfos.add("");
		requestInfos.add(line);
		requestInfos.add("# HTTP Invoke Info").add(line);
		requestInfos.add(" - Request URI : " + httpInvokeInfo.getRequestUri());
		requestInfos.add(" - Http Method : " + httpInvokeInfo.getHttpMethod());
		requestInfos.add(line);

		/*
		 * Option Variables
		 */
		Map<String, String> option = httpInvokeInfo.getOptions();
		if (option != null && !option.isEmpty()) {
			requestInfos.add("# Http Option").add(line);
			option.forEach((k, v) -> requestInfos.add(" - " + k + " : " + v));
			requestInfos.add(line);
		}

		/*
		 * Path Variables
		 */
		Map<String, String> pathVariables = httpInvokeInfo.getPathVariables();
		if (pathVariables != null) {
			requestInfos.add("# Path Variables").add(line);
			pathVariables.forEach((k, v) -> requestInfos.add(" - " + k + " : " + v));
			requestInfos.add(line);
		}

		/*
		 * Request Body
		 */
		Object requestBody = httpInvokeInfo.getRequestBody();
		if (requestBody != null) {
			String requestBodyValue = FormatUtil.toUnderScoreJsonString(requestBody);
			requestInfos.add("# Request Body").add(line);
			requestInfos.add(requestBodyValue);
			requestInfos.add(line);
		}

		/*
		 * Response
		 */
		String response = httpInvokeInfo.getResult();
		if (ValueUtil.isNotEmpty(response)) {
			// Handler Info.
			requestInfos.add("# Response ").add(line);
			requestInfos.add(response);
			requestInfos.add(line);
		}

		requestInfos.add("");

		logger.debug(requestInfos.toString());
	}

	/**
	 * Get Http Send Information.
	 * 
	 * @param methodName
	 * @param args
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private HttpInvokeInfo getHttpSendInfo(String methodName, Object[] args) {
		final String GET = "sendByGet";
		final String POST = "sendByPost";
		final String PUT = "sendByPut";
		final String DELETE = "sendByDelete";

		List<String> methodNames = Arrays.asList(GET, POST, PUT, DELETE);
		if (!methodNames.contains(methodName)) {
			return null;
		}

		Object[] arguments = new Object[4];
		System.arraycopy(args, 0, arguments, 0, args.length);

		HttpInvokeInfo httpInvokeInfo = new HttpInvokeInfo();
		httpInvokeInfo.setRequestUri(ValueUtil.toString(arguments[0]));

		if (ValueUtil.isEqual(methodName, GET)) {
			httpInvokeInfo.setHttpMethod("GET");

			if (arguments[1] != null)
				httpInvokeInfo.setPathVariables((Map<String, String>) arguments[1]);
			if (arguments[2] != null)
				httpInvokeInfo.setOptions((Map<String, String>) arguments[2]);
		} else {
			switch (methodName) {
				case POST :
					httpInvokeInfo.setHttpMethod("POST");
					break;
				case PUT :
					httpInvokeInfo.setHttpMethod("PUT");
					break;
				case DELETE :
					httpInvokeInfo.setHttpMethod("DELETE");
					break;
			}

			if (arguments[1] != null)
				httpInvokeInfo.setRequestBody(arguments[1]);
			if (arguments[2] != null)
				httpInvokeInfo.setOptions((Map<String, String>) arguments[2]);
			if (arguments[3] != null)
				httpInvokeInfo.setOptions((Map<String, String>) arguments[2]);
		}

		return httpInvokeInfo;
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
	 * Service 실행 로그 기능 활성화 여부.
	 * 
	 * @return
	 */
	private boolean isSendLogEnabled() {
		if (isEnabled == null) {
			isEnabled = ValueUtil.toBoolean(env.getProperty(SysConfigConstants.HTTP_SENDER_LOG_ENABLE), false);
		}
		return isEnabled;
	}

	/**
	 * Service Log를 생성 할 URL 목록 가져오기 실행.
	 * 
	 * @return
	 */
	private List<String> getAvailableUrlList() {
		if (this.availableUrlList == null) {
			String logUrls = env.getProperty(SysConfigConstants.HTTP_SENDER_LOG_AVAILABLE_URLS);
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
			String logUrls = env.getProperty(SysConfigConstants.HTTP_SENDER_LOG_EXCEPT_URLS);
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
}
