package operato.logis.wcs.common.service.audit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import xyz.elidom.sys.entity.User;
import xyz.elidom.util.ValueUtil;

/**
 * HTTP 요청의 행위자를 ActorContext 에 태깅.
 * ECS M2M 콜백 경로는 ECS, 인증 운영자는 USER 로 설정한다.
 * preHandle 에서 set, afterCompletion 에서 clear.
 */
@Component
public class AuditActorInterceptor implements HandlerInterceptor {

    private static final String ECS_CALLBACK_PREFIX = "/rest/wcs/ecs-callback";

    /** 요청 진입 시 행위자 결정. */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // ECS 콜백은 세션이 없으므로 ECS 행위자로 태깅
        if (request.getRequestURI().contains(ECS_CALLBACK_PREFIX)) {
            ActorContext.set(ActorContext.ecs());
            return true;
        }
        // 인증된 운영자면 USER 행위자로 태깅 (미인증은 미설정 → SYSTEM 폴백)
        User user = currentUserOrNull();
        if (ValueUtil.isNotEmpty(user) && ValueUtil.isNotEmpty(user.getId())) {
            ActorContext.set(ActorContext.user(user.getId(), user.getName()));
        }
        return true;
    }

    /** 요청 종료 시 컨텍스트 정리. */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        ActorContext.clear();
    }

    /** 현재 사용자 추출. 미인증·예외 시 null. */
    private User currentUserOrNull() {
        try {
            return User.currentUser();
        } catch (Throwable t) {
            return null;
        }
    }
}
