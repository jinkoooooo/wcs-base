/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.params.conds;

import java.util.List;

/**
 * 검색을 하여 List를 리턴하되 Pagination 하지 않는 서비스의 Output
 * 
 * @author shortstop
 */
public interface IListOutput extends IBasicOutput {

	/**
	 * Get 조회 결과 리스트
	 * 
	 * @return
	 */
	public List<?> getItems();

	/**
	 * Set 조회 결과 리스트
	 * 
	 * @param items
	 */
	public void setItems(List<?> items);

	/**
	 * List에 Item 추가
	 * @param item
	 */
	public <T> void addItem(T item);
}