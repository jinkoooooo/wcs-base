/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.params;

import xyz.elidom.sys.system.service.params.conds.IPageInput;

/**
 * 검색 결과를 Pagination하여 리턴하는 서비스를 위한 Input
 * 
 * @author shortstop
 */
public class PageInput extends ListInput implements IPageInput {

	/**
	 * 현재 페이지
	 */
	private int page;
	/**
	 * 페이지 당 표시할 레코드 수
	 */
	private int limit;
	/**
	 * Skip할 레코드 수
	 */
	private long start;

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

}
