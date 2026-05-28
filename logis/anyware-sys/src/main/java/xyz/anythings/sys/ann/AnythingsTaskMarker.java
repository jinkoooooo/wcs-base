package xyz.anythings.sys.ann;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

/**
 * 단위 태스크 (단위 함수들의 조합) 마커
 * 
 * @author shortstop
 */
@Component
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AnythingsTaskMarker {
}
