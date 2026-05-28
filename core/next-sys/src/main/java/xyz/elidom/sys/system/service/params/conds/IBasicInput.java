/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.params.conds;

/**
 *  ID 하나만 받아서 조회나 트랜잭션을 일으키는 서비스 - ex) Find One, Delete 등의 API 
 * 
 * @author shortstop
 */
public interface IBasicInput {

	/**
	 * ID 리턴 
	 * 
	 * @return
	 */
	public String getId();
	
	/**
	 * ID 설정
	 * 
	 * @param id
	 */
	public void setId(String id);
}
