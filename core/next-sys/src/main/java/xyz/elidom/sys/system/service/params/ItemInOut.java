/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.params;

import xyz.elidom.sys.system.service.params.conds.IItemInput;

/**
 * Input 내용으로 그대로 트랜잭션을 일으켜서 하나의 레코드를 리턴하는 서비스 - ex) Create, Update
 * 
 * @author shortstop
 */
@SuppressWarnings("unchecked")
public class ItemInOut extends ItemOutput implements IItemInput {
	
	/**
	 * Input Parameter
	 */
	private Object input;

	@Override
	public <T> T getInput() {
		return (T) this.input;
	}

	@Override
	public <T> void setInput(T input) {
		this.input = input;
	}

}
