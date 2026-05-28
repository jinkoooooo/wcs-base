package xyz.anythings.sys;

import xyz.elidom.base.BaseConstants;
import xyz.elidom.sys.SysConstants;

/**
 * Anythings 관련 상수 정의 
 * 
 * @author shortstop
 */
public class AnyConstants extends BaseConstants {

	/**
	 * 기본 unselect fields
	 */
	public static final String[] DEFAULT_UNSELECT_QUERY_FIELDS = {
		SysConstants.ENTITY_FIELD_CREATOR, 
		SysConstants.ENTITY_FIELD_UPDATER, 
		SysConstants.ENTITY_FIELD_CREATOR_ID, 
		SysConstants.ENTITY_FIELD_UPDATER_ID, 
		SysConstants.ENTITY_FIELD_CREATED_AT, 
		SysConstants.ENTITY_FIELD_UPDATED_AT
	};
	
	/**
	 * Asterisk
	 */
	public static final String ASTERISK = "*";
	/**
	 * ng 
	 */
	public static final String NG_STRING = "ng";
	/**
	 * NG
	 */
	public static final String NG_CAP_STRING = "NG";
	/**
	 * n
	 */
	public static final String N_STRING = "n";
	/**
	 * N
	 */
	public static final String N_CAP_STRING = "N";
	/**
	 * y
	 */
	public static final String Y_STRING = "y";
	/**
	 * Y
	 */
	public static final String Y_CAP_STRING = "Y";
	/**
	 * null
	 */
	public static final String NULL_STRING = "null";
	/**
	 * NULL
	 */
	public static final String NULL_CAP_STRING = "NULL";
	/**
	 * 해당 없음 상수 : _na_
	 */
	public static final String NOT_AVAILABLE_STRING = "_na_";
	/**
	 * 해당 없음 상수 : NA
	 */
	public static final String NOT_AVAILABLE_CAP_STRING = "NA";
	/**
	 * all
	 */
	public static final String ALL_STRING = "all";
	/**
	 * ALL
	 */
	public static final String ALL_CAP_STRING = "ALL";
	
	/**
	 * 문자열 0
	 */
	public static final String ZERO_STRING = "0";
	/**
	 * 문자열 1
	 */
	public static final String ONE_STRING = "1";
	/**
	 * 문자열 2
	 */
	public static final String TWO_STRING = "2";
	/**
	 * 문자열 -1
	 */
	public static final String MINUS_ONE_STRING = "-1";
	/**
	 * 문자열 9
	 */
	public static final String NINE_STRING = "9";
	
	/**
	 * 공통 대기 상태 : Waiting
	 */
	public static final String COMMON_STATUS_WAIT = "W";
	/**
	 * 공통 투입 상태 : Input
	 */
	public static final String COMMON_STATUS_INPUT = "I";
	/**
	 * 공통 완료 상태 : Completed
	 */
	public static final String COMMON_STATUS_FINISHED = "F";
	/**
	 * 공통 진행 상태 : Running
	 */
	public static final String COMMON_STATUS_RUNNING = "R";
	/**
	 * 공통 에러 상태 : Error
	 */
	public static final String COMMON_STATUS_ERROR = "E";
	/**
	 * 공통 취소 상태 : Canceled
	 */
	public static final String COMMON_STATUS_CANCEL = "C";
	/**
	 * 공통 Skip 상태 : Skipped
	 */
	public static final String COMMON_STATUS_SKIPPED = "S";
	
}
