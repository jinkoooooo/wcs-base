/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.params;

import xyz.elidom.sys.system.service.params.conds.IPageInput;
import xyz.elidom.sys.system.service.params.conds.IPageOutput;

/**
 * Pagination 서비스를 위한 Input & Output
 * 
 * @author shortstop
 */
public class PageInOut extends ListInOut implements IPageInput, IPageOutput {

	/**
	 * 현재 페이지 - Input
	 */
	private int page;
	/**
	 * 페이지 당 표시할 레코드 수 - Input
	 */
	private int limit;
	/**
	 * Skip할 레코드 수 - Input
	 */
	private long start;
	
	/**
	 * total count - Output
	 */
	private long total;
	/**
	 * total page - Output
	 */
	private int totalPage;	

	@Override
	public int getPage() {
		return page;
	}

	@Override
	public void setPage(int page) {
		this.page = page;
	}

	@Override
	public int getLimit() {
		return limit;
	}

	@Override
	public void setLimit(int limit) {
		this.limit = limit;
	}

	@Override
	public long getStart() {
		return start;
	}

	@Override
	public void setStart(long start) {
		this.start = start;
	}
	
	@Override
	public long getTotal() {
		return total;
	}

	@Override
	public void setTotal(long total) {
		this.total = total;
	}

	@Override
	public int getTotalPage() {
		return totalPage;
	}

	@Override
	public void setTotalPage(int totalPage) {
		this.totalPage = totalPage;
	}
	
}
