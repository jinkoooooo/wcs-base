/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.params.conds;

import java.util.List;

/**
 * 조회 조건을 넘겨주고 결과를 Pagination 하지 않는 서비스를 위한 Input
 * 
 * @author shortstop
 */
public interface IListInput {
	
	/**
	 * Get 조회 필드 - comma separated 
	 * 
	 * @return
	 */
	public String getSelectFields();
	
	/**
	 * Set 조회 필드 - comma separated 
	 * 
	 * @param selectFields
	 */
	public void setSelectFields(String selectFields);

	/**
	 * Get 조회 조건 리스트 
	 * 
	 * @return
	 */
	public List<SearchCondition> getQueryList();

	/**
	 * Set 조회 조건 리스트
	 * 
	 * @param queryList
	 */
	public void setQueryList(List<SearchCondition> queryList);
	
	/**
	 * 조회 조건 추가 
	 * 
	 * @param query
	 */
	public void addQueryCondition(SearchCondition query);

	/**
	 * Get 소팅 조건 리스트
	 * 
	 * @return
	 */
	public List<SortCondition> getSortList();

	/**
	 * Set 소팅 조건 리스트
	 * 
	 * @param sortList
	 */
	public void setSortList(List<SortCondition> sortList);
	
	/**
	 * 소팅 조건 추가  
	 * 
	 * @param sort
	 */
	public void addSortCondition(SortCondition sort);
	
}
