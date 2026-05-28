/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sec;

import xyz.elidom.sys.SysConfigConstants;

/**
 * Security 모듈 설정 관련 상수 정의
 * 
 * @author shortstop
 */
public class SecConfigConstants extends SysConfigConstants {
	/**
	 * Permit All URL
	 */
	public static final String SECURITY_ALL_PERMIT_URI = "security.all.permit.uri";

	/**
	 * Read Only (GET Method) URL
	 */
	public static final String SECURITY_READ_ONLY_URI = "security.read.only.uri";

	/**
	 * Login Fail Lock Count
	 */
	public static final String LOGIN_FAIL_LOCK_COUNT = "login.fail.lock.count";

	/**
	 * Password Lock Minute
	 */
	public static final String USER_PASS_LOCK_MINUTE = "user.pass.lock.minute";

	/**
	 * Unused Account Lock
	 */
	public static final String USER_ACCOUNT_LOCK_UNUSED_DAY = "user.account.lock.unused.day";

	/**
	 * Super Admin Role 이름.
	 */
	public static final String ROLE_SUPER = "role.super";

	/**
	 * Admin Role 이름.
	 */
	public static final String ROLE_ADMIN = "role.admin";

	/**
	 * Operator Role 이름.
	 */
	public static final String ROLE_OPERATOR = "role.operator";

	/**
	 * Guest Role 이름.
	 */
	public static final String ROLE_GUEST = "role.guest";
	
	/**
	 * 계정 활성화, 비밀번호 초기화등의 요청 메일 발송 여부.
	 */
	public static final String USER_MAIL_REQUEST_AUTH_ENABLE = "user.mail.request.auth.enable";
	
	/**
	 * OAUTH2 인증 지원 여부 키
	 */
	public static final String SECURTY_OAUTH2_ENABLED = "security.oauth2.enabled";
}
