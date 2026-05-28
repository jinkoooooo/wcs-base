/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.auth;

import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.entity.User;

/**
 * 인증 기능에 대한 프로바이더 인터페이스   
 * 
 * @author shortstop
 */
public interface IAuthProvider {
	
	/**
	 * AuthProviderFactory에 설정될 IAuthProvider의 우선순위를 설정하기 위한 정보
	 * 
	 * @return priority
	 */
	public int getProviderPriority();
	
	/**
	 * login과 userId간 매핑 규칙 설정
	 * 화면에서 입력한 login 정보로 부터 시스템에서 로그인시 사용하는 userId로 매핑하는 메소드
	 * 기본 구현은 login == userId
	 * 
	 * @param login
	 * @return
	 */
	public String loginToUserId(String login);

	/**
	 * 현재 세션의 사용자 정보를 리턴 
	 * 
	 * @return
	 */
	public User currentUser();
	
	/**
	 * 로그인 시 currentDomain, user 정보로 부터 세션 정보를 생성  
	 * 
	 * @param currentDomain
	 * @param user
	 * @return
	 */
	public Object sessionUserInfo(Domain currentDomain, User user);
	
	/**
	 * 패스워드를 인코딩한다. 
	 * 
	 * @param defaultPass
	 * @return
	 */
	public String encodePassword(String defaultPass);
	
	/**
	 * 패스워드가 유효한 지 체크 
	 * 
	 * @param encPass
	 * @param rawPass
	 * @return
	 */
	public boolean isPasswordValid(String encPass, String rawPass);
	
	/**
	 * 새로운 패스워드를 생성한다.
	 * 
	 * @return
	 */
	public String newPass();
}
