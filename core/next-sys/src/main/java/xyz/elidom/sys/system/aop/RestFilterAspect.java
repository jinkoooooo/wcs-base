/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.beans.factory.annotation.Autowired;

import xyz.elidom.sys.model.ServiceLogInfo;
import xyz.elidom.sys.system.controller.handler.filter.IAfterHandlerFilterChain;
import xyz.elidom.sys.system.controller.handler.filter.IBeforeHandlerFilterChain;
import xyz.elidom.sys.util.ExceptionUtil;
import xyz.elidom.util.FormatUtil;
import xyz.elidom.util.ValueUtil;

/**
 * 클라이언트의 RESTful 서비스 요청을 받아 처리하는 서비스 핸들러에 대한 Filtering Aspect
 * 
 * @author shortstop
 */
public class RestFilterAspect {

	@Autowired
	private IBeforeHandlerFilterChain beforeFilterChain;

	@Autowired
	private IAfterHandlerFilterChain afterFilterChain;

	@Autowired
	private RestServiceLogger restServiceLogger;

	/**
	 * 서비스 핸들러가 실행되기 전 Filtering 과정을 수행한다.
	 * 
	 * @param joinPoint
	 */
	public Object filterAroundAccess(ProceedingJoinPoint joinPoint) throws Throwable {

		// Return 값 선언
		Object retVal = null;

		// 수행 여부
		boolean goingOn = true;

		// 1. Before Filter Chain 수행
		Object val = this.beforeFilterChain.doFilters(joinPoint, joinPoint.getArgs());
		if (val != null && val instanceof Boolean) {
			goingOn = (Boolean) val;
		}

		if (goingOn) {
			// 2. JoinPoint 수행
			ServiceLogInfo serviceInfo = restServiceLogger.getServiceLogInfo(joinPoint);

			try {
				retVal = joinPoint.proceed();
			} catch (Exception e) {
				if (serviceInfo != null) {
					retVal = ExceptionUtil.getErrorStackTraceToString(e);
				}
				throw e;
			} finally {
				if (serviceInfo != null && ValueUtil.isNotEmpty(retVal)) {
					String resultValue = retVal instanceof String ? ValueUtil.toString(retVal) : FormatUtil.toJsonString(retVal);
					serviceInfo.setResultValue(resultValue);

					// Service 실행 로그 출력.
					restServiceLogger.doPrint(serviceInfo);
				}
			}
		}

		// 3. After Filter Chain 수행, joinPoint 예외 발생시 스킵
		if (goingOn) {
			retVal = this.afterFilterChain.doFilters(joinPoint, retVal, joinPoint.getArgs());
		}

		return retVal;
	}
}