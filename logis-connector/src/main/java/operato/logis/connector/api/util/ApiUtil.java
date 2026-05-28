package operato.logis.connector.api.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.UnsupportedEncodingException;

public class ApiUtil {

    /**
     * 프록시 환경을 고려한 IP 추출 유틸리티
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) { ip = request.getHeader("Proxy-Client-IP"); }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) { ip = request.getHeader("WL-Proxy-Client-IP"); }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) { ip = request.getHeader("HTTP_CLIENT_IP"); }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) { ip = request.getHeader("HTTP_X_FORWARDED_FOR"); }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) { ip = request.getRemoteAddr(); }

        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 캐싱된 Request에서 Body 데이터를 문자열로 추출하는 유틸리티
     */
    public static String getRequestBody(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper) {
            ContentCachingRequestWrapper wrapper = (ContentCachingRequestWrapper) request;

            // 컨트롤러 메서드에 진입한 시점에는 이미 Spring이 @RequestBody 바인딩을 위해
            // 스트림을 한 번 읽었기 때문에, 캐시(ByteArray)에 데이터가 들어있습니다.
            byte[] buf = wrapper.getContentAsByteArray();

            if (buf.length > 0) {
                try {
                    return new String(buf, 0, buf.length, wrapper.getCharacterEncoding());
                } catch (UnsupportedEncodingException e) {
                    return "Encoding Error";
                }
            }
        }
        return "No Body or Not Wrapped";
    }
}