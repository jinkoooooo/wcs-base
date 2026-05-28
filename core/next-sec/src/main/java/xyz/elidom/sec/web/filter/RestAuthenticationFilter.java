/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sec.web.filter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import xyz.elidom.exception.server.ElidomServiceException;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sec.SecConfigConstants;
import xyz.elidom.sec.SecConstants;
import xyz.elidom.sec.system.auth.AuthenticationService;
import xyz.elidom.sec.util.SecurityUtil;
import xyz.elidom.sys.SysConfigConstants;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.SysMessageConstants;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.system.context.DomainContext;
import xyz.elidom.sys.system.service.params.ErrorOutput;
import xyz.elidom.sys.util.EnvUtil;
import xyz.elidom.sys.util.MessageUtil;
import xyz.elidom.sys.util.SessionUtil;
import xyz.elidom.sys.util.SettingUtil;
import xyz.elidom.util.FormatUtil;
import xyz.elidom.util.ValueUtil;

@Order(2)
@Service
@WebFilter(urlPatterns = { "/*" })
public class RestAuthenticationFilter extends AbstractSecurityWebApplicationInitializer implements Filter {
	/**
	 * logger
	 */
	protected Logger logger = LoggerFactory.getLogger(RestAuthenticationFilter.class);
	/**
	 * IQueryManager
	 */
	@Autowired
	private IQueryManager queryMgr;
	/**
	 * 인증 서비스
	 */
	@Autowired
	private AuthenticationService authSvc;

	/**
	 * restful 기본 URL - '/rest'
	 */
	private static final String BASE_URL = "/rest";

	/**
	 * restful 기본 URL - '/rest'
	 */
	private static final String SWAGGER_URL = "/swagger-ui";

	/**
	 * 헤더 키 x-locale
	 */
	private static final String HEADER_LOCALE = "x-locale";
	/**
	 * Content Type - 'application/json; charset=UTF-8'
	 */
	private static final String CONTENT_TYPE_JSON_UTF_8 = "application/json; charset=UTF-8";
	/**
	 * 인증 없이 들어올 수 있는 URL 리스트 - key : 도메인 ID, value : permitAllUrls
	 */
	private Map<Long, Set<String>> permitAllUrls = new ConcurrentHashMap<Long, Set<String>>();
	/**
	 * GET 방식만 인증 없이 들어올 수 있는 URL 리스트 - key : 도메인 ID, value : permitReadOnlyUrls
	 */
	private Map<Long, Set<String>> permitReadOnlyUrls = new ConcurrentHashMap<Long, Set<String>>();
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		String uri = request.getRequestURI();
		String method = request.getMethod();
		
		// 클라이언트 접근 도메인으로 현재 도메인 정보 설정
		Domain currentDomain = null;
		
		if(uri.startsWith(BASE_URL)  ) {
			// 미들웨어 서비스간 http 리퀘스트를 이용해 각각의 server에 명령을 내려야 하는 경우 domain_id를 찾지못해 문제 발생. Rabbitmq 서비스는 시스템 도메인으로 통일
	        List<String> keywords = List.of("rest/rabbitmq", "rest/vue", "/rest/provision");

	        boolean containsKeyword = keywords.stream().anyMatch(uri::contains);
	        //기본도메인이 없을 경우 sys domain을 기본 도메인으로
			currentDomain = containsKeyword ? Domain.systemDomain() : SessionUtil.setCurrentDomain();
			DomainContext.setCurrentDomain(currentDomain);
//			if(currentDomain == null) {
//				currentDomain = uri.contains("rest/vue") ? Domain.systemDomain() : SessionUtil.setCurrentDomain();
//			}

			// 도메인 정보가 없으면 사용할 수 없는 도메인
			if(currentDomain == null) {
				throw new ElidomServiceException("DOMAIN_NOT_FOUND", MessageUtil.getMessage("UNAVAILABLE_SITE"), ValueUtil.newStringList(SessionUtil.getCurrentReqeustDomainClass())); 
			}
		}
		
		// 메소드가 OPTION이 아닌 경우
		if (uri.startsWith(BASE_URL) && !RequestMethod.OPTIONS.name().equalsIgnoreCase(method)) {
			// 인증되지 않은 Session에 대한 처리
			if (SecurityUtil.isAnonymous()) {
				// GET 방식이 아니거나, Read Permit URL에 포함되어 있지 않을 경우 인증 확인
				if (!(RequestMethod.GET.name().equalsIgnoreCase(method))) {
					
					this.doFilterByAuth(request, response);
					
					// 인증되지 않았을 경우 메시지 처리
//					if (SecurityUtil.isAnonymous() || SecurityUtil.getAuthentication() == null) {
//						this.processUnauthorized(request, response);
//						return;
//					}
				}
			}

			Object locale = ValueUtil.checkValue(request.getHeader(HEADER_LOCALE), SettingUtil.getValue(currentDomain.getId(), SysConfigConstants.DEFAULT_LOCALE, SysConstants.EN_US));
			SessionUtil.setAttribute(SysConstants.LOCALE, locale);
		}

		// Swagger 경우
		if(uri.startsWith(SWAGGER_URL) || uri.startsWith("/v3/api-docs")) {
			// 임시적으로 생략 가능
			chain.doFilter(req, res);
			return;
		}

		chain.doFilter(req, res);
	}

	/**
	 * 인증이 안 된 경우 처리
	 * 
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	private void processUnauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ErrorOutput output = new ErrorOutput();
		output.setCode(SysMessageConstants.NOT_AUTHORIZED_USER);
		output.setMsg(MessageUtil.getMessage(SysMessageConstants.NOT_AUTHORIZED_USER, "Unauthorized user"));
		output.setSuccess(false);
		output.setStatus(HttpStatus.UNAUTHORIZED.value());

		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setCharacterEncoding(SysConstants.CHAR_SET_UTF8);
		response.setContentType(CONTENT_TYPE_JSON_UTF_8);

		PrintWriter writer = response.getWriter();
		writer.println(FormatUtil.toJsonString(output));
	}

	/**
	 * 인증을 실행하지 않고 호출 할 수 있는 읽기 전용 기본 URL 설정.
	 */
	private Set<String> getDefaultReadOnlyPermitURL() {
		Set<String> permitUrls = new HashSet<String>();
		permitUrls.add("/rest/download/public");
		permitUrls.add("/rest/publishers");
		return permitUrls;
	}
	
	/**
	 * 기본 인증 방식일 때 필터 처리
	 * 
	 * @param request
	 * @param response
	 * @throws IOException
	 * @throws ServletException
	 */
	private void doFilterByAuth(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		// 1. HTTP 헤더에 타입, 키를 추출
		String authType = request.getHeader(SecConstants.AUTH_TYPE);
		String authKey = request.getHeader(SecConstants.AUTH_KEY);

		// 2. HttpSender를 통한 JSON 호출 시 Type에 따른 인증 실행
		if (ValueUtil.isNotEmpty(authType) && ValueUtil.isNotEmpty(authKey)) {
			switch (authType) {
				case SecConstants.AUTH_TYPE_JSON :
					this.doJsonAuth(authKey, request, response);
					break;
	
				case SecConstants.AUTH_TYPE_TOKEN :
					this.doTokenAuth(authKey, request, response);
					break;
			}
		}
	}
	
	/**
	 * JSON 호출에 대한 인증
	 * 
	 * @param authKey
	 * @param request
	 * @param response
	 * @return
	 */
	private void doJsonAuth(String authKey, HttpServletRequest request, HttpServletResponse response) {
		String authJsonValue = new String(Base64.decodeBase64(authKey.getBytes()));
		User user = FormatUtil.underScoreJsonToObject(authJsonValue, User.class);
		String userId = ValueUtil.checkValue(user.getLogin(), user.getEmail());
		this.doAuthencate(request, response, userId, user.getEncryptedPassword(), SecConstants.AUTH_TYPE, SecConstants.AUTH_TYPE_JSON);
	}

	/**
	 * Token 방식에 대한 인증
	 * 
	 * @param authKey
	 * @param request
	 * @param response
	 */
	private void doTokenAuth(String authKey, HttpServletRequest request, HttpServletResponse response) {
		User user = this.queryMgr.select(User.class, authKey);
		
		if (ValueUtil.isNotEmpty(user)) {
			this.doAuthencate(request, response, user.getId(), user.getEncryptedPassword(), SecConstants.AUTH_TYPE, SecConstants.AUTH_TYPE_TOKEN);
		}
	}

	
	/**
	 * 인증 처리
	 * 
	 * @param request
	 * @param response
	 * @param userId
	 * @param password
	 * @param authType
	 * @param authValue
	 */
	private void doAuthencate(HttpServletRequest request, HttpServletResponse response, String userId, String password, String authType, String authValue) {
		Authentication authentication = this.authSvc.doAuthenticate(request, response, userId, password);
		SecurityContextHolder.getContext().setAuthentication(authentication);
		SessionUtil.setAttribute(authType, authValue);
	}
}