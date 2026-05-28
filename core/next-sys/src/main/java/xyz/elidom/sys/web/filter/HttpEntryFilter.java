package xyz.elidom.sys.web.filter;

import java.io.IOException;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import xyz.elidom.sys.web.HttpRequestWrapper;

@Order(2)
@Service
public class HttpEntryFilter implements Filter {

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;

		/*
			multipart/form-data 요청은 절대 wrapping 하지 않는다.
			HttpRequestWrapper 가 생성자에서 InputStream 을 끝까지 읽어 byte[] 로 캐싱하는데,
			Spring 의 multipart 파싱(StandardServletMultipartResolver → Tomcat native parts)은
			원본 request 의 InputStream 을 직접 사용한다. wrapper 의 getInputStream override 로는
			Tomcat native parser 를 우회할 수 없으므로 parts 가 모두 사라져 @RequestParam("file")
			바인딩이 항상 실패한다.
		 */
		String contentType = request.getContentType();
		if (contentType != null && contentType.toLowerCase().startsWith("multipart/")) {
			chain.doFilter(req, res);
			return;
		}

		chain.doFilter(new HttpRequestWrapper(request), res);
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void destroy() {
	}
}