package operato.logis.connector.api.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

@Component
public class RequestCachingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException, ServletException {

        // 들어오는 요청을 캐싱 래퍼로 감쌉니다.
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);

        // 래핑된 요청을 다음 필터 및 컨트롤러로 전달합니다.
        filterChain.doFilter(wrappedRequest, response);
    }
}