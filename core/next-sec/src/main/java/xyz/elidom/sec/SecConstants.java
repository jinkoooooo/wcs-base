/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sec;

import xyz.elidom.sys.SysConstants;

/**
 * Security 모듈에 필요한 상수 정의
 * 
 * @author shortstop
 */
public class SecConstants extends SysConstants {
	
	/**
	 * HTTP Authorization 헤더의 인증 타입 키
	 */
	public static final String AUTH_TYPE = "Authorization-Type";
	/**
	 * HTTP Authorization 헤더의 인증 키 키
	 */
	public static final String AUTH_KEY = "Authorization-Key";

	/**
	 * HTTP Authorization 헤더의 인증 방법 - JSON 방식
	 */
	public static final String AUTH_TYPE_JSON = "json";
	/**
	 * HTTP Authorization 헤더의 인증 방법 - 토큰 방식
	 */
	public static final String AUTH_TYPE_TOKEN = "token";
	/**
	 * HTTP Authorization 헤더의 인증 방법 - JWT
	 */
	public static final String AUTH_TYPE_JWT = "jwt";

	public static final String REQUEST_URL_PATTERN = "request_url_pattern";

	/**
	 * 접근 가능한 도메인 목록을 Session의 저장하기 위한 키 값
	 */
	public static final String PERMITTED_DOMAINS = "PERMITTED_DOMAINS";

	public static final String PERMIT_URL_TYPE_ALL = "ALL";
	public static final String PERMIT_URL_TYPE_READ_ONLY = "READ_ONLY";

	// Account Login Info.
	public static final String ACCOUNT_LOGIN_INFO = "login_info";

	public static final String IP_ADDRESS = "IP_ADDRESS";

	public static final String USER_CREATED = "CREATED";
	public static final String USER_DELETED = "DELETED";
	public static final String USER_ACTIVE = "ACTIVE";
	public static final String USER_INACTIVE = "INACTIVE";
	public static final String USER_PASSWORD_LOCK = "PASSWORD_LOCK";

	public static final String USER_ROLE_CREATED = "CREATED";
	public static final String USER_ROLE_DELETED = "DELETED";

	// V6 방식의 Local Host 표현.
	public static final String LOCALHOST_V6 = "0:0:0:0:0:0:0:1";
}
