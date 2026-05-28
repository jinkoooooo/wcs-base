/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.orm.entity.relation;

/**
 * name 값으로 active 상태의 엔티티 id를 조회하는 참조 엔티티
 * 
 * @author shortstop
 */
public abstract class UniqueNameActiveStringIdRef implements IUniqueNameRef {

	/**
	 * get name
	 */
	public abstract String getName();

	/**
	 * get version
	 */
	public abstract Integer getVersion();
	
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
