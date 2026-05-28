/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.params;

import xyz.elidom.sys.system.service.params.conds.IBasicInput;

/**
 *  ID 하나만 받아서 조회나 트랜잭션을 일으키는 서비스 - ex) Find One, Delete 등의 API 
 * 
 * @author shortstop
 */
public class BasicInput implements IBasicInput {

	/**
	 * ID
	 */
	private String id;
	
	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

}
