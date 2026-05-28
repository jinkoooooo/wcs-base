/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.orm.system.annotation.entity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Service API (Service Bean의 Method) Descriptor
 * 
 * @author shortstop
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RefInclude {

	/**
	 * (Required) 현재 Entity에 매핑된 컬럼명 
	 * 
	 * @return
	 */
	String column();
	
	/**
	 * (Required) Reference Entity 
	 * 
	 * @return
	 */
	Class<?> refEntity();
	
	/**
	 * (Optional) Reference Field Id
	 * 
	 * @return
	 */
	String refId() default "id";
	
	/**
	 * (Required) Reference Entity Select Fields - comma separated
	 * 
	 * @return
	 */
	String selectColumns();	

}
