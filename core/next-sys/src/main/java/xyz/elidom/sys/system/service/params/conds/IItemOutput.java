/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.params.conds;

/**
 * 하나의 레코드만 리턴하는 경우의 Output
 * 
 * @author shortstop
 */
public interface IItemOutput {

	/**
	 * Get Output Record
	 * 
	 * @return
	 */
	public <T> T getItem();

	/**
	 * Set Output Record
	 * 
	 * @param item
	 */
	public <T> void setItem(T item);
}