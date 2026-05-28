/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.transport.sender;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestTemplate;

import xyz.elidom.exception.client.ElidomBadRequestException;
import xyz.elidom.sys.SysMessageConstants;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.system.transport.ISender;
import xyz.elidom.sys.util.SysValueUtil;
import xyz.elidom.util.FormatUtil;

@Component
@Qualifier("http")
public class HttpSender implements ISender {

	/**
	 * HTTP METHOD KEY
	 */
	private final String HTTP_METHOD = "HTTP_METHOD";
	private final String AUTH_TYPE = "Authorization-Type";
	private final String AUTH_KEY = "Authorization-Key";
	private final String AUTH_TYPE_JSON = "json";
	private final String AUTH_TYPE_TOKEN = "token";

	/**
	 * Status - OK
	 */
	private final String OK = "OK";

	/**
	 * REST 기반 서비스 요청.
	 * 
	 * @param url 호출 URL.
	 * @param request RequestBody로 전송 할 데이터 객체.
	 * @param parameters URL 파라메터.
	 * @param options Request Header 설정 파라메터.
	 * @return 실행 결과를 Object 타입으로 반환.
	 */
	public Object send(Object url, Object request, Map<String, ?> parameters, Map<String, ?> options) {
		return this.send(null, null, url, request, parameters, options);
	}

	/**
	 * 전송 제목을 title로, 전송자 from이 대상 to로 파라미터 parameters와 옵션 options로 content 내용으로 호출
	 * 
	 * @param title 전송 타이틀
	 * @param from 전송하는 측의 정보 예) 메일 주소, IP등 ... (optional)
	 * @param to 받는 측의 정보 예) 메일 주소, IP, Mobile Token, JMS Queue, Topic명 등 ...
	 * @param content 전송할 내용
	 * @param paramters 전송 파라미터 Http Sender라면 파라미터
	 * @param options 전송하는 데 추가적으로 필요한 옵션 예) 메일이라면 MIME Type, attachment, HTTP Sender라면 Header 정보 등...
	 * @return
	 */
	@Override
	public Object send(String title, Object from, Object to, Object request, Map<String, ?> parameters, Map<String, ?> options) {
		String method = String.valueOf(options.get(HTTP_METHOD));
		String url = String.valueOf(to);

		if (SysValueUtil.isEmpty(method) || SysValueUtil.isEqual(method, RequestMethod.GET.name())) {
			return this.sendByGet(url, parameters, options);

		} else if (SysValueUtil.isEqual(method, RequestMethod.POST.name())) {
			return this.sendByPost(url, request, parameters, options);

		} else if (SysValueUtil.isEqual(method, RequestMethod.PUT.name())) {
			return this.sendByPut(url, request, parameters, options);

		} else if (SysValueUtil.isEqual(method, RequestMethod.DELETE.name())) {
			return this.sendByDelete(url, request, parameters, options);

		} else {
			throw new ElidomBadRequestException(SysMessageConstants.NOT_SUPPORTED_METHOD, SysMessageConstants.NOT_SUPPORTED_METHOD + " (" + method + ")");
		}
	}

	/**
	 * GET 방식으로 URL 호출하는데 파라미터 parameters로 호출
	 * 
	 * @param url
	 * @param parameters
	 * @return
	 */
	public Object sendByGet(String url) {
		return this.sendByGet(url, null, null);
	}
	
	public Object sendByGet(String url, Map<String, ?> uriVariables) {
		return this.sendByGet(url, uriVariables, null);
	}

	public Object sendByGet(String url, Map<String, ?> uriVariables, Map<String, ?> options) {
		if (SysValueUtil.isEmpty(uriVariables)) {
			return this.getRestTemplate(options).getForObject(url, Object.class);
		} else {
			return this.getRestTemplate(options).getForObject(url, Object.class, uriVariables);
		}
	}

	/**
	 * POST 방식으로 URL 호출하는데 파라미터 parameters로 호출
	 * 
	 * @param url
	 * @return
	 */
	public Object sendByPost(String url) {
		return this.sendByPost(url, null, null, null);
	}

	public Object sendByPost(String url, Object request) {
		return this.sendByPost(url, request, null, null);
	}

	public Object sendByPost(String url, Object request, Map<String, ?> uriVariables) {
		return this.sendByPost(url, request, uriVariables, null);
	}

	public Object sendByPost(String url, Object request, Map<String, ?> uriVariables, Map<String, ?> options) {
		if (SysValueUtil.isEmpty(uriVariables)) {
			return this.getRestTemplate(options).postForObject(url, request, Object.class);
		} else {
			return this.getRestTemplate(options).postForObject(url, request, Object.class, uriVariables);
		}
	}

	/**
	 * PUT 방식으로 URL 호출하는데 파라미터 parameters로 호출
	 * 
	 * @param url
	 * @param request
	 * @param parameters
	 * @return
	 */
	public Object sendByPut(String url, Object request) {
		return this.sendByPut(url, request, null, null);
	}

	public Object sendByPut(String url, Object request, Map<String, ?> uriVariables) {
		return this.sendByPut(url, request, uriVariables, null);
	}

	public Object sendByPut(String url, Object request, Map<String, ?> uriVariables, Map<String, ?> options) {
		this.getRestTemplate(options).put(url, request, uriVariables);
		return OK;
	}

	/**
	 * DELETE 방식으로 URL 호출하는데 파라미터 parameters로 호출
	 * 
	 * @param url
	 * @param request
	 * @param parameters
	 * @return
	 */
	public Object sendByDelete(String url) {
		return this.sendByDelete(url, null, null, null);
	}

	public Object sendByDelete(String url, Object request) {
		return this.sendByDelete(url, request, null, null);
	}

	public Object sendByDelete(String url, Object request, Map<String, ?> uriVariables) {
		return sendByDelete(url, request, uriVariables, null);
	}

	public Object sendByDelete(String url, Object request, Map<String, ?> uriVariables, Map<String, ?> options) {
		this.getRestTemplate(options).delete(url, uriVariables);
		return OK;
	}

	/**
	 * Option 파라메터를 이용하여 인증정보 설정 후, RestTemplate 가져오기 실행.
	 * 
	 * @return
	 */
	private RestTemplate getRestTemplate(Map<String, ?> options) {
		if (SysValueUtil.isEmpty(options)) {
			return this.getRestTemplate(null, null);
		}

		String type = SysValueUtil.toString(options.get(AUTH_TYPE));
		String key = SysValueUtil.toString(options.get(AUTH_KEY));

		if (SysValueUtil.isEmpty(type) || SysValueUtil.isEmpty(key)) {
			User currentUser = User.currentUser();
			if (SysValueUtil.isNotEmpty(currentUser)) {
				String authJsonParam = FormatUtil.toJsonString(currentUser);
				byte[] encodedAuthorisation = Base64.encodeBase64(authJsonParam.getBytes());

				type = AUTH_TYPE_JSON;
				key = new String(encodedAuthorisation);
			}
		}

		return this.getRestTemplate(type, key);
	}

	/**
	 * Token 인증 기반 RestTemplate 가져오기 실행.
	 * 
	 * @param key
	 * @return
	 */
	public RestTemplate getRestTemplateByToken(String key) {
		return this.getRestTemplate(AUTH_TYPE_TOKEN, key);
	}

	/**
	 * JSON 인증 기반 RestTemplate 가져오기 실행.
	 * 
	 * @param key
	 * @return
	 */
	public RestTemplate getRestTemplateByJson(String key) {
		return this.getRestTemplate(AUTH_TYPE_JSON, key);
	}

	/**
	 * type, key 파라메터를 이용하여 인증정보 설정 후, RestTemplate 가져오기 실행.
	 * 
	 * @param type
	 * @param key
	 * @return
	 */
	private RestTemplate getRestTemplate(String type, String key) {
		// 인증 정보가 존재하지 않을 경우.
		if (SysValueUtil.isEmpty(type) || SysValueUtil.isEmpty(key)) {
			return new RestTemplate();
		}

		// 인증 정보가 존재 할 경우, Request Header에 값 설정.
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory() {
			@Override
			protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
				super.prepareConnection(connection, httpMethod);
				connection.setRequestProperty(AUTH_TYPE, type);
				connection.setRequestProperty(AUTH_KEY, key);
			}
		};

		return new RestTemplate(requestFactory);
	}
}