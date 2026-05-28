package operato.logis.lms.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import operato.logis.lms.dto.hist.AccessSysLogDto;
import operato.logis.lms.service.impl.hist.UserLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import xyz.anythings.sys.util.AnyValueUtil;
import xyz.elidom.dbist.annotation.LmsUserActivityLog;
import xyz.elidom.exception.ElidomException;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.util.SessionUtil;
import xyz.elidom.sys.web.HttpRequestWrapper;
import xyz.elidom.util.FormatUtil;

import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 사용자 활동 로그 수집 Aspect
 */
@Aspect
@Component
public class UserLogAspect {

    @Autowired
    private UserLogService userLogService;

    @Autowired
    private ObjectMapper objectMapper;

    // 로그 크기 제한
    private static final int MAX_LOG_SIZE = 4000;
    private static final String MASK_VALUE = "***";

    /**
     * @LmsUserActivityLog 가 붙은 메서드 호출 시 maskFields 필드 마스킹 후 이력 저장
     * 1. 로그기록 예외 제외
     * 2. 응답 시작 시간 측정
     * 3. 요청 데이터 추출 및 마스킹
     * 4. 실제 메서드 실행 후 응답 데이터 추출 및 마스킹
     * 5. 로그 요청/응답 필드 길이 제한
     * 6. 로그 큐에 추가
     */
    @Around("@annotation(lmsUserActivityLog)")
    public Object logUserActivity(ProceedingJoinPoint joinPoint, LmsUserActivityLog lmsUserActivityLog) throws Throwable {
        if (lmsUserActivityLog.ignore()) {
            return joinPoint.proceed();
        }

        long start = System.currentTimeMillis();

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        String reqJson = null;                          // 요청내용(JSON)
        String resJson = null;                          // 응답결과(JSON)
        Object response;                                // 응답결과
        Integer resStatus = HttpServletResponse.SC_OK;  // 응답상태 (기본값: 200)
        String actType = request.getMethod();           // 요청유형
        String userAgent = request.getHeader("User-Agent");
        String accessIp = AnyValueUtil.getRemoteIp(request);

        Logger logger = LoggerFactory.getLogger(joinPoint.getTarget().getClass());

        Map<String, Object> params = new HashMap<>();
        params.put("requestBody", convertStrToJson(this.getRequestBody(request)));
        params.put("queryParams", this.getRequestParams(request));
        params.put("pathVariables", this.extractPathVariables(joinPoint));
        reqJson = new GsonBuilder().setPrettyPrinting().create().toJson(params);
        reqJson = maskJsonFields(reqJson, lmsUserActivityLog.maskFields());

        try {
            response = joinPoint.proceed();

            // 응답결과 JSON 변환 및 마스킹
            resJson = FormatUtil.toJsonString(response);
            resJson = maskJsonFields(resJson, lmsUserActivityLog.maskFields());

            if (resJson != null && resJson.length() > MAX_LOG_SIZE) {
                logger.info("logUserActivity - Response exceeds limit: {} chars", resJson.length());
                resJson = resJson.substring(0, MAX_LOG_SIZE - 3) + "...";
            }
            if (reqJson != null && reqJson.length() > MAX_LOG_SIZE) {
                logger.info("logUserActivity - Request exceeds limit: {} chars", reqJson.length());
                reqJson = reqJson.substring(0, MAX_LOG_SIZE - 3) + "...";
            }

        } catch (Exception e) {
            resJson = createErrorResponse(e);
            resStatus = resolveHttpStatus(e);
            logger.error("logUserActivity [ERROR] - error: {}", e.getMessage());
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - start;
            String reqUri = request.getRequestURI();
            int serverPort = request.getServerPort();
            String userId = "-";
            String sessionId = "-";

            if (lmsUserActivityLog.isLogin()) {
                sessionId = SessionUtil.getSessionId();
                try {
                    userId = User.currentUser().getId();
                } catch (Exception e) {
                    logger.warn("User ID 조회 실패: {}", e.getMessage());
                }
            }

            this.userLogService.saveUserActHistory(new AccessSysLogDto(lmsUserActivityLog, userId, sessionId, reqUri, actType, reqJson, resJson, resStatus, serverPort, duration, accessIp, userAgent));
            logger.info("logUserActivity completed - duration: {}ms, reqUri: {}", duration, reqUri);
        }
        return response;
    }

    /**
     * 예외 유형에 따른 HTTP 상태 코드 반환
     */
    private int resolveHttpStatus(Exception e) {
        if (e instanceof ElidomException) {
            return ((ElidomException) e).getStatus();
        } else if (e instanceof AuthenticationException) {
            return HttpServletResponse.SC_UNAUTHORIZED;             // 401
        } else if (e instanceof AccessDeniedException) {
            return HttpServletResponse.SC_FORBIDDEN;                // 403
        } else if (e instanceof MethodArgumentNotValidException
                || e instanceof IllegalArgumentException) {
            return HttpServletResponse.SC_BAD_REQUEST;              // 400
        } else if (e instanceof HttpRequestMethodNotSupportedException) {
            return HttpServletResponse.SC_METHOD_NOT_ALLOWED;       // 405
        } else {
            return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;    // 500
        }
    }

    /**
     * 에러 응답 JSON 생성
     */
    private String createErrorResponse(Exception e) {
        try {
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("error", e.getClass().getSimpleName());
            errorInfo.put("message", e.getMessage());
            errorInfo.put("timestamp", new Date());
            return objectMapper.writeValueAsString(errorInfo);
        } catch (Exception ex) {
            return "{\"error\":\"로그 생성 실패\",\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * Request Body 추출
     */
    private String getRequestBody(HttpServletRequest request) {
        if (request instanceof HttpRequestWrapper) {
            byte[] rawBody = ((HttpRequestWrapper) request).getRawData();
            if (rawBody != null && rawBody.length > 0) {
                return new String(rawBody, StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    /**
     * Query Parameter 추출
     */
    private Map<String, Object> getRequestParams(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        request.getParameterMap().forEach((key, value) ->
                result.put(key, value.length == 1 ? value[0] : value));
        return result;
    }

    /**
     * Path Variable 추출
     */
    private Map<String, Object> extractPathVariables(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        Map<String, Object> pathVariables = new HashMap<>();

        Annotation[][] paramAnnotations = signature.getMethod().getParameterAnnotations();
        for (int i = 0; i < paramAnnotations.length; i++) {
            for (Annotation annotation : paramAnnotations[i]) {
                if (annotation instanceof PathVariable) {
                    String paramName = ((PathVariable) annotation).value();
                    pathVariables.put(paramName.isEmpty() ? parameterNames[i] : paramName, args[i]);
                }
            }
        }
        return pathVariables;
    }

    /**
     * 문자열을 JsonElement로 변환 (파싱 실패 시 빈 JsonObject 반환)
     */
    private JsonElement convertStrToJson(String baseStr) {
        try {
            return JsonParser.parseString(baseStr);
        } catch (Exception e) {
            return new JsonObject();
        }
    }

    /**
     * JSON 문자열에서 지정한 필드를 마스킹 처리 (MASK_VALUE 로 대체, 중첩 구조 포함)
     *
     * @param json         마스킹할 JSON 문자열
     * @param fieldsToMask 마스킹 대상 필드명 목록
     * @return 마스킹된 JSON 문자열
     */
    private String maskJsonFields(String json, String[] fieldsToMask) {
        if (json == null || fieldsToMask == null || fieldsToMask.length == 0) return json;
        try {
            Set<String> maskSet = new HashSet<>(Arrays.asList(fieldsToMask));
            JsonElement element = JsonParser.parseString(json);
            maskJsonElement(element, maskSet);
            return new GsonBuilder().setPrettyPrinting().create().toJson(element);
        } catch (Exception e) {
            return json;
        }
    }

    /**
     * JsonElement를 재귀 순회하며 지정된 필드를 마스킹 처리
     *
     * @param element 순회할 JsonElement
     * @param maskSet 마스킹 대상 필드명 Set
     */
    private void maskJsonElement(JsonElement element, Set<String> maskSet) {
        if (element == null || element.isJsonNull() || element.isJsonPrimitive()) return;
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (String key : new HashSet<>(obj.keySet())) {
                if (maskSet.contains(key)) {
                    obj.addProperty(key, MASK_VALUE);
                } else {
                    maskJsonElement(obj.get(key), maskSet);
                }
            }
        } else if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(item -> maskJsonElement(item, maskSet));
        }
    }
}