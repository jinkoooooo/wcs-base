/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.anyware.wcs.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import xyz.anythings.sys.util.AnyValueUtil;
import xyz.anyware.wcs.query.store.VueQueryStore;
import xyz.elidom.base.rest.ResourceController;
import xyz.elidom.core.entity.Code;
import xyz.elidom.core.entity.CodeDetail;
import xyz.elidom.core.rest.CodeController;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sec.SecConfigConstants;
import xyz.elidom.sec.SecConstants;
import xyz.elidom.sec.rest.LoginHistoryController;
import xyz.elidom.sec.system.auth.AuthenticationService;
import xyz.elidom.sec.util.SecurityUtil;
import xyz.elidom.sys.SysConfigConstants;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.system.auth.AuthProviderFactory;
import xyz.elidom.sys.system.service.AbstractRestService;
import xyz.elidom.sys.util.DateUtil;
import xyz.elidom.sys.util.MessageUtil;
import xyz.elidom.sys.util.SessionUtil;
import xyz.elidom.sys.util.SettingUtil;
import xyz.elidom.sys.util.ThrowUtil;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ValueUtil;

@RestController
@Transactional
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/vue")
@ServiceDesc(description="Domain Service API")
@Tag(name = "예제 API", description = "Swagger VUE API")
public class VueController extends AbstractRestService {
	/**
	 * 인증 정보 프로바이더
	 */
	@Autowired
	private AuthProviderFactory authProviderFactory;

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

	@Autowired
	private VueQueryStore vueQueryStore;


	@Value("${server.session.timeout:2592000}")
	int sessionTime;

	/**
	 * 로그인 이력 컨트롤러
	 */
	@Autowired
	private LoginHistoryController loginHistoryController;


	@Override
	protected Class<?> entityClass() {
		return null;
	}


	/**
	 * Menu Permission of User SQL
	 */
	private String USER_ROLE_SQL = "select a.name from roles a, users_roles b where a.id = b.role_id and a.domain_id =:domainId and b.user_id =:userId";



	class EmptyObject implements Serializable {

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

	@SuppressWarnings("rawtypes")
	@PostMapping(value="/vue_menus", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Search user menus by user authorization")
	@Operation(hidden = true)
	public List<Map<String,Object>>  vueMenus(HttpServletRequest req, @RequestBody Map<String,String> param) {

		User currentUser = SecurityUtil.getUser();
		Domain domain = (Domain)SessionUtil.getAttribute(SysConstants.CURRENT_DOMAIN);
		String locale = currentUser.getLocale();

		Map<String, Object> params = ValueUtil.newMap("domainId", domain.getId());

		if(AnyValueUtil.isNotEmpty(req.getHeader("x-locale"))) {
			locale = req.getHeader("x-locale");
		}
		String sqlPermission = this.vueQueryStore.getSystemRouterMenuQuery();

		List<Map> listPermission = this.queryManager.selectListBySql(sqlPermission, params, Map.class, 0, 0);

		List<Map<String,Object>> permission =new ArrayList<>();
		String checkParent = "";
		int index = -1;
		for (Map item : listPermission) {
			String parent_name =item.get("parent_name").toString();
			if (!checkParent.equals(parent_name)) {
				Map<String, Object> router = AnyValueUtil.newMap("path,name,component,redirect", "/"+item.get("parent_name"), item.get("parent_name"), "LAYOUT", item.get("redirect"));
				List<Map<String,Object>> childrenList =new ArrayList<>();
				String title = MessageUtil.getLocaleTerm(locale, MessageUtil.getMenuTermKey(item.get("name").toString()), item.get("name").toString());
				String partentTitle = MessageUtil.getLocaleTerm(locale, MessageUtil.getMenuTermKey(parent_name), parent_name);

				Map<String, Object> children =new HashMap<>();
				Map<String, Object> parentmeta =new HashMap<>();
				Map<String, Object> meta =new HashMap<>();
				parentmeta.put("title", partentTitle);
				router.put("title",partentTitle);
				parentmeta.put("hideChildrenInMenu", false);
				parentmeta.put("icon",item.get("icon"));
				parentmeta.put("redirect",item.get("redirect"));
				router.put("meta",parentmeta);
				if(ValueUtil.isNotEmpty(item.get("path"))) {
					children.put("path", item.get("path"));
					children.put("name", item.get("name"));
					children.put("hideMenu", item.get("hidemenu"));
					children.put("title", title);
					children.put("currentActiveMenu", item.get("currentactivemenu"));
					children.put("icon", item.get("icon"));
					children.put("component", item.get("component"));
					meta.put("title", title);
					meta.put("icon", item.get("icon"));

					children.put("meta", meta);
					childrenList.add(children);

					router.put("children", childrenList);
				}
				permission.add(router);
				checkParent=item.get("parent_name").toString();
				index++;
			} else {
				Map<String, Object> router = permission.get(index);
				@SuppressWarnings("unchecked")
				List<Map<String,Object>> childrenList= (List<Map<String, Object>>) router.get("children");
				Map<String, Object> children = AnyValueUtil.newMap("name" ,item.get("name"));
				Map<String, Object> meta =new HashMap<>();
				String title = MessageUtil.getLocaleTerm(locale, MessageUtil.getMenuTermKey(item.get("name").toString()), item.get("name").toString());

				meta.put("title", title);
				meta.put("hideMenu", item.get("hidemenu"));
				meta.put("icon",item.get("icon"));

				children.put("currentActiveMenu",item.get("currentactivemenu"));
				children.put("path",item.get("path"));
				children.put("name",item.get("name"));
				children.put("hideMenu",item.get("hidemenu"));
				children.put("title",item.get("title"));
				children.put("component",item.get("component"));
				children.put("icon",item.get("icon"));
				children.put("meta",meta);
				childrenList.add(children);
				router.put("children",childrenList);
				permission.set(index,router);
			}
		}
		return permission;
	}

	/**
	 * 매니저 화면 로그인2
	 * @param req
	 * @param res
	 * @param param
	 * @return Map<String, Object>
	 */
	@PostMapping(value = "/vue_login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Login Manager")
	@Operation(
			summary = "Login Manager",
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
					required = true,
					content = @Content(
							mediaType = "application/json",
							schema = @Schema(
									type = "object",
									example = "{ \"username\": \"admin\", \"password\": \"admin\" , \"domain\": \"7\"}"
							)
					)
			)
	)
	public Map<String, Object> vueLogin(
			HttpServletRequest req,
			HttpServletResponse res,
			@RequestBody Map<String,String> param){

		String login = param.get("username");
		String password = param.get("password");
		String domainId = param.get("domain");

		Domain domain = null;

		// operator 로그인은 domainId 필수
		if(AnyValueUtil.isNotEmpty(domainId)) {
			domain = Domain.find(Long.parseLong(domainId));
			SecurityContextHolder.clearContext();
		} else {
			// 클라이언트 접근 URL로 부터 도메인을 조회
			domain = this.findDomainByRequest(req);
		}

		// 로그인 정보로 사용자 정보 체크
		String ipAddr = AnyValueUtil.getRemoteIp(req);
		User user = this.getUserForAuth(domain, login, ipAddr);

		// 사용자 정보가 유효한 지 체크
		Map<String, Object> userInfo = this.checkValidAccount(domain, user, password, ipAddr);
		String accountStatus = (String)userInfo.get("status");

		// 계정 상태가 OK가 아니면 사용자 정보 리턴
		if(AnyValueUtil.isNotEqual(accountStatus, SysConstants.OK_STRING)) {
			return userInfo;
			// 계정 상태가 OK이면 인증 실행
		} else {
			// 세션에 도메인, 접속 IP 설정
			SessionUtil.setAttribute(SecConstants.CURRENT_DOMAIN, domain);
			SessionUtil.setAttribute("ACCESS_IP", ipAddr);

			// operator 로그인은 domainId 필수
			if(AnyValueUtil.isNotEmpty(domainId)) {
				SessionUtil.setAttribute("CLIENT_TYPE", "MOBILE");
			} else {
				SessionUtil.setAttribute("CLIENT_TYPE", "MANAGER");
			}

			// 인증 실행
			this.authenticationService.doAuthenticate(req, res, user.getId(), password);

			// Token 생성
			Date iat = new Date();
			Calendar c = Calendar.getInstance();
			c.setTime(iat);
			c.add(Calendar.SECOND, this.sessionTime);
			Date exp = c.getTime();
			String tokenStr = this.createToken(domain, user, iat, exp);
			Cookie token = new Cookie("access_token", tokenStr);
			token.setMaxAge(this.sessionTime);
			token.setPath("/"); // 모든 경로에서 접근 가능 하도록 설정
			res.addCookie(token);

			String locale = user.getLocale();
			if(AnyValueUtil.isNotEmpty(req.getHeader("x-locale"))) {
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

			// 세션 정보 리턴
			Map<String, Object> sessionInfo = this.currentSession(user, domain);
			sessionInfo.put(SecConstants.ACCOUNT_STATUS, accountStatus);

			int code =0;
			String type="success";
			String message="ok";
			Map<String, Object> result =AnyValueUtil.newMap("userInfo",user);

			return AnyValueUtil.newMap("code,type,message,result,token", code,type,message,result,tokenStr);
		}
	}



	@SuppressWarnings("unchecked")
	private Map<String, Object> currentSession(User currentUser, Domain domain) {
		return (Map<String, Object>) this.authProviderFactory.getAuthProvider().sessionUserInfo(domain, currentUser);
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
			throw ThrowUtil.newDomainNotExist(AnyValueUtil.getClientRequestSubDomain(req));
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
	 * base64 decoding login parameter
	 * @param login
	 * @return
	 */
	public String decodeLogin(String login) {
		String encodedString = Base64.getEncoder().encodeToString(login.getBytes());
		// 디코딩
		byte[] decodedBytes = Base64.getDecoder().decode(encodedString);
		String decodedString = new String(decodedBytes);
		return decodedString;
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
			validUser = ValueUtil.isEqual(encPasswd, password);

			if(!validUser) {
				String encodePass = SecurityUtil.encodePasswordV2(password);
				validUser = ValueUtil.isEqual(encPasswd, encodePass);
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
			throw new ElidomRuntimeException(MessageUtil.getMessage("ACCOUNT_TEMPORARY_LOCKED","계정이 일시 잠금 상태입니다. {0}분 후에 시도해주세요",ValueUtil.newStringList(SettingUtil.getValue(SecConfigConstants.USER_PASS_LOCK_MINUTE,"0"))));
		}

		// 5. 계정 상태 체크, null이면 OK, 나머지는 추가 프로세스 필요
		String accountStatus = this.isCredentialsNonExpired(user);

		// 6. 계정 정보 및 상태 리턴
		return AnyValueUtil.newMap(
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


	private String createToken(Domain domain, User user, Date iat, Date exp) {
		String SECRET_KEY = "0xD58F835B69D207A76CC5F84a70a1D0d4C79dAC95";

		return Jwts.builder()
				.setHeader(AnyValueUtil.newMap("alg,typ", "HS256","JWT"))
				.claim("id", user.getId())
				.claim("domain", new VueController.EmptyObject())
				.setIssuedAt(iat)
				.setExpiration(exp)
				.setIssuer("hatiolab.com")
				.setSubject("user")
				.signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()), SignatureAlgorithm.HS256)
				.compact();
	}

	/**
	 * 계정이 잠겨 있지 않은지 체크
	 *
	 * @param user
	 * @return
	 */
	private boolean isAccountNonLocked(User user) {
		String expireDate = user.getAccountExpireDate();
		if (AnyValueUtil.isEmpty(expireDate)) {
			return true;
		}

		int lockMinute = AnyValueUtil.toInteger(SettingUtil.getValue(SecConfigConstants.USER_PASS_LOCK_MINUTE), 0);
		if (lockMinute < 1) {
			return true;
		}

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(ValueUtil.toDate(expireDate));
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
		if (!ValueUtil.toBoolean(SettingUtil.getValue(SysConfigConstants.USER_PASSWORD_EXPIRE_ENABLE), false)) {
			return null;
		}

		// 비밀번호 만료 상태 Check
		String accountType = user.getAccountType();

		// 계정 타입이 User가 아니거나, 비어있지 않은 경우 OK
		if (!(ValueUtil.isEmpty(accountType) || ValueUtil.isEqual(accountType, SysConstants.ACCOUNT_TYPE_USER))) {
			return null;
		}

		// 기본 비밀번호와 동일한지 확인.
		boolean isDefaultPass = false;
		String defaultPass = SettingUtil.getValue(SysConfigConstants.SECURITY_INIT_PASS);
		if (ValueUtil.isNotEmpty(defaultPass)) {
			isDefaultPass = ValueUtil.isEqual(user.getEncryptedPassword(), SecurityUtil.encodePassword(defaultPass));
		}

		// 계정 상태
		String accountStatus = null;

		// 만료 날짜가 현재 날짜보다 작을 경우
		if (ValueUtil.isNotEmpty(expireDate) && !DateUtil.isBiggerThenCurrentDate(expireDate)) {
			accountStatus = isDefaultPass ? SecConstants.ACCOUNT_STATUS_PASSWORD_CHANGE : SecConstants.ACCOUNT_STATUS_PASSWORD_EXPIRED;
		}

		boolean isInitPass = ValueUtil.isNotEmpty(user.getResetPasswordToken());
		boolean isFirstLogin = 	ValueUtil.isEmpty(user.getLastSignInAt()) &&
				ValueUtil.isNotEmpty(user.getCreatorId()) &&
				ValueUtil.isEmpty(user.getPasswordExpireDate());

		// 비밀번호 초기화 요청 또는 최초 로그인 시.
		if (isInitPass || isFirstLogin || isDefaultPass) {
			return SecConstants.ACCOUNT_STATUS_PASSWORD_CHANGE;
		}

		// 최초 로그인이 아니고, 만료 날짜가 비어 있는 경우 비밀번호 만료 날짜 생성.
		if (!isFirstLogin && ValueUtil.isEmpty(expireDate)) {
			// 비밀번호 만료 날짜가 비어있는 경우 초기값 설정(오늘 날짜 + 변경 주기)
			String period = SettingUtil.getValue(SysConfigConstants.USER_PASSWORD_CHANGE_PERIOD_DAY, "90");
			expireDate = DateUtil.addDateToStr(new Date(), ValueUtil.toInteger(period));
			user.setPasswordExpireDate(expireDate);
			this.queryManager.update(user, "passwordExpireDate");
		}

		return accountStatus;
	}


	/**
	 * User별 siteList 조회
	 *
	 * @param
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	@PostMapping(value = "/site_list", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Site List")
	@Operation(
			summary = "도메인 목록 조회",
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
					required = true,
					content = @Content(
							mediaType = "application/json",
							schema = @Schema(
									type = "object",
									example = "{ \"userId\": \"admin\" }"
							)
					)
			)
	)
	public List<?> domainList(@RequestBody Map<String,String> param)  {
		// 1. 현재 도메인 & 사용자 추출
		String userId = param.get("userId");
		User currentUser = User.getUserById(userId);

		// 2. 사용자가 방문 권한이 있는 사이트 리스트 조회
		String sql = "select id, name, brand_name, description, subdomain from domains where id in (select domain_id from domain_users where user_id = :userId)";

		if(currentUser.getSuperUser()) {
			sql = "select id, name, brand_name, description, subdomain from domains";
		}

		Map<String, Object> params = ValueUtil.newMap("userId,systemFlag", currentUser.getId(), true);
		List<Map> siteList =  this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);

		// 5. 결과 리턴
		return siteList;
	}



	@PostMapping("/getPermCode")
	@ApiDesc(description="Create Permission")
	@Operation(hidden = true)
	public List<String> create(HttpServletRequest req, @RequestBody Map<String,String> param) {
		User currentUser = SecurityUtil.getUser();
		if (currentUser == null) {
			return null;
		}

		Map<String, Object> paramsMap = AnyValueUtil.newMap("domainId,userId", currentUser.getDomain().getId(), currentUser.getId());
		return this.queryManager.selectListBySql(USER_ROLE_SQL, paramsMap, String.class, 0, 0);
	}

	@GetMapping(value="/getUserInfo", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Find one from UsersRole by id")
	@Operation(hidden = true)
	public Map<String,Object> findOne() {
		User currentUser = SecurityUtil.getUser();
		if (currentUser == null) {
			return null;
		}

		Map<String, Object> paramsMap = AnyValueUtil.newMap("domainId,userId", currentUser.getDomain().getId(), currentUser.getId());
		List<String> rolesFromDb = queryManager.selectListBySql(USER_ROLE_SQL, paramsMap, String.class, 0, 0);
		List<String> roles = (rolesFromDb == null) ? new ArrayList<String>() : new ArrayList<String>(rolesFromDb);

		// users.super_user=true 인 계정은 frontend RoleEnum.SUPER('super') 권한을 자동 부여.
		// users_roles 매핑 row가 없어도 admin/super 사용자가 권한 게이팅을 통과하도록 보정한다.
		if (Boolean.TRUE.equals(currentUser.getSuperUser()) && !roles.contains("super")) {
			roles.add("super");
		}

		Map<String,Object> userInfo = new HashMap<String,Object>();
		userInfo.put("username",currentUser.getName());
		userInfo.put("realName",currentUser.getName());
		userInfo.put("avatar","");
		userInfo.put("desc",currentUser.getAccountType());
		userInfo.put("roles",roles);
		return userInfo;
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


	@PostMapping("/columns")
	@ApiDesc(description = "column")
	@Operation(hidden = true)
	public List<Map<String,Object>> findMenuColumn(HttpServletRequest req, HttpServletResponse res,  @RequestBody Map<String,String> param){

		User currentUser = SecurityUtil.getUser();
		String locale = currentUser.getLocale();

		if(AnyValueUtil.isNotEmpty(req.getHeader("x-locale"))) {
			locale = req.getHeader("x-locale");
		}

		String MENU_COLUMN_SQL = vueQueryStore.getMenuColumnQuery();
		@SuppressWarnings("rawtypes")
		List<Map> listMenuColumns= this.queryManager.selectListBySql(MENU_COLUMN_SQL, param, Map.class, 0, 0);;

		List<Map<String,Object>> menuColumnList =new ArrayList<>();
		for (@SuppressWarnings("rawtypes") Map menuColumn : listMenuColumns) {
			Map<String, Object> router = AnyValueUtil.newMap("dataIndex,width,align,ifShow", menuColumn.get("name"), menuColumn.get("grid_width"), menuColumn.get("grid_align"), menuColumn.get("if_show"));
			String title = MessageUtil.getLocaleTerm(locale,menuColumn.get("term").toString());
			router.put("title",title);
			router.put("edit",false);
			menuColumnList.add(router);
		}
		return menuColumnList;
	}

	@SuppressWarnings("rawtypes")
	@PostMapping("/toastColumns")
	@ApiDesc(description = "tostColumns")
	@Operation(hidden = true)
	public List<Map<String,Object>> findTostMenuColumn(HttpServletRequest req, HttpServletResponse res,  @RequestBody Map<String,String> param){

		User currentUser = SecurityUtil.getUser();
		String locale = currentUser.getLocale();

		ResourceController entityCtrl = BeanUtil.get(ResourceController.class);
		CodeController codeCtrl = BeanUtil.get(CodeController.class);

		if(AnyValueUtil.isNotEmpty(req.getHeader("x-locale"))) {
			locale = req.getHeader("x-locale");
		}

		String TOAST_COLUMN_SQL = vueQueryStore.getToastMenuColumnQuery();
		List<Map> listMenuColumns= this.queryManager.selectListBySql(TOAST_COLUMN_SQL, param, Map.class, 0, 0);;

		List<Map<String,Object>> menuColumnList =new ArrayList<>();
		for (Map menuColumn : listMenuColumns) {
			Map<String, Object> router = AnyValueUtil.newMap("name,align,minWidth,hidden", menuColumn.get("name"),menuColumn.get("grid_align"),menuColumn.get("grid_width"),menuColumn.get("if_show"));
			String title = MessageUtil.getLocaleTerm(locale,menuColumn.get("term").toString());
			router.put("header",title);
			String  grid_editor = menuColumn.get("grid_editor").toString();
			if(grid_editor.equals("text")){
				router.put("editor", "text");
			} else if(grid_editor.equals("code-combo")){
				Code code = codeCtrl.findOne(SysConstants.SHOW_BY_NAME_METHOD, menuColumn.get("ref_name").toString());
				List<CodeDetail> optionList= code.getItems();
				List<Map<String,Object>> options = new ArrayList<>();
				for(CodeDetail codeItem : optionList) {
					Map<String, Object> item = new HashMap<>();
					item.put("text",codeItem.getDescription());
					item.put("value",codeItem.getName());
					options.add(item);
				}
				Map<String, Object> listItems = AnyValueUtil.newMap("listItems", options);

				Map<String, Object> editor = AnyValueUtil.newMap("type,options", "select", listItems);

				router.put("formatter", "listItemText");
				router.put("editor", editor);

			}
			else if(grid_editor.equals("resource-code")){
				List<CodeDetail> optionList = entityCtrl.searchResourceDataAsCode(menuColumn.get("ref_name").toString());
				List<Map<String,Object>> options = new ArrayList<>();
				for(CodeDetail codeItem : optionList) {
					Map<String, Object> item = new HashMap<>();
					item.put("text",codeItem.getDescription());
					item.put("value",codeItem.getName());
					options.add(item);
				}
				Map<String, Object> listItems = AnyValueUtil.newMap("listItems", options);

				Map<String, Object> editor = AnyValueUtil.newMap("type,options", "select", listItems);
				router.put("formatter", "listItemText");
				router.put("editor", editor);

			}else if(grid_editor.equals("date-picker")){
				Map<String, Object> options = new HashMap<>();
				options.put("type","datePicker");
				options.put("options",ValueUtil.newMap("format","yyyy/MM/dd"));

				router.put("editor",options);
			}else if(grid_editor.equals("readonly")){}
			else if(grid_editor.equals("boolean")) {
				Map<String, Object> editor = AnyValueUtil.newMap("type", "boolean");
				router.put("renderer", editor);
			}
			else{
				router.put("editor", "text");
			}

			menuColumnList.add(router);
		}

		return menuColumnList;

	}

	@SuppressWarnings("rawtypes")
	@PostMapping("/searchs")
	@ApiDesc(description = "searchs")
	@Operation(hidden = true)
	public List<Map<String,Object>> findMenuSearch(HttpServletRequest req, HttpServletResponse res,  @RequestBody Map<String,String> param){
		User currentUser = SecurityUtil.getUser();
		String locale = currentUser.getLocale();
		ResourceController entityCtrl = BeanUtil.get(ResourceController.class);
		CodeController codeCtrl = BeanUtil.get(CodeController.class);
		if(AnyValueUtil.isNotEmpty(req.getHeader("x-locale"))) {
			locale = req.getHeader("x-locale");
		}

		String SEARCH_SQL = vueQueryStore.getSearchColumnQuery();
		List<Map> listMenuColumns= this.queryManager.selectListBySql(SEARCH_SQL, param, Map.class, 0, 0);;

		List<Map<String,Object>> menuSearchList =new ArrayList<>();
		for (Map menuColumn : listMenuColumns) {
			if(AnyValueUtil.isNotEmpty(menuColumn.get("search_editor")))  {
				String title = MessageUtil.getLocaleTerm(locale,menuColumn.get("term").toString());
				String  search_editor = menuColumn.get("search_editor").toString();
				Map<String, Object> search = AnyValueUtil.newMap("field,label", menuColumn.get("name"), title);
				if(search_editor.equals("text")){
					search.put("component", "Input");
				}else if(search_editor.equals("resource-selector")){
					search.put("component", "Input");
				}
				else if(search_editor.equals("code-combo")){
					search.put("component", "Select");
					Code code = codeCtrl.findOne(SysConstants.SHOW_BY_NAME_METHOD, menuColumn.get("ref_name").toString());
					List<CodeDetail> optionList= code.getItems();
					List<Map<String,Object>> option = new ArrayList<>();
					for(CodeDetail codeItem : optionList) {
						Map<String, Object> item = new HashMap<>();
						item.put("label",codeItem.getDescription());
						item.put("value",codeItem.getName());
						option.add(item);
					}
					Map<String, Object> options = AnyValueUtil.newMap("options", option);

					search.put("componentProps", options);

				}
				else if(search_editor.equals("resource-code")){
					search.put("component", "Select");
					List<CodeDetail> optionList = entityCtrl.searchResourceDataAsCode(menuColumn.get("ref_name").toString());
					List<Map<String,Object>> option = new ArrayList<>();
					for(CodeDetail codeItem : optionList) {
						Map<String, Object> item = new HashMap<>();
						item.put("label",codeItem.getDescription());
						item.put("value",codeItem.getName());
						option.add(item);
					}
					Map<String, Object> options = AnyValueUtil.newMap("options", option);

					search.put("componentProps", options);


				}else if(search_editor.equals("date-picker")){
					search.put("component", "DatePicker");
				}else if(search_editor.equals("switch")){
					search.put("component", "Switch");
				}else if(search_editor.equals("tristate-radio")){
					search.put("component", "RadioGroup");
					List<Map<String, Object>> options = new ArrayList<Map<String, Object>>(3);
					options.add(AnyValueUtil.newMap("label,value", "No", "no"));
					options.add(AnyValueUtil.newMap("label,value", "Yes", "yes"));
					options.add(AnyValueUtil.newMap("label,value", "All", "all"));
					search.put("componentProps", AnyValueUtil.newMap("options", options));
				}else{
					search.put("component", "Input");
				}
				String operator = menuColumn.get("search_oper").toString();
				search.put("operator",operator);
				search.put("resource_url", menuColumn.get("resource_url").toString());
				search.put("colProps",AnyValueUtil.newMap("span",8));
				menuSearchList.add(search);
			}
		}

		return menuSearchList;
	}


	@SuppressWarnings("rawtypes")
	@PostMapping("/forms")
	@ApiDesc(description = "forms")
	@Operation(hidden = true)
	public List<Map<String,Object>> findMenuForm(HttpServletRequest req, HttpServletResponse res,  @RequestBody Map<String,String> param){
		User currentUser = SecurityUtil.getUser();
		String locale = currentUser.getLocale();
		CodeController codeCtrl = BeanUtil.get(CodeController.class);

		if(AnyValueUtil.isNotEmpty(req.getHeader("x-locale"))) {
			locale = req.getHeader("x-locale");
		}
		String FORM_SQL = vueQueryStore.getFormColumnQuery();
		List<Map> listMenuColumns= this.queryManager.selectListBySql(FORM_SQL, param, Map.class, 0, 0);;

		List<Map<String,Object>> menuSearchList =new ArrayList<>();
		for (Map menuColumn : listMenuColumns) {
			if(AnyValueUtil.isNotEmpty(menuColumn.get("search_editor")))  {
				String title = MessageUtil.getLocaleTerm(locale,menuColumn.get("term").toString());
				String  form_editor = menuColumn.get("search_editor").toString();
				Map<String, Object> search = AnyValueUtil.newMap("field,label", menuColumn.get("name"), title);
				if(form_editor.equals("text")){
					search.put("component", "Input");
				}else if(form_editor.equals("resource-selector")){
					search.put("component", "Input");
				}
				else if(form_editor.equals("code-combo")){
					search.put("component", "Select");
					Code code = codeCtrl.findOne(SysConstants.SHOW_BY_NAME_METHOD, menuColumn.get("ref_name").toString());
					List<CodeDetail> optionList= code.getItems();
					List<Map<String,Object>> option = new ArrayList<>();
					for(CodeDetail codeItem : optionList) {
						Map<String, Object> item = new HashMap<>();
						item.put("label",codeItem.getDescription());
						item.put("value",codeItem.getName());
						option.add(item);
					}
					Map<String, Object> options = AnyValueUtil.newMap("options", option);

					search.put("componentProps", options);
				}
				else if(form_editor.equals("resource")){
					search.put("component", "Input");
				}else if(form_editor.equals("date-picker")){
					search.put("component", "DatePicker");
				}else if(form_editor.equals("switch")){
					search.put("component", "Switch");
				}else if(form_editor.equals("hidden")) {
					search.put("component", "Input");
					search.put("show", false);
				}else {
					search.put("component", "Input");
				}

				search.put("colProps",AnyValueUtil.newMap("span",8));
				menuSearchList.add(search);
			}
		}

		return menuSearchList;
	}


	@SuppressWarnings("rawtypes")
	@PostMapping(value="/vue_menus_list", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Search user menus by user authorization")
	@Operation(hidden = true)
	public List<Map<String,Object>>  vueMenusList(HttpServletRequest req, @RequestBody Map<String,String> param) {

		User currentUser = SecurityUtil.getUser();
		Domain domain = (Domain)SessionUtil.getAttribute(SysConstants.CURRENT_DOMAIN);
		String locale = currentUser.getLocale();

		Map<String, Object> params = ValueUtil.newMap("domainId", domain.getId());

		if(AnyValueUtil.isNotEmpty(req.getHeader("x-locale"))) {
			locale = req.getHeader("x-locale");
		}


		String sqlPermission = vueQueryStore.getSystemMenuListQuery();

		List<Map> listPermission = this.queryManager.selectListBySql(sqlPermission, params, Map.class, 0, 0);

		List<Map<String,Object>> menuList =new ArrayList<>();
		String checkParent = "";
		int index = -1;
		for (Map item : listPermission) {
			String parent_name =item.get("parent_name").toString();
			if (!checkParent.equals(parent_name)) {
				Map<String, Object> router = AnyValueUtil.newMap("id,template,name,icon_path,rank",item.get("p_id"), "/"+item.get("parent_name"), item.get("parent_name"), item.get("icon_path"), item.get("parent_rank"));
				List<Map<String,Object>> childrenList =new ArrayList<>();
				String partentTitle = MessageUtil.getLocaleTerm(locale, MessageUtil.getMenuTermKey(parent_name), parent_name);
				router.put("title", partentTitle);
				Map<String, Object> children =new HashMap<>();

				if(ValueUtil.isNotEmpty(item.get("template"))) {
					String title = MessageUtil.getLocaleTerm(locale, MessageUtil.getMenuTermKey(item.get("name").toString()), item.get("name").toString());

					children.put("id", item.get("id"));
					children.put("name", item.get("name"));
					children.put("rank", item.get("rank"));
					children.put("parent_id", item.get("parent_id"));
					children.put("template", item.get("template"));
					children.put("routing", item.get("routing"));
					children.put("title", title);
					children.put("icon_path", item.get("icon_path"));
					children.put("hidden_flag", item.get("hidden_flag"));

					childrenList.add(children);

					router.put("children", childrenList);
				}
				menuList.add(router);
				checkParent=item.get("parent_name").toString();
				index++;
			} else {
				Map<String, Object> router = menuList.get(index);
				@SuppressWarnings("unchecked")
				List<Map<String,Object>> childrenList= (List<Map<String, Object>>) router.get("children");
				Map<String, Object> children = AnyValueUtil.newMap("name" ,item.get("name"));

				String title = MessageUtil.getLocaleTerm(locale, MessageUtil.getMenuTermKey(item.get("name").toString()), item.get("name").toString());
				children.put("id", item.get("id"));
				children.put("name", item.get("name"));
				children.put("rank", item.get("rank"));
				children.put("parent_id", item.get("parent_id"));
				children.put("template", item.get("template"));
				children.put("routing", item.get("routing"));
				children.put("title", title);
				children.put("icon_path", item.get("icon_path"));
				children.put("hidden_flag", item.get("hidden_flag"));
				childrenList.add(children);
				router.put("children",childrenList);
				menuList.set(index,router);
			}
		}
		return menuList;
	}

	@SuppressWarnings("rawtypes")
	@PostMapping(value="/parentMenu", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Search user menus by user authorization")
	@Operation(hidden = true)
	public List<Map>  parentMenu(HttpServletRequest req, @RequestBody Map<String,String> param) {
		Domain domain = (Domain)SessionUtil.getAttribute(SysConstants.CURRENT_DOMAIN);

		Map<String, Object> params = ValueUtil.newMap("domainId", domain.getId());

		String sqlParintMenu = "select id,name from vue_menus  where domain_id =:domainId and (parent_id is null or parent_id='') order by rank ";

		List<Map> parentMenuList = this.queryManager.selectListBySql(sqlParintMenu, params, Map.class, 0, 0);

		return parentMenuList;
	}

	@PostMapping(value="/menuSave", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(hidden = true)
	public String create(@RequestBody Map<String, Object>  input) {
		Domain domain = (Domain)SessionUtil.getAttribute(SysConstants.CURRENT_DOMAIN);
		input.put("domain_id",domain.getId());
		if(!input.containsKey("parent_id")) {
			input.put("parent_id","");
		}

		if(!input.containsKey("category")) {
			input.put("category","STANDARD");
		}
		String hidden_flag = input.get("hidden_flag").toString();
		if("0".equals(hidden_flag))
			input.put("hidden_flag",true);
		else
			input.put("hidden_flag",false);


		String vueMenuInsertSql = "INSERT INTO vue_menus (id, name, parent_id, template, category, rank, icon_path, hidden_flag, routing,domain_id) VALUES(uuid_generate_v4(), :name, :parent_id, :template, :category, :rank, :icon_path, :hidden_flag, :routing,:domain_id)";
		this.queryManager.executeBySql(vueMenuInsertSql, input);
		return  "ok";
	}

	@PutMapping(value = "/menuSave/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Update")
	@Operation(hidden = true)
	public String update(@PathVariable("id") String id, @RequestBody Map<String, Object> input) {
		String vueMenuUPDATESql = "update vue_menus SET  name=:name, parent_id=:parent_id, template=:template, rank=:rank, icon_path=:icon_path, hidden_flag=:hidden_flag, routing=:routing   WHERE id=:id";

		String hidden_flag = input.get("hidden_flag").toString();
		if("0".equals(hidden_flag))
			input.put("hidden_flag",true);
		else
			input.put("hidden_flag",false);

		this.queryManager.executeBySql(vueMenuUPDATESql, input);
		return  "ok";
	}


	@PutMapping(value = "/menuDelete/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Delete")
	@Operation(hidden = true)
	public String menuDelete(@PathVariable("id") String id ) {
		String vueMenuDeleteSql = "delete from vue_menus WHERE (id=:id or parent_id =:id)";

		Map<String, Object> params = ValueUtil.newMap("id", id);
		this.queryManager.executeBySql(vueMenuDeleteSql, params);
		return  "ok";
	}
}