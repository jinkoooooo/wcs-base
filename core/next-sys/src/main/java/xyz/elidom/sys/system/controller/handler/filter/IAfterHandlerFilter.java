/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.controller.handler.filter;

import org.aspectj.lang.JoinPoint;

/**
 * 서비스 핸들러에서 호출 타겟(서비스 클래스의 메소드)를 호출한 후에 수행될 필터 정의 - 메소드 수행 후 리턴값을 핸들링 할 수 있다.
 * 
 * @author shortstop
 */
public interface IAfterHandlerFilter {
	
	/**
	 * 필터링을 수행한다.
	 * 
	 * @param joinPoint
	 * @param retVal
	 * @param args
	 * @return
	 */
	public Object doFilter(JoinPoint joinPoint, Object retVal, Object... args);
}
