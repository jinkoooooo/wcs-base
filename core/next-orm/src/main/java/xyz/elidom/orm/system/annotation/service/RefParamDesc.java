/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.orm.system.annotation.service;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface RefParamDesc {

	/**
	 * Parameter name
	 * 
	 * @return
	 */
	String name();
		
	/**
	 * Reference Parameter type (class name) 
	 * Required if type is 'object'
	 * 
	 * @return
	 */
	String refType();	
}
