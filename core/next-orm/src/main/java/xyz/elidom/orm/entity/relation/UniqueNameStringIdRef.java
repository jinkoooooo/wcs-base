/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.orm.entity.relation;

/**
 * name 값으로 id를 조회하는 참조 엔티티
 * 
 * @author Minu.Kim
 */
public abstract class UniqueNameStringIdRef implements IUniqueNameRef {

	/**
	 * get name
	 */
	public abstract String getName();

	/**
	 * set Id
	 * 
	 * @param id
	 */
	public abstract void setId(String id);
	
	/**
	 * get Id
	 * 
	 * @return
	 */
	public abstract String getId();

	@Override
	public Object findAndSetId() {
		return this.getId();
	}
}
