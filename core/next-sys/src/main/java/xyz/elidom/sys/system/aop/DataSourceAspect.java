package xyz.elidom.sys.system.aop;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import xyz.elidom.sys.system.config.CustomRoutingDataSource;
import xyz.elidom.util.ValueUtil;

@Aspect
@Component
@Order(1)
public class DataSourceAspect {

	@Value("${transation.use-readonly-replica:}")
    private String useReadonlyReplica;
	
	@Value("${transation.readonly-exclude-pattern:}")
    private String readonlyExcludePattern;

    private static final Logger log = LoggerFactory.getLogger(DataSourceAspect.class);

    @Pointcut("execution(* *..*Controller.*(..))")
    public void controllerMethods() {};

    @Around("controllerMethods()")
    public Object setDataSource(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();  // java.lang.reflect.Method 사용
        Class<?> declaringClass = method.getDeclaringClass(); //class level에서의 설정

        // 메서드 레벨과 클래스 레벨에서 @Transactional 확인
        Transactional transactional = method.getAnnotation(Transactional.class); //메서드 레벨
        if (transactional == null) {
            transactional = declaringClass.getAnnotation(Transactional.class); //클래스 레벨
        }

        try {
            if (ValueUtil.isEqualIgnoreCase(useReadonlyReplica, "true") && (transactional.readOnly() || isReadOnlyPattern())) {
                CustomRoutingDataSource.setDataSourceKey("READ");  // Read Replica
            } else {
                CustomRoutingDataSource.setDataSourceKey("WRITE");  // Write Master
            }	
            return joinPoint.proceed();
        }
        catch (Throwable t) {
            log.error("[DataSourceAspect] Error in method: {} | Message: {}", method.getName(), t.getMessage(), t);
            throw t;
        }
        finally {
            CustomRoutingDataSource.clearDataSourceKey();
        }
    }

    
    public boolean isReadOnlyPattern() {
        // HTTP 요청이 아닌 경우 null일 수 있음
        if (RequestContextHolder.getRequestAttributes() == null) {
            return false;  // HTTP 요청이 아닌 경우 READ로 처리하지 않음
        }
        
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String requestUri = request.getRequestURI();
        String httpMethod = request.getMethod();

        // GET 메서드 && /rest/{대상명} or /rest/{대상명}/{id} 패턴일 경우 READ 처리
        boolean isReadOnly = httpMethod.equalsIgnoreCase("GET") && !requestUri.matches(readonlyExcludePattern);
        
		return isReadOnly;
    }
}