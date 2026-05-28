/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.orm.system.annotation.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Service Group에 대한 Descriptor
 * 
 * @author shortstop
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceDesc {

	/**
	 * TODO 제거 
	 * Module Name
	 * 
	 * @return
	 */
	String module() default "";
	
	/**
	 * TODO 제거 
	 * Service Name
	 * 
	 * @return
	 */
	String name() default "";
	
	/**
	 * Service Description
	 * 
	 * @return
	 */
	String description();
	
}
