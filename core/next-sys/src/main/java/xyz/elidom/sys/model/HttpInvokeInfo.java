package xyz.elidom.sys.model;

import java.util.Map;

public class HttpInvokeInfo {

	private String requestUri;
	private String httpMethod;
	private Map<String, String> options;
	private Map<String, String> pathVariables;
	private Object requestBody;
	private String result;

	public String getRequestUri() {
		return requestUri;
	}

	public void setRequestUri(String requestUri) {
		this.requestUri = requestUri;
	}

	public String getHttpMethod() {
		return httpMethod;
	}

	public void setHttpMethod(String httpMethod) {
		this.httpMethod = httpMethod;
	}

	public Map<String, String> getOptions() {
		return options;
	}

	public void setOptions(Map<String, String> options) {
		this.options = options;
	}

	public Map<String, String> getPathVariables() {
		return pathVariables;
	}

	public void setPathVariables(Map<String, String> pathVariables) {
		this.pathVariables = pathVariables;
	}

	public Object getRequestBody() {
		return requestBody;
	}

	public void setRequestBody(Object requestBody) {
		this.requestBody = requestBody;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}
}