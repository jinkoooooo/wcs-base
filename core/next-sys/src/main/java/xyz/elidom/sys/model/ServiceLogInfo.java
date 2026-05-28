package xyz.elidom.sys.model;

import java.util.Map;

public class ServiceLogInfo {

	private String userId;
	private String ipAddress;
	private String requestUri;
	private String requestUrlPattern;
	private String className;
	private String methodName;
	private String httpMethod;
	private String resultValue;
	private Map<String, String> pathVariables;
	private Map<String, String> requestParams;
	private String requestBody;

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getRequestUrlPattern() {
		return requestUrlPattern;
	}

	public void setRequestUrlPattern(String requestUrlPattern) {
		this.requestUrlPattern = requestUrlPattern;
	}

	public String getRequestUri() {
		return requestUri;
	}

	public void setRequestUri(String requestUri) {
		this.requestUri = requestUri;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public String getHttpMethod() {
		return httpMethod;
	}

	public void setHttpMethod(String httpMethod) {
		this.httpMethod = httpMethod;
	}

	public String getResultValue() {
		return resultValue;
	}

	public void setResultValue(String resultValue) {
		this.resultValue = resultValue;
	}

	public Map<String, String> getPathVariables() {
		return pathVariables;
	}

	public void setPathVariables(Map<String, String> pathVariables) {
		this.pathVariables = pathVariables;
	}

	public Map<String, String> getRequestParams() {
		return requestParams;
	}

	public void setRequestParams(Map<String, String> requestParams) {
		this.requestParams = requestParams;
	}

	public String getRequestBody() {
		return requestBody;
	}

	public void setRequestBody(String requestBody) {
		this.requestBody = requestBody;
	}
}