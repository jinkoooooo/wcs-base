/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.controller.handler.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 서비스 핸들러에서 호출 타겟(서비스 클래스의 메소드)를 호출하던 중 예외가 발생할 경우 핸들링할 필터 
 * 
 * @author shortstop
 */
public interface IExceptionHandlerFilter {

	/**
	 * 필터링을 수행한다. 
	 * 
	 * @param req
	 * @param res
	 * @param ex
	 */
	public void doFilter(HttpServletRequest req, HttpServletResponse res, Throwable ex);
	
}
