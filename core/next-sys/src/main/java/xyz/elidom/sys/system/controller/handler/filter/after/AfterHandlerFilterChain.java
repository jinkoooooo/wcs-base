/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.controller.handler.filter.after;

import java.util.ArrayList;
import java.util.List;

import org.aspectj.lang.JoinPoint;
import org.springframework.stereotype.Component;

import xyz.elidom.sys.system.controller.handler.filter.IAfterHandlerFilter;
import xyz.elidom.sys.system.controller.handler.filter.IAfterHandlerFilterChain;

/**
 * 기본 After Handler Filter Chain
 * 
 * @author shortstop
 */
@Component
public class AfterHandlerFilterChain implements IAfterHandlerFilterChain {
	/**
	 * Handler Filter Chain
	 */
	protected List<IAfterHandlerFilter> filterList = new ArrayList<IAfterHandlerFilter>();
	
	@Override
	public void addFilter(IAfterHandlerFilter filter) {
		this.filterList.add(filter);
	}
	
	@Override
	public Object doFilters(JoinPoint joinPoint, Object retVal, Object... args) {
		for(int i = 0 ; i < this.filterList.size() ; i++) {
			retVal = this.filterList.get(i).doFilter(joinPoint, retVal, args);
		}
		
		return retVal;
	}
}
