package operato.logis.wcs.config;

import operato.logis.wcs.common.service.audit.AuditActorInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WCS MVC 설정.
 * 감사 행위자 인터셉터를 WCS REST 경로에 등록한다.
 */
@Configuration
public class WcsWebMvcConfig implements WebMvcConfigurer {

    private final AuditActorInterceptor auditActorInterceptor;

    public WcsWebMvcConfig(AuditActorInterceptor auditActorInterceptor) {
        this.auditActorInterceptor = auditActorInterceptor;
    }

    /** WCS REST 경로에 행위자 인터셉터 적용. */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(auditActorInterceptor).addPathPatterns("/rest/wcs/**");
    }
}
