/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.controller.handler.filter.before;

import java.util.ArrayList;
import java.util.List;

import org.aspectj.lang.JoinPoint;
import org.springframework.stereotype.Component;

import xyz.elidom.sys.system.controller.handler.filter.IBeforeHandlerFilter;
import xyz.elidom.sys.system.controller.handler.filter.IBeforeHandlerFilterChain;

/**
 * 기본 Before Handler Filter Chain
 * 
 * @author shortstop
 */
@Component
public class BeforeHandlerFilterChain implements IBeforeHandlerFilterChain {

	/**
	 * Handler Filter Chain
	 */
	protected List<IBeforeHandlerFilter> filterList = new ArrayList<IBeforeHandlerFilter>();
	
	@Override
	public void addFilter(IBeforeHandlerFilter filter) {
		this.filterList.add(filter);
	}
	
	@Override
	public Object doFilters(JoinPoint joinPoint, Object... args) {
		Object retVal = null;
		
		for(int i = 0 ; i < this.filterList.size() ; i++) {
			retVal = this.filterList.get(i).doFilter(joinPoint, args);
		}
		
		return retVal;
	}

}
