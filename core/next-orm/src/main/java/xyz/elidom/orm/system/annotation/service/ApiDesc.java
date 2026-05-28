/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.orm.system.annotation.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Service API (Service Bean의 Method) Descriptor
 * 
 * @author shortstop
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiDesc {

	/**
	 * TODO 제거 
	 * (Optional) Service Method Name
	 * 
	 * @return
	 */
	String name() default "";
	
	/**
	 * (Optional) Service Method Description
	 * 
	 * @return
	 */
	String description() default "";
	
	/**
	 * TODO 제거 
	 * Service URL Type - collection or member
	 *  member 타입이면 URL의 마지막에 {id}가 들어가게 된다.
	 *  
	 * @return
	 */
	String urlType() default "collection"; 
	
	/**
	 * TODO 제거 
	 * HTTP Method
	 * 
	 * @return
	 */
	String method() default "GET";
	
	/**
	 * (Optional) Input Parameter 중에서 타입을 알 수 없는 Object type의 Input에 대한 클래스 명을 기술 
	 *
	 * @return The Input Paramemeter Descriptor
	 */
	RefParamDesc[] inputs() default {};
	
	/**
	 * (Optional) Output Parameter 중에서 타입을 알 수 없는 Object type의 Input에 대한 클래스 명을 기술
	 *
	 * @return The Output Paramemeter Descriptor
	 */
	RefParamDesc[] outputs() default {};
	
}
