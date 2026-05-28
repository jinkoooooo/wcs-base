package xyz.elidom.sec.system.auth;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.sec.SecConstants;
import xyz.elidom.sec.rest.LoginHistoryController;
import xyz.elidom.sec.util.SecurityUtil;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.entity.relation.DomainRef;
import xyz.elidom.sys.util.SessionUtil;
import xyz.elidom.sys.util.SysValueUtil;

//@Configuration
@Service
public class AuthenticationService {
	/**
	 * 환경설정 객체
	 */
	@Resource
	private Environment env;
	/**
	 * 쿼리 매니저
	 */
	@Autowired
	private IQueryManager queryManager;
	/**
	 * 로그인 이력 컨트롤러
	 */
	@Autowired
	private LoginHistoryController loginHistoryController;
	/**
	 * 사용자 조회 서비스
	 */
//	@Autowired
	//private UserDetailsService userDetailsService;
    @Autowired
    private AuthenticationManager authenticationManager;
	
	/**
	 * 프로바이더 매니저 생성
	 * 
	 * @return
	 */
//	@Bean
//	public ProviderManager createProviderManager() {
//		DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
//		daoAuthenticationProvider.setUserDetailsService(this.userDetailsService);
//		daoAuthenticationProvider.setPasswordEncoder(passwordEncoder())
//		List<AuthenticationProvider> list = ValueUtil.toList(daoAuthenticationProvider);
//		ProviderManager providerManager = new ProviderManager(list);
//		return providerManager;
//	}

	/**
	 * Login 사용자에 대한 인증 실행
	 * 
	 * @param req
	 * @param res
	 * @param id
	 * @param password
	 * @return
	 */
	public Authentication doAuthenticate(HttpServletRequest req, HttpServletResponse res, String id, String password) {
		// 1. 인증 결과
		Authentication authentication = null;
		
		// 2. 세션에서 현재 도메인 추출
		Domain domain = (Domain)SessionUtil.getAttribute(SecConstants.CURRENT_DOMAIN);
		String accessIp = (String)SessionUtil.getAttribute("ACCESS_IP");
//		ProviderManager providerManager = BeanUtil.get(ProviderManager.class);
		
		// 3. 비밀번호 암호화를 하지 않고 인증 실행.

		try {
			// 인증 시도
			authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(id, password));

		} catch (AuthenticationException ae) {
			try {
				// 비밀번호를 암호화 처리하여 인증 실행.
				String encodePass = SecurityUtil.encodePasswordV2(password);
				authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(id, encodePass));
				
			} catch (BadCredentialsException bce) {
				User user = this.queryManager.select(User.class, id);
				
				// 사용자가 존재 할 경우 Login 이력 생성.
				if (user != null) {
					this.loginHistoryController.saveLoginFailHistory(domain.getId(), id, accessIp);
					this.loginHistoryController.doLoginFailLock(user);
				}
				
				throw bce;
			}
		}
		
		// 4. 인증 정보를 Context에 저장
		SecurityContextHolder.getContext().setAuthentication(authentication);
		User user = SecurityUtil.getUser();
		DomainRef domainRef = user.getDomain();
		domainRef.setId(domain.getId());
		domainRef.setBrandName(domain.getBrandName());
		domainRef.setName(domain.getName());
		
		// 5. 로그인 정보 업데이트
		Date currentDate = new Date();
		user.setLastSignInAt(SysValueUtil.checkValue(user.getCurrentSignInAt(), currentDate));
		user.setCurrentSignInAt(currentDate);
		user.setAccountExpireDate(null);
		user.setDomainId(domain.getId());
		user.setUpdaterId(user.getId());
		this.queryManager.update(user, "lastSignInAt", "currentSignInAt", "accountExpireDate", "domainId", "updaterId", "updatedAt");
	
		// 6. 로그인 이력 생성
		this.loginHistoryController.saveLoginSuccessHistory(domain.getId(), user.getId(), accessIp);
		
		// 7. 인증 결과 리턴
		return authentication;
	}
}
