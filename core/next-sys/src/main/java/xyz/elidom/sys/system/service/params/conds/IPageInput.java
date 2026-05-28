/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.params.conds;

/**
 * 검색 결과를 Pagination하여 리턴하는 서비스를 위한 Input
 * 
 * @author shortstop
 */
public interface IPageInput extends IListInput {

	/**
	 * Get 현재 페이지 
	 * 
	 * @return
	 */
	public int getPage();

	/**
	 * Set 현재 페이지
	 * 
	 * @param page
	 */
	public void setPage(int page);

	/**
	 * Get 페이지 당 표시할 레코드 개수 
	 * 
	 * @return
	 */
	public int getLimit();

	/**
	 * Set 페이지 당 표시할 레코드 개수
	 * 
	 * @param limit
	 */
	public void setLimit(int limit);

	/**
	 * Get Skip할 레코드 수
	 * 
	 * @return
	 */
	public long getStart();

	/**
	 * Set Skip할 레코드 수
	 * 
	 * @param start
	 */
	public void setStart(long start);
	
}
