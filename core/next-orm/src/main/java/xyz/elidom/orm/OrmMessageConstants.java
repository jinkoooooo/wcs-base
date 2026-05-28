/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.orm;

import xyz.elidom.exception.ExceptionMessageConstants;

/**
 * ORM에서 사용되는 메시지 상수 정의 
 * 
 * @author Minu.Kim
 */
public class OrmMessageConstants extends ExceptionMessageConstants {
	/**
	 * {0}({1}) 을(를) 찾을수 없습니다.
	 * {0}({1}) not found.
	 */
	public static final String NOT_FOUND = "NOT_FOUND";

	/**
	 * 파라미터 {0}은 빈 값을 허용하지 않습니다.
	 * Empty {0} is not allowed!
	 */
	public static final String EMPTY_PARAM = "EMPTY_PARAM";

	/**
	 * ({0}) 데이터 ({1})은 이미 존재 합니다.
	 * ({0)) Data ({1}) already exist.
	 */
	public static final String DATA_DUPLICATED = "DATA_DUPLICATED";

	/**
	 * 엔티티 ({0})에 uniqueFields 정보가 존재하지 않습니다.
	 * Not found uniqueFields in entity ({0})
	 */
	public static final String NOT_FOUND_UNIQUE_FIELDS = "NOT_FOUND_UNIQUE_FIELDS";

	/**
	 * 엔티티의 키가 올바르지 않습니다.
	 * Invalid keys of entity!
	 */
	public static final String INVALID_KEYS = "INVALID_KEYS";

	/**
	 * 엔티티 ({0})에 Detail Data가 존재합니다. Detail 데이터 삭제 후 다시 시도해주세요.
	 * There are detail data in entity ({0}), Please re-try after deleting detail data.
	 */
	public static final String HAS_DETAIL_DATA = "HAS_DETAIL_DATA";

	/**
	 * 엔티티 ({0})의 ID Type ({1})의 데이터 타입은 반드시 ({2}) 이어야 합니다.
	 * Data type must be ({2}) of ID Type ({1}) of entity ({0})
	 */
	public static final String INVALID_DATA_TYPE_OF_ENTITY_ID_TYPE = "INVALID_DATA_TYPE_OF_ENTITY_ID_TYPE";
}