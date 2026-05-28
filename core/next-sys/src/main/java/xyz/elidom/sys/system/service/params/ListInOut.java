/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.params;

import java.util.ArrayList;
import java.util.List;

import xyz.elidom.sys.system.service.params.conds.IListInput;
import xyz.elidom.sys.system.service.params.conds.IListOutput;
import xyz.elidom.sys.system.service.params.conds.SearchCondition;
import xyz.elidom.sys.system.service.params.conds.SortCondition;

/**
 * 조회 조건을 넘겨주고 결과를 Pagination 하지 않는 리스트로 받는 서비스를 위한 Input & Output
 * 
 * @author shortstop
 */
public class ListInOut extends BasicOutput implements IListInput, IListOutput {

	/**
	 * 검색 필드 comma separated - IListInput
	 */
	private String selectFields;
	/**
	 * 검색 조건 리스트 - IListInput
	 */
	private List<SearchCondition> queryList;
	/**
	 * 소팅 조건 리스트 - IListInput
	 */
	private List<SortCondition> sortList;
	/**
	 * 조회 결과 리스트 - IListOutput
	 */
	private List<?> items;

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

	@Override
	public List<?> getItems() {
		return this.items;
	}

	@Override
	public void setItems(List<?> items) {
		this.items = items;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> void addItem(T item) {
		if (this.items == null) {
			this.items = new ArrayList<T>();
		}

		((List<T>) this.items).add(item);
	}

}