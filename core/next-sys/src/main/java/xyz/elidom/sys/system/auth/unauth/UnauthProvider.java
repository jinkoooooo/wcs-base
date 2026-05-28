/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.auth.unauth;

import java.util.Map;

import xyz.elidom.sys.SysConfigConstants;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.entity.relation.DomainRef;
import xyz.elidom.sys.system.auth.IAuthProvider;
import xyz.elidom.sys.util.SettingUtil;
import xyz.elidom.util.ValueUtil;

/**
 * 인증을 사용하지 않는 인증 기능 프로바이더 
 * 
 * @author shortstop
 */
public class UnauthProvider implements IAuthProvider {

	@Override
	public int getProviderPriority() {
		return -10;
	}
	
	@Override
	public String loginToUserId(String login) {
		return login;
	}
	
	@Override
	public User currentUser() {
		User currentUser = new User();
		currentUser.setId("admin");
		currentUser.setName("admin");
		currentUser.setLogin("admin");
		currentUser.setEmail("admin@example.com");
		
		Domain currentDomain = Domain.currentDomain();
		DomainRef domain = new DomainRef();
		domain.setId(currentDomain.getId());
		domain.setName(domain.getName());
		domain.setBrandName(domain.getBrandName());
		domain.setBrandImage(domain.getBrandImage());
		domain.setTheme(domain.getTheme());
		currentUser.setDomain(domain);
		return currentUser;
	}
	
	@Override
	public Object sessionUserInfo(Domain currentDomain, User user) {
		user.setDomainId(currentDomain.getId());
		Map<String, Object> sessionInfo = this.userToMap(user);
		Map<String, Object> domainInfo = this.domainToMap(currentDomain);
		sessionInfo.put("domain", domainInfo);
		return sessionInfo;
	}	

	@Override
	public String encodePassword(String defaultPass) {
		// no encoding
		return defaultPass;
	}

	@Override
	public boolean isPasswordValid(String encPass, String rawPass) {
		// check if two passwords same
		return ValueUtil.isEqualIgnoreCase(encPass, rawPass);
	}

	@Override
	public String newPass() {
		return SettingUtil.getValue(SysConfigConstants.SECURITY_INIT_PASS);
	}
	
	/**
	 * 사용자 객체를 Map으로 변환
	 * 
	 * @param user 사용자 정보
	 * @return
	 */
	private Map<String, Object> userToMap(User user) {
		Map<String, Object> sessionInfo = ValueUtil.newMap(
				"id,domain_id,login,email,name,dept,division,locale,super_user,admin_flag,operator_flag,exclusive_role", 
				user.getId(), 
				user.getDomainId(),
				user.getLogin(), 
				user.getEmail(), 
				user.getName(), 
				user.getDept(), 
				user.getDivision(), 
				user.getLocale(), 
				user.getSuperUser(), 
				user.getAdminFlag(), 
				user.getOperatorFlag(),
				user.getExclusiveRole());		
		return sessionInfo;
	}
	
	/**
	 * 도메인 객체를 Domain으로 변환 
	 * 
	 * @param domain
	 * @return
	 */
	private Map<String, Object> domainToMap(Domain domain) {
		Map<String, Object> domainInfo = ValueUtil.newMap("id,name,brandName,theme", domain.getId(), domain.getName(), domain.getBrandName(), domain.getTheme());
		return domainInfo;
	}	

}
