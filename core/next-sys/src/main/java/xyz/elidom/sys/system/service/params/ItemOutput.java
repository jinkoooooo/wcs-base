/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.params;

import xyz.elidom.sys.system.service.params.conds.IItemOutput;

/**
 * 하나의 레코드만 리턴하는 경우의 Output
 * 
 * @author shortstop
 */
@SuppressWarnings("unchecked")
public class ItemOutput extends BasicOutput implements IItemOutput {

	/**
	 * 리턴 오브젝트
	 */
	private Object item;

	@Override
	public <T> T getItem() {
		return (T) item;
	}

	@Override
	public <T> void setItem(T item) {
		this.item = item;
	}
}