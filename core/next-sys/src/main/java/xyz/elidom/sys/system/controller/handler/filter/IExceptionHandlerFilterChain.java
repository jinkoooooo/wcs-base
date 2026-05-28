/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.controller.handler.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 서비스 핸들러에서 호출 타겟(서비스 클래스의 메소드)를 수행하다가 예외가 발생할 경우 수행할 Exception Filter Chain을 구성한다.
 * 
 * @author shortstop
 */
public interface IExceptionHandlerFilterChain {
	
	/**
	 * Filter가 존재하는지 체크 
	 * 
	 * @return
	 */
	public boolean isExistFilter();
	
	/**
	 * Filter Chain에 필터를 추가한다.
	 * 
	 * @param filter
	 */
	public void addFilter(IExceptionHandlerFilter filter);
	
	/**
	 * Filter Chain에 존재하는 모든 필터들의 필터링을 수행한다.
	 * 
	 * @param req
	 * @param res
	 * @param ex
	 */
	public void doFilters(HttpServletRequest req, HttpServletResponse res, Throwable ex);
	
}
