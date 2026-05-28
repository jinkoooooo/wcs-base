/* Copyright © Nearsolution Inc. All rights reserved. */
/**
 * 
 */
package xyz.elidom.sys;

import java.util.List;

import xyz.elidom.orm.OrmConstants;
import xyz.elidom.util.ValueUtil;

/**
 * 시스템 모듈 상수 정의
 * 
 * @author Minu.Kim
 */
public class SysConstants extends OrmConstants {
	
	/**
	 * ApplicationContext Name
	 */
	public static final String APP_CONTEXT_NAME = "elidom";
	
	/**
	 * Entity Reserved Fields - show_by_name
	 */
	public static final String SHOW_BY_NAME_METHOD = "show_by_name";
	
	/**
	 * Account Status
	 */
	public static final String ACCOUNT_STATUS = "status";
	
	/**
	 * Account Status - Password Expired
	 */
	public static final String ACCOUNT_STATUS_PASSWORD_EXPIRED = "password_expired";
	
	/**
	 * Account Status - Password Change
	 */
	public static final String ACCOUNT_STATUS_PASSWORD_CHANGE = "password_change";
	
	
	/**
	 * Account Type - User
	 */
	public static final String ACCOUNT_TYPE_USER = "user";
	
	/**
	 * Account Type - Token
	 */
	public static final String ACCOUNT_TYPE_TOKEN = "token";
	
	/**
	 * Account Type - Json
	 */
	public static final String ACCOUNT_TYPE_JSON = "json";
	
	/**
	 * false string : 'false'
	 */
	public static final String FALSE_STRING = "false";
	/**
	 * true string : 'true'
	 */
	public static final String TRUE_STRING = "true";
	/**
	 * Capital false string : 'FALSE'
	 */
	public static final String CAP_FALSE_STRING = "FALSE";
	/**
	 * Capital true string : 'TRUE'
	 */
	public static final String CAP_TRUE_STRING = "TRUE";
	/**
	 * yes string : 'yes'
	 */
	public static final String YES_STRING = "yes";	
	/**
	 * Capital YES string : 'YES'
	 */
	public static final String CAP_YES_STRING = "YES";
	/**
	 * yes string : 'y'
	 */
	public static final String Y_STRING = "y";	
	/**
	 * Capital YES string : 'Y'
	 */
	public static final String CAP_Y_STRING = "Y";	
	/**
	 * no string : 'no'
	 */
	public static final String NO_STRING = "no";	
	/**
	 * Capital NO string : 'NO'
	 */
	public static final String CAP_NO_STRING = "NO";
	/**
	 * OK string : 'ok'
	 */
	public static final String OK_STRING = "ok";	
	/**
	 * Capital OK string : 'OK'
	 */
	public static final String CAP_OK_STRING = "OK";
	/**
	 * ON string : 'on'
	 */
	public static final String ON_STRING = "on";	
	/**
	 * Capital ON string : 'ON'
	 */
	public static final String CAP_ON_STRING = "ON";
	
	/**
	 * 기본적인 Reserved field list
	 */
	public static final List<String> ENTITY_FIELD_RESERVED_LIST = 
			ValueUtil.newStringList(ENTITY_FIELD_ID, 
									ENTITY_FIELD_DOMAIN_ID, 
									ENTITY_FIELD_CREATED_AT, 
									ENTITY_FIELD_UPDATED_AT,
									ENTITY_FIELD_CREATOR_ID, 
									ENTITY_FIELD_UPDATER_ID, 
									ENTITY_FIELD_CREATOR, 
									ENTITY_FIELD_UPDATER);
	/**
	 * 저장시 무시할 수 있는 기본 필드들
	 */
	public static final List<String> ENTITY_FIELD_DEFAULT_IGNORED_LIST = 
			ValueUtil.newStringList(ENTITY_FIELD_DOMAIN_ID, 
									ENTITY_FIELD_CREATED_AT, 
									ENTITY_FIELD_UPDATED_AT,
									ENTITY_FIELD_CREATOR_ID, 
									ENTITY_FIELD_UPDATER_ID, 
									ENTITY_FIELD_CREATOR, 
									ENTITY_FIELD_UPDATER);
	
	/**
	 * Megabyte 상수 - 1024 * 1024 byte
	 */
	public static final int MB = 1024 * 1024;

	/**
	 * 언어 - 한국어 
	 */
	public static final String KO_KR = "ko-KR";
	/**
	 * 언어 - 영어  
	 */	
	public static final String EN_US = "en-US";
	/**
	 * 언어 - 중국어  
	 */
	public static final String ZH_CN = "zh-CN";
	/**
	 * 기본 로케일 키    
	 */
	public static final String DEFAULT_LOCALE_KEY = "locale.default";
	
	/**
	 * Character Set - MS949    
	 */
	public static final String CHAR_SET_MS949 = "MS949";
	/**
	 * Character Set - UTF-8    
	 */	
	public static final String CHAR_SET_UTF8 = "UTF-8";
	
	/**
	 * 현재 도메인 키 - CURRENT_DOMAIN
	 */
	public static final String CURRENT_DOMAIN = "CURRENT_DOMAIN";
	/**
	 * 로케일 키 - LOCALE
	 */
	public static final String LOCALE = "LOCALE";
	/**
	 * 도메인 아이디 키 - DOMAIN_ID
	 */
	public static final String DOMAIN_ID = "DOMAIN_ID";
	/**
	 * 서버 시작 키 - SERVER_STARTED
	 */
	public static final String SERVER_STARTED = "SERVER_STARTED";
	
	/**
	 * 기본 한 페이지 레코드 개수 - 50개  
	 */
	public static final int PAGE_LIMIT = 50;

	/**
	 * Master-Detail 엔티티의 경우 마스터 삭제시 디테일 삭제 전략 : 디테일이 존재 할 경우 에러
	 */
	public static final String RESTRICT_WITH_ERROR = "restrict_with_error";

	/**
	 * Master-Detail 엔티티의 경우 마스터 삭제시 디테일 삭제 전략 : 디테일 삭제 후 디테일 콜백 없음(Hook 실행하지 않음)
	 */
	public static final String DELETE = "delete";

	/**
	 * Master-Detail 엔티티의 경우 마스터 삭제시 디테일 삭제 전략 : 디테일 삭제 후 디테일 콜백(Hook 실행)
	 */
	public static final String DESTROY = "destroy";

	/**
	 * Master-Detail 엔티티의 경우 마스터 삭제시 디테일 삭제 전략 : 디테일 삭제 후 디테일 콜백(Hook 실행)
	 */
	public static final String NULLIFY = "nullify";
	
	/**
	 * Http Reqeust
	 */
	public static final String HTTP_REQUEST = "httpRequest";
	/**
	 * Service Class
	 */	
	public static final String SERVICE_CLASS = "SERVICE_CLASS";
	/**
	 * Base Path
	 */	
	public static final String BASE_PATH = "BASE_PATH";
	/**
	 * Package Path
	 */	
	public static final String PACKAGE_PATH = "PACKAGE_PATH";
	/**
	 * Class Path
	 */
	public static final String CLASS_PATH = "CLASS_PATH";
	/**
	 * Class Name
	 */
	public static final String CLASS_NAME = "CLASS_NAME";
	/**
	 * Method Name
	 */
	public static final String METHOD_NAME = "METHOD_NAME";
	
	/**
	 * File Separator
	 */
	public static final String FILE_SEPARATOR = "/";
	/**
	 * Line Separator
	 */
	public static final String LINE_SEPARATOR = System.getProperty("line.separator");
	
	/**
	 * User Entity Constants
	 */
	/**
	 * query user by email : 'select * from users where email = :email'
	 */
	public static final String USER_BY_EMAIL_QUERY = "select * from users where email = :email";
	/**
	 * query user by login : 'select * from users where login = :login'
	 */
	public static final String USER_BY_LOGIN_QUERY = "select * from users where login = :login";
	/**
	 * User field email : 'email'
	 */
	public static final String USER_FIELD_EMAIL = "email";
	/**
	 * User field login : 'login'
	 */
	public static final String USER_FIELD_LOGIN = "login";
	
	/**
	 * Terms terms - 'terms.'
	 */
	public static final String TERM_TERMS = "terms.";
	/**
	 * Terms label - 'label.'
	 */
	public static final String TERM_LABELS = "label.";	
	/**
	 * Terms Menu Prefix : 'terms.menu.'
	 */
	public static final String TERM_MENU_PREFIX = "terms.menu.";
	/**
	 * Terms Menu Prefix : 'terms.label.'
	 */
	public static final String TERM_LABEL_PREFIX = "terms.label.";
	/**
	 * Terms Button Prefix : 'terms.button.'
	 */
	public static final String TERM_BUTTON_PREFIX = "terms.button.";
	/**
	 * Terms Setting Prefix : 'terms.setting.'
	 */
	public static final String TERM_SETTING_PREFIX = "terms.setting.";
	/**
	 * Terms Label Name : 'terms.label.name'
	 */
	public static final String TERM_LABEL_NAME = "terms.label.name";
	/**
	 * Terms Label Title : 'terms.label.title'
	 */
	public static final String TERM_LABEL_TITLE = "terms.label.title";
	/**
	 * Terms Label Content : 'terms.label.content'
	 */
	public static final String TERM_LABEL_CONTENT = "terms.label.content";	
	/**
	 * Terms Label Title : 'terms.label.destination'
	 */
	public static final String TERM_LABEL_DESTINATION = "terms.label.destination";	
	
	/**
	 * Mail Template Resource Path Prefix : 'templates.'
	 */
	public static final String MAIL_TEMPLATE_PATH_PREFIX = "templates.";
	
	/**
	 * Mail Template Resource Path Suffix : '.vm'
	 */
	public static final String MAIL_TEMPLATE_PATH_SUFFIX = ".vm";
	
	/**
	 * Character empty : ' '
	 */
	public static final Character CHAR_EMPTY = ' ';
	
	/**
	 * Character under score : '_'
	 */
	public static final Character CHAR_UNDER_SCORE = '_';
	
	/**
	 * Character dash : '-'
	 */
	public static final Character CHAR_DASH = '-';
	
	/**
	 * E-MAIL MIME-TYPE Option : 'mimeType' 
	 */
	public static final String EMAIL_OPT_MIME_TYPE = "mimeType";
	
	/**
	 * E-MAIL MIME-TYPE Text/Html : 'text/html' 
	 */
	public static final String EMAIL_MIME_TYPE_TEXT_HTML = "text/html";
	
	/**
	 * E-MAIL MIME-TYPE Text/Html UTF-8 : 'text/html; charset=UTF-8' 
	 */
	public static final String EMAIL_MIME_TYPE_TEXT_HTML_UTF_8 = "text/html; charset=UTF-8";
	
	/**
	 * E-MAIL Attachment : 'attachments' 
	 */
	public static final String EMAIL_ATTACHMENTS = "attachments";
	
	// V6 방식의 Local Host 표현.
	public static final String LOCALHOST_V6 = "0:0:0:0:0:0:0:1";
	
}