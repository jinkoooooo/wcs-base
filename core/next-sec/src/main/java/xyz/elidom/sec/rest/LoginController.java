/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sec.rest;

import java.io.Serializable;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import org.apache.tomcat.util.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import xyz.elidom.exception.client.ElidomUnauthorizedException;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sec.SecConfigConstants;
import xyz.elidom.sec.SecConstants;
import xyz.elidom.sec.system.auth.AuthenticationService;
import xyz.elidom.sec.util.SecurityUtil;
import xyz.elidom.sys.SysConfigConstants;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.system.auth.AuthProviderFactory;
import xyz.elidom.sys.util.MessageUtil;
import xyz.elidom.sys.util.SessionUtil;
import xyz.elidom.sys.util.SettingUtil;
import xyz.elidom.sys.util.SysValueUtil;
import xyz.elidom.sys.util.ThrowUtil;
import xyz.elidom.util.DateUtil;

@RestController
@Transactional
@ResponseStatus(HttpStatus.OK)
@ServiceDesc(description = "Login Service API")
@RequestMapping("/rest")
public class LoginController {


	class EmptyObject implements Serializable{

		private static final long serialVersionUID = -7394231593714216905L;

		@JsonInclude(JsonInclude.Include.NON_NULL)
		String subDomain;

		public String getSubDomain() {
			return subDomain;
		}

		public void setSubDomain(String subDomain) {
			this.subDomain = subDomain;
		}
	}


	/**
	 * logger
	 */
	protected Logger logger = LoggerFactory.getLogger(LoginController.class);

	/**
	 * 쿼리 매니저
	 */
	@Autowired
	private IQueryManager queryManager;
	/**
	 * 인증 서비스
	 */
	@Autowired
	private AuthenticationService authenticationService;
	/**
	 * 로그인 이력 컨트롤러
	 */
	@Autowired
	private LoginHistoryController loginHistoryController;
	/**
	 * 인증 정보 프로바이더
	 */
	@Autowired
	private AuthProviderFactory authProviderFactory;

	@Value("${server.session.timeout:2592000}")
	int sessionTime;



	/**
	 * 사용자 ID/Password 정보로 사용자 체크 후 토큰 발급
	 *
	 * @param req
	 * @param res
	 * @param login
	 * @param password
	 * @param requesterId
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	@PostMapping(value = "/check_user", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Check User")
	public Map<String, Object> checkUser(
			HttpServletRequest req,
			HttpServletResponse res,
			@RequestParam(name = "email") String login,
			@RequestParam(name = "password") String password,
			@RequestParam(name = "requester_id") String requesterId) {

		Domain domain = this.findDomainByRequest(req);

		String clientType = (String)SessionUtil.getAttribute("CLIENT_TYPE");
		if(clientType != null && SysValueUtil.isEqual(clientType, "MOBILE")) {
			domain = Domain.systemDomain();
		} else {
			domain = this.findDomainByRequest(req);
		}

		// 1. 로그인 한 도메인이 사이트 도메인이면 /rest/login URL로 요청하라고 상태를 알려줌
		if(domain != null && !domain.getSystemFlag()) {
			return SysValueUtil.newMap("status", "site_domain");
		}

		// 3. 로그인 정보로 부터 사용자 ID를 조회
		String ipAddr = SysValueUtil.getRemoteIp(req);
		User user = this.getUserForAuth(domain, login, ipAddr);

		// 4. 계정 상태 체크, null이면 OK, 나머지는 추가 프로세스 필요
		Map<String, Object> userInfo = this.checkValidAccount(domain, user, password, ipAddr);
		String accountStatus = (String)userInfo.get("status");

		// 5. 로그인 가능 상태
		if(SysValueUtil.isEqual(SysConstants.OK_STRING, accountStatus)) {
			// 5.1 사이트 권한
			List<Map> siteList = this.searchSiteList(user);

			// 5.2 사이트 접근 권한이 없는 경우 에러 발생
			if(SysValueUtil.isEmpty(siteList)) {
				throw new ElidomRuntimeException(MessageUtil.getMessage("NO_SITE_PERMISSION"));
			} else {
				userInfo.put("site_list", siteList);
			}
		}

		return userInfo;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@PostMapping(value = "/site_list_by_user/{requester_id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Site List by User")
	public List<?> siteList(HttpServletRequest req, @PathVariable("requester_id") String requesterId) {
		// 1. 현재 도메인 & 사용자 추출
		Domain currentDomain = Domain.currentDomain();
		User currentUser = User.currentUser();

		// 2. 사용자가 방문 권한이 있는 사이트 리스트 조회
		List<Map> siteList = this.searchSiteList(currentUser);

		for(Map site : siteList) {
			String domainId = SysValueUtil.getMapData(site, "id").toString();
			site.put("current_domain", SysValueUtil.isEqualIgnoreCase(domainId, currentDomain.getId().toString()));
		}

		// 5. 결과 리턴
		return siteList;
	}

	/**
	 * siteList 조회
	 *
	 * @param serverName
	 * @param currentUserId
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private List<Map> searchSiteList(User currentUser) {
		String sql = "select id, name, brand_name, description, subdomain from domains where id in (select domain_id from domain_users where user_id = :userId)";

		if(currentUser.getSuperUser()) {
			sql = "select id, name, brand_name, description, subdomain from domains";
		}

		Map<String, Object> params = SysValueUtil.newMap("userId,systemFlag", currentUser.getId(), true);
		return this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);
	}

	/**
	 * Request로 부터 브라우저에서 접근한 도메인으로 부터 매핑된 도메인(사이트)를 찾아냄
	 *
	 * @param req
	 * @return
	 */
	public Domain findDomainByRequest(HttpServletRequest req) {
		Domain domain = (Domain)SessionUtil.getAttribute(SysConstants.CURRENT_DOMAIN);
		// 없다면 에러
		if(domain == null) {
			throw ThrowUtil.newDomainNotExist(SysValueUtil.getClientRequestSubDomain(req));
		}

		return domain;
	}

	/**
	 * 1. login이 base64로 인코딩 되어 있다고 추정하여 decoding한 뒤 사용자 조회 시도
	 * 2. 조회 중 에러 발생하면 decoding 하지 않은 상태로 사용자 조회 시도
	 * 3. 조회 중 에러 발생하면 로그인 정보 불일치로 판단하여 에러 발생
	 *
	 * @param domain
	 * @param login
	 * @param ipAddr
	 * @return
	 */
	private User getUserForAuth(Domain domain, String login, String ipAddr) {
		try {
			return this.findUserForAuth(domain, this.decodeLogin(login), ipAddr);
		} catch (ElidomRuntimeException e) {
			return this.findUserForAuth(domain, login, ipAddr);
		}
	}

	/**
	 * base64 decoding login parameter
	 * @param login
	 * @return
	 */
	private String decodeLogin(String login) {
	    return new String(Base64.getDecoder().decode(login));
	}

	/**
	 * 인증을 위해 login 정보로 사용자 조회
	 *
	 * @param domain
	 * @param login
	 * @param ipAddr
	 * @return
	 */
	private User findUserForAuth(Domain domain, String login, String ipAddr) {
		String userId = this.authProviderFactory.getAuthProvider().loginToUserId(login);
		User user = this.queryManager.select(User.class, userId);

		if(user == null) {
			throw new ElidomRuntimeException(MessageUtil.getMessage("USER_INVALID_ID_OR_PASS"));
		}

		return user;
	}

	/**
	 * 사용자 정보가 유효한 지 체크
	 *
	 * @param domain
	 * @param user
	 * @param password
	 * @param ipAddress
	 * @return 계정 상태
	 */
	private Map<String, Object> checkValidAccount(Domain domain, User user, String password, String ipAddress) {
		// 1. 사용자 활성화 상태 체크
		if(!user.getActiveFlag()) {
			throw new ElidomRuntimeException(MessageUtil.getMessage("ACCOUNT_LOCKED"));
		}

		// 2. 비밀번호 체크
		boolean validUser = false;
		if(user != null) {
			String encPasswd = user.getEncryptedPassword();
			validUser = SysValueUtil.isEqual(encPasswd, password);

			if(!validUser) {
				String encodePass = SecurityUtil.encodePassword(password);
				validUser = SysValueUtil.isEqual(encPasswd, encodePass);
			}
		}

		// 3. 비밀번호가 맞지 않으면 로그인 실패 이력 기록
		if(!validUser) {
			this.loginHistoryController.saveLoginFailHistory(domain.getId(), user.getId(), ipAddress);
			this.loginHistoryController.doLoginFailLock(user);
			throw new ElidomRuntimeException(MessageUtil.getMessage("USER_INVALID_ID_OR_PASS"));
		}

		// 4. 계정 일시 잠금 체크
		if(!this.isAccountNonLocked(user)) {
			throw new ElidomRuntimeException(MessageUtil.getMessage("ACCOUNT_TEMPORARY_LOCKED","계정이 일시 잠금 상태입니다. {0}분 후에 시도해주세요",SysValueUtil.newStringList(SettingUtil.getValue(SecConfigConstants.USER_PASS_LOCK_MINUTE,"0"))));
		}

		// 5. 계정 상태 체크, null이면 OK, 나머지는 추가 프로세스 필요
		String accountStatus = this.isCredentialsNonExpired(user);

		// 6. 계정 정보 및 상태 리턴
		return SysValueUtil.newMap(
				"id,name,locale,timezone,account_type,super_user,admin_flag,status",
				user.getId(),
				user.getName(),
				user.getLocale(),
				user.getTimezone(),
				user.getAccountType(),
				user.getSuperUser(),
				user.getAdminFlag(),
				(accountStatus == null ? SysConstants.OK_STRING : accountStatus));
	}

	/**
	 * 모바일 화면 로그인
	 * @param req
	 * @param res
	 * @param login
	 * @param password
	 * @param domainId
	 * @return
	 */
	@PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Login")
	public Map<String, Object> login(
			HttpServletRequest req,
			HttpServletResponse res,
			@RequestParam(name = "email") String login,
			@RequestParam(name = "password") String password,
			@RequestParam(name = "domainId", required = false) Long domainId) {

		Domain domain = null;

		// operator 로그인은 domainId 필수
		if(SysValueUtil.isNotEmpty(domainId)) {
			domain = Domain.find(domainId);
			SecurityContextHolder.clearContext();
		} else {
			// 클라이언트 접근 URL로 부터 도메인을 조회
			domain = this.findDomainByRequest(req);
		}

		// 로그인 정보로 사용자 정보 체크
		String ipAddr = SysValueUtil.getRemoteIp(req);
		User user = this.getUserForAuth(domain, login, ipAddr);

		// 사용자 정보가 유효한 지 체크
		Map<String, Object> userInfo = this.checkValidAccount(domain, user, password, ipAddr);
		String accountStatus = (String)userInfo.get("status");

		// 계정 상태가 OK가 아니면 사용자 정보 리턴
		if(SysValueUtil.isNotEqual(accountStatus, SysConstants.OK_STRING)) {
			return userInfo;
			// 계정 상태가 OK이면 인증 실행
		} else {
			// 세션에 도메인, 접속 IP 설정
			SessionUtil.setAttribute(SecConstants.CURRENT_DOMAIN, domain);
			SessionUtil.setAttribute("ACCESS_IP", ipAddr);

			// operator 로그인은 domainId 필수
			if(SysValueUtil.isNotEmpty(domainId)) {
				SessionUtil.setAttribute("CLIENT_TYPE", "MOBILE");
			} else {
				SessionUtil.setAttribute("CLIENT_TYPE", "MANAGER");
			}

			// 인증 실행
			this.authenticationService.doAuthenticate(req, res, user.getId(), password);

			// 세션 정보 리턴
			Map<String, Object> sessionInfo = this.currentSession(user, domain);
			sessionInfo.put(SecConstants.ACCOUNT_STATUS, accountStatus);
			return sessionInfo;
		}
	}


	/**
	 * 매니저 화면 로그인
	 * @param req
	 * @param res
	 * @param login
	 * @param password
	 * @param domainId
	 * @return
	 */
	@PostMapping(value = "/man_login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Login Manager")
	public Map<String, Object> managerLogin(
			HttpServletRequest req,
			HttpServletResponse res,
			@RequestBody Map<String,String> param){


		if(SysValueUtil.isEmpty(req.getHeader("x-domain-id"))) {
			throw new RuntimeException("Current Domain Not Exist!");
		}


		String login = param.get("email");
		String password = param.get("pass");

		Domain domain = Domain.findBySubdomain(req.getHeader("x-domain-id"));

		if(SysValueUtil.isEmpty(domain)) {
			throw new RuntimeException("Current Domain Not Exist!");
		}


		// 로그인 정보로 사용자 정보 체크
		String ipAddr = SysValueUtil.getRemoteIp(req);
		User user = this.getUserForAuth(domain, login, ipAddr);

		// 사용자 정보가 유효한 지 체크
		Map<String, Object> userInfo = this.checkValidAccount(domain, user, password, ipAddr);
		String accountStatus = (String)userInfo.get("status");

		if(SysValueUtil.isEqual(accountStatus, SecConstants.ACCOUNT_STATUS_PASSWORD_CHANGE)
				|| SysValueUtil.isEqual(accountStatus, SecConstants.ACCOUNT_STATUS_PASSWORD_EXPIRED)) {
			accountStatus = SysConstants.OK_STRING;
		}

		// 계정 상태가 OK가 아니면 사용자 정보 리턴
		if(SysValueUtil.isNotEqual(accountStatus, SysConstants.OK_STRING)) {
			return userInfo;
			// 계정 상태가 OK이면 인증 실행
		} else {
			// 세션에 도메인, 접속 IP 설정
			SessionUtil.setAttribute(SecConstants.CURRENT_DOMAIN, domain);
			SessionUtil.setAttribute("ACCESS_IP", ipAddr);
			SessionUtil.setAttribute("CLIENT_TYPE", "MANAGER");

			// 인증 실행
			this.authenticationService.doAuthenticate(req, res, user.getId(), password);

			Date iat = new Date();
			Calendar c = Calendar.getInstance();
			c.setTime(iat);
			c.add(Calendar.SECOND, this.sessionTime);
			Date exp = c.getTime();

			List<Object> domains = SysValueUtil.newList(domain);
			String tokenStr = this.createToken(domain, user, iat, exp);

			Cookie token = new Cookie("access_token", tokenStr);
			token.setMaxAge(this.sessionTime);
			token.setPath("/"); // 모든 경로에서 접근 가능 하도록 설정
			res.addCookie(token);

			String locale = user.getLocale();
			if(SysValueUtil.isNotEmpty(req.getHeader("x-locale"))) {
				locale = req.getHeader("x-locale");
			}

			if(locale.indexOf("ko")>0) {
				locale = "ko-KR";
			} else if(locale.indexOf("en")>0){
				locale = "en-US";
			} else if(locale.indexOf("zh")>0){
				locale = "zh-CN";
			} else {
				locale = "ko-KR";
			}

			Cookie i18next = new Cookie("i18next", locale);
			i18next.setMaxAge(this.sessionTime);
			i18next.setPath("/"); // 모든 경로에서 접근 가능 하도록 설정
			res.addCookie(i18next);

			return SysValueUtil.newMap("success,user,token,domains", true, user,"xxxx",domains);
		}
	}


	private String createToken(Domain domain, User user, Date iat, Date exp) {
		String SECRET_KEY = "0xD58F835B69D207A76CC5F84a70a1D0d4C79dAC95";

		return Jwts.builder()
				.setHeader(SysValueUtil.newMap("alg,typ", "HS256","JWT"))
				.claim("id", user.getId())
				.claim("userType", user.getUserType())
				.claim("status", user.getStatus())
				.claim("domain", new EmptyObject())
				.setIssuedAt(iat)
				.setExpiration(exp)
				.setIssuer("hatiolab.com")
				.setSubject("user")
				.signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()), SignatureAlgorithm.HS256)
				.compact();
	}


	/**
	 * 현재 화면과 체결된 세션 정보
	 * @return
	 */
	@GetMapping(value = "/session_info", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Find Session Information")
	public Map<String, Object> sessionInfo() {
		Domain currentDomain = Domain.currentDomain();

		User currentUser = SecurityUtil.getUser();

		// 로그인한 사용자 정보가 없으면
		if(currentUser == null) {
			ElidomUnauthorizedException eue = new ElidomUnauthorizedException();
			eue.setWritable(false);
			throw eue;
		}

		Map<String, Object> s = this.currentSession(currentUser, currentDomain);
		return s;
	}

	@GetMapping(value = "/check_auth", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Check User Auth Domain List")
	public Map<String, Object> checkAuth(HttpServletRequest req) {
		Domain currentDomain = Domain.currentDomain();
		User currentUser = SecurityUtil.getUser();

		// 로그인한 사용자 정보가 없으면
		if(currentUser == null) {
			ElidomUnauthorizedException eue = new ElidomUnauthorizedException();
			eue.setWritable(false);
			throw eue;
		}

		Map<String,Object> retMap = new HashMap<String,Object>();

		// 1. subDomain이 없는 경우 (시스템 도메인으로 route용 url인 경우)
		if(SysValueUtil.isEqual(SysValueUtil.getClientRequestSubDomain(req), "_ROOT_")) {
			String qry = "SELECT * FROM DOMAINS WHERE ID in (SELECT DOMAIN_ID FROM DOMAIN_USERS WHERE USER_ID = :userId) AND SYSTEM_FLAG = :systemFlag";

			List<Domain> domainList = this.queryManager.selectListBySql(qry, SysValueUtil.newMap("userId,systemFlag", currentUser.getId(), false), Domain.class, 0, 0);
			// 1.1. 사용자가 super user 이면 system domain 접근 권한 있음
			if(currentUser.getSuperUser()) domainList.add(currentDomain);

			retMap.put("result", "SITE_LIST");
			retMap.put("site_list", domainList);

		} else {
			// 2. subDomain이 있는 경우
			String qry = "SELECT count(1) FROM DOMAIN_USERS WHERE USER_ID = :userId AND DOMAIN_ID = :domainId";

			int userDomainCheck = this.queryManager.selectBySql(qry, SysValueUtil.newMap("userId,domainId", currentUser.getId(), currentDomain.getId()), Integer.class);
			// 도메인 정보가 없으면 ... 사용할 수 없는 도메인
			if(userDomainCheck == 0) {
				// 2.1. system domain + super user 는 pass
				if(currentUser.getSuperUser() && currentDomain.getSystemFlag()) retMap.put("result", "OK");
				else {
					SessionUtil.removeAttribute(SysConstants.CURRENT_DOMAIN);
					retMap.put("result", "USER_DOMAIN_NOT_AUTH");
				}
			} else {
				retMap.put("result", "OK");
			}
		}

		return retMap;
	}

	@PostMapping("/logout")
	@ApiDesc(description = "Logout")
	public boolean logout(HttpServletRequest req, HttpServletResponse res) {
		this.loginHistoryController.updateLogOutInfo();
		SecurityContextHolder.clearContext();

		if (req != null) {
			HttpSession session = req.getSession();
			if (session != null) {
				session.invalidate();
			}
		}

		Cookie token = new Cookie("access_token", null);
		token.setMaxAge(0);
		token.setPath("/");
		res.addCookie(token);

		Cookie i18next = new Cookie("i18next", null);
		i18next.setMaxAge(0);
		i18next.setPath("/");
		res.addCookie(i18next);

		return true;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> currentSession(User currentUser, Domain domain) {
		return (Map<String, Object>) this.authProviderFactory.getAuthProvider().sessionUserInfo(domain, currentUser);
	}

	/**
	 * 계정이 잠겨 있지 않은지 체크
	 *
	 * @param user
	 * @return
	 */
	private boolean isAccountNonLocked(User user) {
		String expireDate = user.getAccountExpireDate();
		if (SysValueUtil.isEmpty(expireDate)) {
			return true;
		}

		int lockMinute = SysValueUtil.toInteger(SettingUtil.getValue(SecConfigConstants.USER_PASS_LOCK_MINUTE), 0);
		if (lockMinute < 1) {
			return true;
		}

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(SysValueUtil.toDate(expireDate));
		calendar.add(Calendar.MINUTE, lockMinute);

		Date lockPeriod = calendar.getTime();
		return new Date().compareTo(lockPeriod) > 0;
	}

	/**
	 * 비밀번호 만료 상태 확인.
	 *
	 * @param user
	 * @return
	 */
	private String isCredentialsNonExpired(User user) {
		// 비밀번호 만료 날짜가 비어있는 경우 초기값 설정 (오늘 날짜 + 변경 주기)
		String expireDate = user.getPasswordExpireDate();

		// 비밀번호 만료 설정이 활성화 되어 있지 않을 경우 OK
		if (!SysValueUtil.toBoolean(SettingUtil.getValue(SysConfigConstants.USER_PASSWORD_EXPIRE_ENABLE), false)) {
			return null;
		}

		// 비밀번호 만료 상태 Check
		String accountType = user.getAccountType();

		// 계정 타입이 User가 아니거나, 비어있지 않은 경우 OK
		if (!(SysValueUtil.isEmpty(accountType) || SysValueUtil.isEqual(accountType, SysConstants.ACCOUNT_TYPE_USER))) {
			return null;
		}

		// 기본 비밀번호와 동일한지 확인.
		boolean isDefaultPass = false;
		String defaultPass = SettingUtil.getValue(SysConfigConstants.SECURITY_INIT_PASS);
		if (SysValueUtil.isNotEmpty(defaultPass)) {
			isDefaultPass = SysValueUtil.isEqual(user.getEncryptedPassword(), SecurityUtil.encodePassword(defaultPass));
		}

		// 계정 상태
		String accountStatus = null;

		// 만료 날짜가 현재 날짜보다 작을 경우
		if (SysValueUtil.isNotEmpty(expireDate) && !DateUtil.isBiggerThenCurrentDate(expireDate)) {
			accountStatus = isDefaultPass ? SecConstants.ACCOUNT_STATUS_PASSWORD_CHANGE : SecConstants.ACCOUNT_STATUS_PASSWORD_EXPIRED;
		}

		boolean isInitPass = SysValueUtil.isNotEmpty(user.getResetPasswordToken());
		boolean isFirstLogin = 	SysValueUtil.isEmpty(user.getLastSignInAt()) &&
				SysValueUtil.isNotEmpty(user.getCreatorId()) &&
				SysValueUtil.isEmpty(user.getPasswordExpireDate());

		// 비밀번호 초기화 요청 또는 최초 로그인 시.
		if (isInitPass || isFirstLogin || isDefaultPass) {
			return SecConstants.ACCOUNT_STATUS_PASSWORD_CHANGE;
		}

		// 최초 로그인이 아니고, 만료 날짜가 비어 있는 경우 비밀번호 만료 날짜 생성.
		if (!isFirstLogin && SysValueUtil.isEmpty(expireDate)) {
			// 비밀번호 만료 날짜가 비어있는 경우 초기값 설정(오늘 날짜 + 변경 주기)
			String period = SettingUtil.getValue(SysConfigConstants.USER_PASSWORD_CHANGE_PERIOD_DAY, "90");
			expireDate = DateUtil.addDateToStr(new Date(), SysValueUtil.toInteger(period));
			user.setPasswordExpireDate(expireDate);
			this.queryManager.update(user, "passwordExpireDate");
		}

		return accountStatus;
	}
}