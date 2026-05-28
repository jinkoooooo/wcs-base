/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.params;

import java.util.ArrayList;
import java.util.List;

import xyz.elidom.sys.system.service.params.conds.IListOutput;

/**
 * 검색을 하여 List를 리턴하되 Pagination 하지 않는 서비스의 Output
 * 
 * @author shortstop
 */
public class ListOutput extends BasicOutput implements IListOutput {

	/**
	 * 조회 결과 리스트 - IListOutput
	 */

	private List<?> items;

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
		if (items == null) {
			items = new ArrayList<T>();
		}

		((List<T>) this.items).add(item);
	}
}