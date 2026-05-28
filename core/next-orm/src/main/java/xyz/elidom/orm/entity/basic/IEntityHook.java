/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.orm.entity.basic;

/**
 * Entity가 CRUD 오퍼레이션 전후에 할 액션들을 정의한다.
 * 
 * @author shortstop
 */
public interface IEntityHook {

	/**
	 * 생성 전 액션
	 */
	public void beforeCreate();

	/**
	 * 생성 후 액션
	 */
	public void afterCreate();

	/**
	 * 업데이트 전 액션
	 */
	public void beforeUpdate();

	/**
	 * 업데이트 후 액션
	 */
	public void afterUpdate();

	/**
	 * 삭제 전 액션
	 */
	public void beforeDelete();

	/**
	 * 삭제 후 액션
	 */
	public void afterDelete();

	/**
	 * 조회(Single) 전 액션
	 */
	public void beforeFind();

	/**
	 * 조회(Single) 후 액션
	 */
	public void afterFind();
	
	/**
	 * 조회(Multi) 전 액션
	 */
	public void beforeSearch();

	/**
	 * 조회(Multi) 후 액션
	 */
	public void afterSearch();
}