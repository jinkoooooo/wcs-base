/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sec.util;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import xyz.elidom.sec.model.ElidomUserDetails;
import xyz.elidom.sys.SysConfigConstants;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.system.auth.AuthProviderFactory;
import xyz.elidom.sys.util.SessionUtil;
import xyz.elidom.sys.util.SettingUtil;
import xyz.elidom.util.BeanUtil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * 보안 및 인증 관련 유틸리티 클래스
 * 
 * @author shortstop
 */
public class SecurityUtil {
	
	public static final String PARAMNAME_USERNAME = "j_username";
	public static final String PARAMNAME_PASSWORD = "j_password";
	public static final String USERID_ANONYMOUS = "anonymous";
	
	/**
	 * annonymous 여부 리턴
	 * 
	 * @return
	 */
	public static boolean isAnonymous() {
		Authentication auth = getAuthentication();
		return auth == null || auth instanceof AnonymousAuthenticationToken;
	}

	/**
	 * 인증정보 리턴
	 * 
	 * @return
	 */
	public static Authentication getAuthentication() {
		SecurityContext sc = SecurityContextHolder.getContext();
		if(sc == null) {
			sc = (SecurityContext) SessionUtil.getAttribute("SPRING_SECURITY_CONTEXT");
		}
		
		return sc == null ? null : sc.getAuthentication();
	}

	/**
	 * 인증된 사용자의 정보 리턴
	 * 
	 * @return
	 */
	public static User getUser() {
		Authentication auth = getAuthentication();
		if (auth == null || auth instanceof AnonymousAuthenticationToken) {
			return null;
		}

		ElidomUserDetails userDetails = (ElidomUserDetails) auth.getPrincipal();
		return userDetails.getUser();
	}
	
	/**
	 * 비밀번호를 암호화하여 리턴
	 * 
	 * @param value
	 * @return
	 */
	public static String encodePassword(String value) {
		return BeanUtil.get(AuthProviderFactory.class).getAuthProvider().encodePassword(value);
	}
	
	/**
	 * 비밀번호 일치 여부 확인
	 * 
	 * @param encPass : 암호화된 패스워드 값
	 * @param rawPass : 평문의 패스워드 값
	 * @return
	 */
	public static boolean isPasswordValid(String encPass, String rawPass) {
		return BeanUtil.get(AuthProviderFactory.class).getAuthProvider().isPasswordValid(encPass, rawPass);
	}
	
	/**
	 * 초기화 비밀번호 리턴
	 * 
	 * @return
	 */
	public static String newPass() {
		return SecurityUtil.encodePasswordV2(SettingUtil.getValue(SysConfigConstants.SECURITY_INIT_PASS));
	}

	/**
	 * 비빌번호를 암호화하여 리턴
	 *
	 * @return
	 */
	public static String encodePasswordV2(String value) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] hashed = md.digest(value.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(hashed);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("SHA-256 Algorithm not available", e);
		}
	}
}