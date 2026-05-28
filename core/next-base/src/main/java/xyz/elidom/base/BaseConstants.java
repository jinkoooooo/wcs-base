/* Copyright © Nearsolution Inc. All rights reserved. */
/**
 * 
 */
package xyz.elidom.base;

import xyz.elidom.core.CoreConstants;
import xyz.elidom.sys.SysConstants;

/**
 * base 모듈에 필요한 상수 정의 
 * 
 * @author Minu.Kim
 */
public class BaseConstants extends CoreConstants {
	
	// Resource Type - ENTITY, DIY_SERVICE, DIY_GRID
	
	/**
	 * Resource Type - ENTITY : 'ENTITY'
	 */
	public static final String RESOURCE_TYPE_ENTITY = "ENTITY";
	
	/**
	 * Reference Type - DIY_SERVICE  : 'DIY_SERVICE'
	 */
	public static final String RESOURCE_TYPE_DIY_SERVICE = "DIY_SERVICE";
	
	/**
	 * Reference Type - DIY_GRID  : 'DIY_GRID'
	 */
	public static final String RESOURCE_TYPE_DIY_GRID = "DIY_GRID";	
	
	// Reference Type - CommonCode, Menu, Entity
	
	/**
	 * Reference Type - CommonCode : 'CommonCode'
	 */
	public static final String REF_TYPE_COMMON_CODE = "CommonCode";
	
	/**
	 * Reference Type - Menu  : 'Menu'
	 */
	public static final String REF_TYPE_MENU = "Menu";
	
	/**
	 * Reference Type - Entity  : 'Entity'
	 */
	public static final String REF_TYPE_ENTITY = "Entity";
	
	// Menu Category : STANDARD, TERMINAL
	
	/**
	 * Menu Category - STANDARD : 'STANDARD'
	 */
	public static final String MENU_CATEGORY_STANDARD = "STANDARD";
	
	/**
	 * Menu Category - TERMINAL : 'TERMINAL'
	 */
	public static final String MENU_CATEGORY_TERMINAL = "TERMINAL";
	
	// Menu Meta Item Names : menu, columns, buttons, menu_params, params
	
	/**
	 * Menu Object - Menu : 'menu'
	 */
	public static final String MENU_OBJECT_MENU_NAME = "menu";
	
	/**
	 * Menu Object - Master : 'master'
	 */
	public static final String MENU_OBJECT_MASTER_NAME = "master";
	
	/**
	 * Menu Object - MenuColumns : 'columns'
	 */
	public static final String MENU_OBJECT_COLUMNS_NAME = "columns";
	
	/**
	 * Menu Object - MenuButtons : 'buttons'
	 */
	public static final String MENU_OBJECT_BUTTONS_NAME = "buttons";
	
	/**
	 * Menu Object - MenuParams : 'menu_params'
	 */
	public static final String MENU_OBJECT_MENU_PARAMS_NAME = "menu_params";
	
	/**
	 * Menu Object - MenuParams : 'params'
	 */
	public static final String MENU_OBJECT_PARAMS_NAME = "params";	
	
	// Menu Button Permissions : create, update, delete, show
	
	/**
	 * Menu Button Read Permission : show
	 */
	public static final String MENU_PERMISION_SHOW = "show";
	
	/**
	 * Menu Button Create Permission : create
	 */
	public static final String MENU_PERMISSION_CREATE = "create";
	
	/**
	 * Menu Button Update Permission : update
	 */
	public static final String MENU_PERMISSION_UPDATE = "update";
	
	/**
	 * Menu Button Delete Permission : delete
	 */
	public static final String MENU_PERMISSION_DELETE = "delete";
	
	// Menu Button Permission Values : C, U, D, R
	
	/**
	 * Menu Button Permission Read Value : R
	 */
	public static final String MENU_PERMISION_SHOW_VALUE = "R";
	
	/**
	 * Menu Button Permission Create Value : C
	 */
	public static final String MENU_PERMISSION_CREATE_VALUE = "C";
	
	/**
	 * Menu Button Permission Update Value : U
	 */
	public static final String MENU_PERMISSION_UPDATE_VALUE = "U";
	
	/**
	 * Menu Button Permission Delete Value : D
	 */
	public static final String MENU_PERMISSION_DELETE_VALUE = "D";
	
	// Menu Button Permission Values : C, U, D, R
	
	/**
	 * Menu Button Permission Read Value : ,R
	 */
	public static final String MENU_PERMISION_COMMA_SHOW_VALUE = ",R";
	
	/**
	 * Menu Button Permission Create Value : ,C
	 */
	public static final String MENU_PERMISSION_COMMA_CREATE_VALUE = ",C";
	
	/**
	 * Menu Button Permission Update Value : ,U
	 */
	public static final String MENU_PERMISSION_COMMA_UPDATE_VALUE = ",U";
	
	/**
	 * Menu Button Permission Delete Value : ,D
	 */
	public static final String MENU_PERMISSION_COMMA_DELETE_VALUE = ",D";
	
	// Menu Query Authorization Mode : all, auth
	
	/**
	 * Menu Query Authorization Mode - 'auth'
	 */
	public static final String MENU_QUERY_AUTH_MODE = "auth";
	
	/**
	 * Menu Query All Mode - 'all'
	 */
	public static final String MENU_QUERY_ALL_MODE = "all";
	
	/**
	 * Grid Code Editor prefix - 'code-'
	 */
	public static final String GRID_CODE_EDITOR_PREFIX = "code-";
	
	// ETC : *
	
	/**
	 * Star : '*'
	 */
	public static final String STAR = "*";
	
	/**
	 * Field Name menuId : 'menuId' 
	 */
	public static final String FIELD_NAME_MENU_ID = "menuId";
	
	/**
	 * Field Name menuDetailId : 'menuDetailId' 
	 */
	public static final String FIELD_NAME_MENU_DETAIL_ID = "menuDetailId";
	
	/**
	 * Field Name title : 'title' 
	 */
	public static final String FIELD_NAME_TITLE = "title";
	
	/**
	 * Field Name menu : 'menu' 
	 */
	public static final String FIELD_NAME_MENU = "menu";	
	
	/**
	 * Field Name rank : 'rank' 
	 */
	public static final String FIELD_NAME_RANK = "rank";
	
	/**
	 * Field Name category : 'category' 
	 */
	public static final String FIELD_NAME_CATEGORY = "category";
	

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