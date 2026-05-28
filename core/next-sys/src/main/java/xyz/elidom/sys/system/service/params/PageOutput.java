/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.params;

import xyz.elidom.sys.system.service.params.conds.IPageOutput;

/**
 * 검색 결과를 Pagination하여 리턴하는 서비스를 위한 Output
 * 
 * @author shortstop
 */
public class PageOutput extends ListOutput implements IPageOutput {

	/**
	 * total count
	 */
	private long total;
	/**
	 * total page
	 */
	private int totalPage;

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
