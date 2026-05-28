package xyz.elidom.dbist.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LmsUserActivityLog {

    String description() default "";    // 사용자 활동 설명
    boolean isLogin() default true;     // 로그인 여부
    boolean ignore() default false;     // 로그 저장 예외 여부
    String[] maskFields() default {};   // 마스킹할 필드명 목록 (snake_case)
}