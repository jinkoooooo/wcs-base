package xyz.elidom.sys.system.context;

import xyz.elidom.sys.entity.Domain;

/**
 * 스레드 실행시 Domain 설정을 위한 컨텍스트
 *  
 * @author shortstop
 */
public class DomainContext {
	/**
	 * 도메인 변수를 유지할 스레드 로컬
	 */
	public static final ThreadLocal<Domain> domainContext = new ThreadLocal<Domain>();
	/**
	 * 사용자 변수를 유지할 스레드 로컬
	 */
	public static final ThreadLocal<Object> userContext = new ThreadLocal<Object>();

	/**
	 * 현재 스레드 변수에 현재 도메인을 설정
	 * 
	 * @param domain
	 */
	public static void setCurrentDomain(Domain currentDomain) {
		domainContext.set(currentDomain);
	}
	
	/**
	 * 현재 스레드 변수에서 도메인을 조회
	 * 
	 * @return
	 */
	public static Domain getCurrentDomain() {
		return domainContext.get();
	}

	/**
	 * 현재 스레드에서 현재 도메인 리셋
	 * 스레드 풀 사용시 unset하지 않으면 스레드 풀에 들어갈 때 currentDomain을 물고 들어가기 때문에 스레드 해제 전에 반드시 unset을 해줘야 함 
	 */
	public static void unsetCurrentDomain() {
		domainContext.remove();
	}

	/**
	 * 현재 스레드 변수에 사용자 Object 설정
	 * 
	 * @param userObj
	 */
	public static void setUserObject(Object userObj) {
		userContext.set(userObj);
	}
	
	/**
	 * 현재 스레드에서 사용자 Object 조회
	 * 
	 * @return
	 */
	public static Object getUserObject() {
		return userContext.get();
	}
	
	/**
	 * 현재 스레드에서 사용자 Object 제거
	 */
	public static void unsetUserObject() {
		userContext.remove();
	}
	
	/**
	 * 스레드 로컬 변수 모두 클리어
	 */
	public static void unsetAll() {
		domainContext.remove();
		userContext.remove();
	}
}
