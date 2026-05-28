/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.controller.handler.filter.after;

import org.aspectj.lang.JoinPoint;
import org.springframework.stereotype.Component;

import xyz.elidom.sys.system.controller.handler.filter.IAfterHandlerFilter;
import xyz.elidom.sys.system.service.params.ErrorOutput;

/**
 * 애플리케이션 운영시 서버 셧다운 예고를 위한 After 필터  
 * 
 * @author shortstop
 */
@Component
public class ShutdownNoticeFilter implements IAfterHandlerFilter {

	@Override
	public Object doFilter(JoinPoint joinPoint, Object retVal, Object... args) {
		ErrorOutput output = new ErrorOutput(500, "MAINT-001", "System will be shutdown soon, Please Wait a moment!", "Not An Error");
		return output;
	}

}
