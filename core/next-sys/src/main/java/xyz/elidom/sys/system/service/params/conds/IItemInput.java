/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.params.conds;

/**
 * Input 내용으로 그대로 트랜잭션을 일으키는 서비스 - ex) Create, Update
 * 
 * @author shortstop
 */
public interface IItemInput {

	/**
	 * Input Parameter
	 * 
	 * @return
	 */
	public <T> T getInput();

	/**
	 * Input Parameter
	 * 
	 * @param input
	 */
	public <T> void setInput(T input);
}