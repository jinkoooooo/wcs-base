/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.controller.handler.filter.before;

import org.aspectj.lang.JoinPoint;
import org.springframework.stereotype.Component;

import xyz.elidom.sys.system.controller.handler.filter.IBeforeHandlerFilter;

/**
 * Handler 실행을 중지하는 Before 필터 
 *  
 * @author shortstop
 */
@Component
public class BlockExecutionFilter implements IBeforeHandlerFilter {

	@Override
	public Object doFilter(JoinPoint joinPoint, Object... args) {
		return false;
	}

}
