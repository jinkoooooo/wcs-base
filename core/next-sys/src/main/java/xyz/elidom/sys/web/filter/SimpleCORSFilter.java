/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.web.filter;

/**
 * CORS 크로스 도메인 이슈 조치를 위한 Filter (No 'Access-Control-Allow-Origin' header is present on the requested resource)
 * 
 * @author Minu.Kim
 */
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.util.SysValueUtil;
import xyz.elidom.util.BeanUtil;

@Order(1)
@Service("simpleCorsFilter")
@WebFilter(urlPatterns = { "/*" }, description = "CORS Filter")
public class SimpleCORSFilter implements Filter {

	@Autowired
	Environment env;

	/**
	 * CORS Check URLs
	 */
	private List<String> corsCheckUrls = new ArrayList<String>();
	/**
	 * restful 기본 URL - '/rest'
	 */
	private static final String BASE_URL = "/rest";
	/**
	 * admin 기본 URL - '/admin/api/applications'
	 */
	private static final String ADMIN_API_URL = "/admin/api/applications";
	/**
	 * 헤더 키 Access-Control-Allow-Origin
	 */
	private static final String HEADER_ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
	/**
	 * 헤더 키 Access-Control-Allow-Credentials
	 */
	private static final String HEADER_ACCESS_CONTROL_ALLOW_CREDENTIAL = "Access-Control-Allow-Credentials";
	/**
	 * 헤더 키 Access-Control-Allow-Methods
	 */
	private static final String HEADER_ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
	/**
	 * 헤더 키 Access-Control-Max-Age
	 */
	private static final String HEADER_ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
	/**
	 * 헤더 키 Access-Control-Allow-Headers
	 */
	private static final String HEADER_ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
	/**
	 * Access-Control-Allow-Methods 기본 값
	 */
	private static final String VALUE_ACCESS_CONTROL_ALLOW_METHODS = "POST, PUT, GET, OPTIONS, DELETE";
	/**
	 * Access-Control-Max-Age 기본 값
	 */
	private static final String VALUE_ACCESS_CONTROL_MAX_AGE = "3600000000";
	/**
	 * Access-Control-Allow-Headers 기본 값
	 */
	private static final String VALUE_ACCESS_CONTROL_ALLOW_HEADERS = "Content-Type,Access-Control-Allow-Headers,Authorization,X-Requested-With,X-Content-Type-Options,X-Frame-Options,X-XSS-Protection,X-Locale,X-Domain-Id,Authorization-Type,Authorization-Key";
	/**
	 * 헤더 키 Origin
	 */
	private static final String HEADER_ORIGIN = "Origin";

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		String uri = request.getRequestURI();

		if (this.isCheckURL(uri)) {
			response.setHeader(HEADER_ACCESS_CONTROL_ALLOW_CREDENTIAL, SysConstants.TRUE_STRING);
			response.setHeader(HEADER_ACCESS_CONTROL_ALLOW_METHODS, VALUE_ACCESS_CONTROL_ALLOW_METHODS);
			response.setHeader(HEADER_ACCESS_CONTROL_MAX_AGE, VALUE_ACCESS_CONTROL_MAX_AGE);
			response.setHeader(HEADER_ACCESS_CONTROL_ALLOW_HEADERS, VALUE_ACCESS_CONTROL_ALLOW_HEADERS);

			String origin = request.getHeader(HEADER_ORIGIN);
			if (!SysValueUtil.isEmpty(origin) && SysValueUtil.isEmpty(response.getHeader(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN))) {
				response.setHeader(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, origin);
			}
		}

		chain.doFilter(req, res);
	}

	/**
	 * url이 CORS 체크를 해야하는 URL인지 체크
	 * 
	 * @param url
	 * @return
	 */
	private boolean isCheckURL(String url) {
		for (String checkUrl : this.corsCheckUrls) {
			if (url.startsWith(checkUrl)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		if (env == null)
			env = BeanUtil.get(Environment.class);

		String contextPath = env.getProperty("server.servlet.context-path", "");
		StringBuilder baseUrl = new StringBuilder(contextPath).append(SimpleCORSFilter.BASE_URL);

		this.corsCheckUrls.add(baseUrl.toString());
		this.corsCheckUrls.add(SimpleCORSFilter.ADMIN_API_URL);
	}

	@Override
	public void destroy() {
	}
}