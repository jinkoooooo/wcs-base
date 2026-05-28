/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.params.conds;

/**
 * 검색 결과를 Pagination하여 리턴하는 서비스를 위한 Output
 * 
 * @author shortstop
 */
public interface IPageOutput extends IListOutput {

	/**
	 * Get Total Record Count
	 * 
	 * @return
	 */
	public long getTotal();

	/**
	 * Set Total Record Count
	 * 
	 * @param total
	 */
	public void setTotal(long total);

	/**
	 * Get Total Page Count
	 * 
	 * @return
	 */
	public int getTotalPage();

	/**
	 * Set Total Record Count
	 * 
	 * @param totalPage
	 */
	public void setTotalPage(int totalPage);
	
}
