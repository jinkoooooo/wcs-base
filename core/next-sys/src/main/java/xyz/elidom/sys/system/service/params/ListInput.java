/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.params;

import java.util.ArrayList;
import java.util.List;

import xyz.elidom.sys.system.service.params.conds.IListInput;
import xyz.elidom.sys.system.service.params.conds.SearchCondition;
import xyz.elidom.sys.system.service.params.conds.SortCondition;

/**
 * 조회 조건을 넘겨주고 결과를 Pagination 하지 않는 서비스를 위한 Input
 * 
 * @author shortstop
 */
public class ListInput implements IListInput {

	/**
	 * 검색 필드 - comma separated 
	 */
	private String selectFields;
	/**
	 * 검색 조건 리스트 
	 */
	private List<SearchCondition> queryList;
	/**
	 * 소팅 조건 리스트
	 */
	private List<SortCondition> sortList;

	@Override
	public String getSelectFields() {
		return this.selectFields;
	}

	@Override
	public void setSelectFields(String selectFields) {
		this.selectFields = selectFields;
	}
	
	@Override
	public List<SearchCondition> getQueryList() {
		return queryList;
	}

	@Override
	public void setQueryList(List<SearchCondition> queryList) {
		this.queryList = queryList;
	}
	
	@Override
	public void addQueryCondition(SearchCondition query) {
		if(this.queryList == null) {
			this.queryList = new ArrayList<SearchCondition>();
		}
		
		this.queryList.add(query);
	}	

	@Override
	public List<SortCondition> getSortList() {
		return sortList;
	}

	@Override
	public void setSortList(List<SortCondition> sortList) {
		this.sortList = sortList;
	}
	
	@Override
	public void addSortCondition(SortCondition sort) {
		if(this.sortList == null) {
			this.sortList = new ArrayList<SortCondition>();
		}
		
		this.sortList.add(sort);
	}

}
