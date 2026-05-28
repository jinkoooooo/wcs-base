package xyz.elidom.sec.system.auth;

import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import xyz.elidom.exception.client.ElidomUnauthorizedException;
import xyz.elidom.sec.SecConstants;
import xyz.elidom.sec.entity.LoginHistory;
import xyz.elidom.sec.entity.Role;
import xyz.elidom.sec.rest.LoginHistoryController;
import xyz.elidom.sec.util.SecurityUtil;
import xyz.elidom.sys.SysConfigConstants;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.system.auth.IAuthProvider;
import xyz.elidom.sys.util.SessionUtil;
import xyz.elidom.sys.util.SettingUtil;
import xyz.elidom.sys.util.SysValueUtil;
import xyz.elidom.util.BeanUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 인증을 사용하는 인증 기능 프로바이더
 * 
 * @author shortstop
 */
public class SecAuthProvider implements IAuthProvider {
    private PasswordEncoder passwordEncoder;

    public SecAuthProvider() {
        this.passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

	@Override
	public int getProviderPriority() {
		return -1;
	}
	
	@Override
	public String loginToUserId(String login) {
		return login;
	}
	
	@Override
	public User currentUser() {
		User user = SecurityUtil.getUser();

		// 인증되지 않은 사용자
		if (SysValueUtil.isEmpty(user)) {
			user = new User();
			user.setName("annonymous");
		}

		if (SysValueUtil.isEmpty(user)) {
			throw new ElidomUnauthorizedException();
		}

		user.setLocale((String) SessionUtil.getAttribute(SysConstants.LOCALE));
		return user;
	}
	
	@Override
	public Object sessionUserInfo(Domain currentDomain, User user) {
		user.setDomainId(currentDomain.getId());
		Map<String, Object> sessionInfo = this.userToMap(user);
		Map<String, Object> domainInfo = this.domainToMap(currentDomain);
		sessionInfo.put("domain", domainInfo);

		List<Role> roles = Role.getRoles(user.getId());
		List<Map<String, Object>> roleList = new ArrayList<Map<String, Object>>(3);
		for (Role role : roles) {
			roleList.add(SysValueUtil.newMap("id,name,description", role.getId(), role.getName(), role.getDescription()));
		}

		sessionInfo.put("roles", roleList);

		try {
			// 최종 로그인 정보 추출.
			LoginHistory lastLoginInfo = BeanUtil.get(LoginHistoryController.class).getLastLoginInfo(user.getId());
			sessionInfo.put(SecConstants.ACCOUNT_LOGIN_INFO, lastLoginInfo);
		} catch (Exception e) {
		}

		return sessionInfo;
	}

	@Override
	public String encodePassword(String defaultPass) {
//		return BeanUtil.get(MessageDigestPasswordEncoder.class).encodePassword(defaultPass, null);
		return  passwordEncoder.encode(defaultPass);
	}

	@Override
	public boolean isPasswordValid(String encPass, String rawPass) {
		boolean result = SysValueUtil.isEqual(encPass, rawPass);
		if (!result)
//			result = BeanUtil.get(MessageDigestPasswordEncoder.class).isPasswordValid(encPass, rawPass, null);
			result = passwordEncoder.matches(rawPass, encPass);


		return result;
	}

	@Override
	public String newPass() {
		boolean useRandomPass = SysValueUtil.toBoolean(SettingUtil.getValue(SysConfigConstants.SECURITY_USE_RANDOM_INIT_PASS, SysConstants.TRUE_STRING));
		return useRandomPass ? randomPassword() : SettingUtil.getValue(SysConfigConstants.SECURITY_INIT_PASS);
	}

	/**
	 * Random Password 생성
	 * 
	 * @return
	 */
	public static String randomPassword() {
		char[] initRandomChar = {
			'a', 'b', 'c', 'd', 'e', 'f', 
			'g', 'h', 'i', 'j', 'k', 'l', 
			'm', 'n', 'o', 'p', 'q', 'r', 
			's', 't', 'u', 'v', 'w', 'x', 
			'y', 'z', '0', '1', '2', '3', 
			'4', '5', '6', '7', '8', '9'
		};

		char[] randomChar = new char[6];
		
		for (int i = 0; i < 6; i++) {
			randomChar[i] += initRandomChar[(int) (Math.random() * initRandomChar.length)];
		}

		StringBuffer buf = new StringBuffer();
		for (char randChar : randomChar) {
			buf.append(randChar);
		}
		
		return buf.toString();
	}

	/**
	 * 사용자 객체를 Map으로 변환
	 * 
	 * @param user 사용자 정보
	 * @return
	 */
	private Map<String, Object> userToMap(User user) {
		Map<String, Object> sessionInfo = SysValueUtil.newMap(
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
		Map<String, Object> domainInfo = SysValueUtil.newMap("id,name,brandName,theme", domain.getId(), domain.getName(), domain.getBrandName(), domain.getTheme());
		return domainInfo;
	}

}
