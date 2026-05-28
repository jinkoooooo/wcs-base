/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.orm.entity.relation;

/**
 * ID가 설정되어 있지 않은 상태에서 Self 인스턴스 데이터로 Id를 조회하여 설정할 수 있는 데이터 인터페이스 
 * 이 인터페이스를 구현하는 Entity 혹은 EntityRef는 반드시 Unique Index가 있어야 한다. 
 * 
 * @author shortstop
 */
public interface IdFindable {

	/**
	 * Self Instance 데이터로 ID를 설정한다.
	 * 
	 * @return id
	 */
	public Object findAndSetId();
}
