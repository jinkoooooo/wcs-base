/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.auth;

import org.springframework.stereotype.Component;

/**
 * 인증 기능 프로바이더 제공하는 Factory 클래스 
 * 오버라이드 하고자 하는 모듈에서 IAuthProvider를 오버라이드한다. 
 * 
 * @author shortstop
 */
@Component
public class AuthProviderFactory {

	/**
	 * 인증 기능 프로바이더 
	 */
	private IAuthProvider authProvider;
	
	/**
	 * @return the authSupport
	 */
	public IAuthProvider getAuthProvider() {
		return this.authProvider;
	}
	
	/**
	 * @param authSupport to set
	 */
	public void setAuthProvider(IAuthProvider authProvider) {
		if(this.authProvider == null) {
			this.authProvider = authProvider;
		} else {
			// 새로 설정될 authProvider가 이전에 설정된 것 보다 priority가 높다면 덮어쓴다. 
			if(authProvider.getProviderPriority() > this.authProvider.getProviderPriority()) {
				this.authProvider = authProvider;
			}
		}
	}
}
