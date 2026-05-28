/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.orm.entity.relation;

/**
 * domainId와 name으로 unique한 Reference 데이터 인터페이스 
 * 
 * @author shortstop
 */
public interface IUniqueNameRef extends IdFindable {
	
	/**
	 * name
	 * 
	 * @return
	 */
	public String getName();
}
