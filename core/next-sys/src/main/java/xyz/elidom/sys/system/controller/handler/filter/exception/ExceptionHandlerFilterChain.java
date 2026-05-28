/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.controller.handler.filter.exception;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import xyz.elidom.sys.system.controller.handler.filter.IExceptionHandlerFilter;
import xyz.elidom.sys.system.controller.handler.filter.IExceptionHandlerFilterChain;

/**
 * 기본 Exception Handler Filter Chain
 * 
 * @author shortstop
 */
@Component
public class ExceptionHandlerFilterChain implements IExceptionHandlerFilterChain {

	/**
	 * Handler Filter Chain
	 */
	protected List<IExceptionHandlerFilter> filterList = new ArrayList<IExceptionHandlerFilter>();
	
	@Override
	public boolean isExistFilter() {
		return !this.filterList.isEmpty();
	}

	@Override
	public void addFilter(IExceptionHandlerFilter filter) {
		this.filterList.add(filter);
	}

	@Override
	public void doFilters(HttpServletRequest req, HttpServletResponse res, Throwable ex) {
		for(int i = 0 ; i < this.filterList.size() ; i++) {
			IExceptionHandlerFilter filter = this.filterList.get(i);
			filter.doFilter(req, res, ex);
		}
	}
}