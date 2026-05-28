/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.controller.handler.filter;

import org.aspectj.lang.JoinPoint;

/**
 * 서비스 핸들러에서 호출 타겟(서비스 클래스의 메소드)를 호출하기 전 수행할 Before Filter Chain을 구성한다.
 * 
 * @author shortstop
 */
public interface IBeforeHandlerFilterChain {
	
	/**
	 * Filter Chain에 필터를 추가한다.
	 * 
	 * @param filter
	 */
	public void addFilter(IBeforeHandlerFilter filter);
	
	/**
	 * Filter Chain에 존재하는 모든 필터들의 필터링을 수행한다.
	 * 
	 * @param joinPoint
	 * @param args
	 * @return
	 */
	public Object doFilters(JoinPoint joinPoint, Object... args);
	
}
