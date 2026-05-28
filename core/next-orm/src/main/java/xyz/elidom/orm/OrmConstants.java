/* Copyright © Nearsolution Inc. All rights reserved. */
/**
 * 
 */
package xyz.elidom.orm;

import java.util.List;

import xyz.elidom.util.ValueUtil;

/**
 * ORM에서 사용되는 상수 정의
 * 
 * @author Minu.Kim
 */
public class OrmConstants {
	
	/**
	 * Database Domain Name : 'public'
	 */
	public static final String DEFAULT_DOMAIN = "public";
	
	/**
	 * Name key : 'name'
	 */
	public static final String NAME_KEY = "name";
	/**
	 * Label key : 'label'
	 */
	public static final String LABEL_KEY = "label";	
	/**
	 * And key : 'and'
	 */
	public static final String AND_KEY = "and";
	/**
	 * Field key : 'and'
	 */
	public static final String FIELD_KEY = "field";
	
	/**
	 * IdField Table Annotation : 'idField'
	 */
	public static final String TABLE_ANN_ID_FIELD = "idField";	
	/**
	 * TitleField Table Annotation : 'titleField'
	 */
	public static final String TABLE_ANN_TITLE_FIELD = "titleField";		
	/**
	 * IdStrategy Table Annotation : 'idStrategy'
	 */
	public static final String TABLE_ANN_ID_STRATEGY_FIELDS = "idStrategy";	
	/**
	 * MeaningfulId Table Annotation : 'meaningfulFields'
	 */
	public static final String TABLE_ANN_MEANINGFUL_FIELDS = "meaningfulFields";
	/**
	 * UniqueFields Table Annotation : 'uniqueFields'
	 */
	public static final String TABLE_ANN_UNIQUE_FIELDS = "uniqueFields";
	/**
	 * NotnullFields Table Annotation : 'notnullFields'
	 */
	public static final String TABLE_ANN_NOTNULL_FIELDS = "notnullFields";
	
	/**
	 * UUID 타입의 ID 필드 사이즈 : 40
	 */
	public static final int FIELD_SIZE_UUID = 40;
	/**
	 * Meaningful 타입의 ID 필드 사이즈 : 45
	 */
	public static final int FIELD_SIZE_MEANINGFUL_ID = 45;
	/**
	 * User ID 필드 사이즈 : 32
	 */
	public static final int FIELD_SIZE_USER_ID = 32;
	/**
	 * 기본적인 Name 필드 사이즈 : 36
	 */
	public static final int FIELD_SIZE_NAME = 36;
	/**
	 * Long Name 필드 사이즈 : 128
	 */
	public static final int FIELD_SIZE_LONG_NAME = 128;
	/**
	 * 기본적인 Status 필드 사이즈 : 20
	 */
	public static final int FIELD_SIZE_STATUS = 20;
	/**
	 * 기본적인 Category 필드 사이즈 : 20
	 */
	public static final int FIELD_SIZE_CATEGORY = 20;
	/**
	 * 기본적인 E-Mail 필드 사이즈 : 40
	 */
	public static final int FIELD_SIZE_EMAIL = 64;
	/**
	 * 기본적인 URL 필드 사이즈 : 128
	 */
	public static final int FIELD_SIZE_URL = 128;
	/**
	 * 기본적인 IP 필드 사이즈 : 32
	 */
	public static final int FIELD_SIZE_IP = 32;
	/**
	 * 기본적인 LOCALE 필드 사이즈 : 10
	 */
	public static final int FIELD_SIZE_LOCALE = 10;
	/**
	 * 기본적인 File Path 필드 사이즈 : 128
	 */
	public static final int FIELD_SIZE_FILE_PATH = 128;	
	/**
	 * Description 필드 사이즈 : 255
	 */
	public static final int FIELD_SIZE_DESCRIPTION = 255;
	/**
	 * Date 필드 사이즈 : 10
	 */
	public static final int FIELD_SIZE_DATE = 10;
	/**
	 * DateTime 필드 사이즈 : 20
	 */
	public static final int FIELD_SIZE_DATETIME = 20;
	/**
	 * 기본적인 Value 255 사이즈 필드 사이즈 : 255
	 */
	public static final int FIELD_SIZE_VALUE_255 = 255;
	/**
	 * 기본적인 Value 1000 사이즈 필드 사이즈 : 1000
	 */
	public static final int FIELD_SIZE_VALUE_1000 = 1000;
	/**
	 * 기본적인 Value 2000 사이즈 필드 사이즈 : 2000
	 */
	public static final int FIELD_SIZE_VALUE_2000 = 2000;
	/**
	 * Max Text 필드 사이즈 : 4000
	 */
	public static final int FIELD_SIZE_MAX_TEXT = 4000;
	
	/**
	 * Create, Update, Delete 플래그 중 - CREATE 플래그 'c'
	 */
	public static final String CUD_FLAG_CREATE = "c";
	/**
	 * Create, Update, Delete 플래그 중 - UPDATE 플래그 'u'
	 */	
	public static final String CUD_FLAG_UPDATE = "u";
	/**
	 * Create, Update, Delete 플래그 중 - DELETE 플래그 'd'
	 */	
	public static final String CUD_FLAG_DELETE = "d";
	
	/**
	 * 검색 조건 - equal : 'eq'
	 */
	public static final String EQUAL = "eq";
	/**
	 * 검색 조건 - not equal : 'noteq'
	 */	
	public static final String NOT_EQUAL = "noteq";
	/**
	 * 검색 조건 - like (%value%) : 'like'
	 */	
	public static final String LIKE = "like";
	/**
	 * 검색 조건 - starts With (%value) : 'sw'
	 */	
	public static final String STARTS_WITH = "sw";
	/**
	 * 검색 조건 - ends With (value%) : 'ew;
	 */
	public static final String ENDS_WITH = "ew";
	/**
	 * 검색 조건 - not Like : 'nlike'
	 */
	public static final String NOT_LIKE = "nlike";
	/**
	 * 검색 조건 - does not starts with : 'dnsw'
	 */
	public static final String DOES_NOT_START_WITH = "dnsw";
	/**
	 * 검색 조건 - does not ends with : 'dnew'
	 */
	public static final String DOES_NOT_END_WITH = "dnew";
	/**
	 * 검색 조건 - greater than : 'gt'
	 */
	public static final String GREATER_THAN = "gt";
	/**
	 * 검색 조건 - greater than equal : 'gte'
	 */
	public static final String GREATER_THAN_EQUAL = "gte";
	/**
	 * 검색 조건 - less than : 'lt'
	 */
	public static final String LESS_THAN = "lt";
	/**
	 * 검색 조건 - less than equal : 'lte'
	 */
	public static final String LESS_THAN_EQUAL = "lte";
	/**
	 * 검색 조건 - between : 'between'
	 */
	public static final String BETWEEN = "between";
	/**
	 * 검색 조건 - is null : 'is_null'
	 */
	public static final String IS_NULL = "is_null";
	/**
	 * 검색 조건 - is not null : 'is_not_null'
	 */
	public static final String IS_NOT_NULL = "is_not_null";
	/**
	 * 검색 조건 - 값이 비어있는지 여부 : 'is_blank'
	 */
	public static final String IS_BLANK = "is_blank";
	/**
	 * 검색 조건 - 존재하는지 여부 : 'is_present'
	 */
	public static final String IS_PRESENT = "is_present";
	/**
	 * 검색 조건 - in : 'in'
	 */
	public static final String IN = "in";
	/**
	 * 소팅 조건 - asc : 'ASC'
	 */
	public static final String ASC = "ASC";
	/**
	 * 소팅 조건 - desc : 'DESC'
	 */
	public static final String DESC = "DESC";
	
	/**
	 * At : '@'
	 */	
	public static final String AT = "@";
	/**
	 * Dot : '.'
	 */
	public static final String DOT = ".";
	/**
	 * Comma : ','
	 */
	public static final String COMMA = ",";
	/**
	 * Dash : '-'
	 */
	public static final String DASH = "-";
	/**
	 * Space : ' '
	 */
	public static final String SPACE = " ";
	/**
	 * Empty String : ''
	 */
	public static final String EMPTY_STRING = "";	
	/**
	 * Colon : ':'
	 */
	public static final String COLON = ":";
	/**
	 * SemiColon : ';'
	 */
	public static final String SEMI_COLON = ";";	
	/**
	 * Colon : '?'
	 */
	public static final String QUESTION = "?";
	/**
	 * Ampersend : '&'
	 */
	public static final String AMPERSEND = "&";
	/**
	 * Slash : '/'
	 */
	public static final String SLASH = "/";	
	/**
	 * Equal : '='
	 */
	public static final String EQUALS = "=";
	/**
	 * Dollar : '='
	 */
	public static final String DOLLAR = "$";	
	/**
	 * Query Binding : ' = :'
	 */
	public static final String QUERY_BINDING = " = :";		
	
	/**
	 * Entity Reserved Fields - id
	 */
	public static final String ENTITY_FIELD_ID = "id";
	/**
	 * Entity Reserved Fields - name
	 */	
	public static final String ENTITY_FIELD_NAME = "name";
	/**
	 * Entity Reserved Fields - description
	 */	
	public static final String ENTITY_FIELD_DESCRIPTION = "description";
	/**
	 * Entity Reserved Fields - domainId
	 */	
	public static final String ENTITY_FIELD_DOMAIN_ID = "domainId";
	/**
	 * Entity Reserved Fields - createdAt
	 */	
	public static final String ENTITY_FIELD_CREATED_AT = "createdAt";
	/**
	 * Entity Reserved Fields - updatedAt
	 */	
	public static final String ENTITY_FIELD_UPDATED_AT = "updatedAt";
	/**
	 * Entity Reserved Fields - creatorId
	 */
	public static final String ENTITY_FIELD_CREATOR_ID = "creatorId";
	/**
	 * Entity Reserved Fields - updaterId
	 */
	public static final String ENTITY_FIELD_UPDATER_ID = "updaterId";
	/**
	 * Entity Reserved Fields - creator
	 */
	public static final String ENTITY_FIELD_CREATOR = "creator";
	/**
	 * Entity Reserved Fields - updater
	 */
	public static final String ENTITY_FIELD_UPDATER = "updater";
	/**
	 * Entity Reserved Fields - status
	 */
	public static final String ENTITY_FIELD_STATUS = "status";
	/**
	 * Entity Reserved Fields - version
	 */
	public static final String ENTITY_FIELD_VERSION = "version";	
	/**
	 * Entity Reserved Fields - onType
	 */
	public static final String ENTITY_FIELD_ON_TYPE = "onType";
	/**
	 * Entity Reserved Fields - onId
	 */	
	public static final String ENTITY_FIELD_ON_ID = "onId";
	/**
	 * Entity Reserved Fields - resourceType
	 */
	public static final String ENTITY_FIELD_RESOURCE_TYPE = "resourceType";
	/**
	 * Entity Reserved Fields - resourceId
	 */	
	public static final String ENTITY_FIELD_RESOURCE_ID = "resourceId";
	
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
	 * Domain ID and Name - 'domainId,name'
	 */
	public static final String ENTITY_DOMAIN_ID_AND_NAME = ENTITY_FIELD_DOMAIN_ID + COMMA + ENTITY_FIELD_NAME;
	
	/**
	 * Table Reserved Fields - id
	 */
	public static final String TABLE_FIELD_ID = "id";
	/**
	 * Table Reserved Fields - name
	 */	
	public static final String TABLE_FIELD_NAME = "name";
	/**
	 * Table Reserved Fields - description
	 */	
	public static final String TABLE_FIELD_DESCRIPTION = "description";
	/**
	 * Table Reserved Fields - domain_id
	 */	
	public static final String TABLE_FIELD_DOMAIN_ID = "domain_id";
	/**
	 * Table Reserved Fields - created_at
	 */	
	public static final String TABLE_FIELD_CREATED_AT = "created_at";
	/**
	 * Table Reserved Fields - updated_at
	 */	
	public static final String TABLE_FIELD_UPDATED_AT = "updated_at";
	/**
	 * Table Reserved Fields - creator_id
	 */
	public static final String TABLE_FIELD_CREATOR_ID = "creator_id";
	/**
	 * Table Reserved Fields - updater_id
	 */
	public static final String TABLE_FIELD_UPDATER_ID = "updater_id";
	
	/**
	 * 기본 테이블 필드들
	 */
	public static final List<String> TABLE_FIELD_DEFAULT_LIST = 
			ValueUtil.newStringList(TABLE_FIELD_DOMAIN_ID, 
									TABLE_FIELD_CREATED_AT, 
									TABLE_FIELD_UPDATED_AT,
									TABLE_FIELD_CREATOR_ID, 
									TABLE_FIELD_UPDATER_ID,
									ENTITY_FIELD_CREATOR, 
									ENTITY_FIELD_UPDATER);
	
	/**
	 * 저장시 무시할 수 있는 기본 테이블 필드들
	 */
	public static final List<String> TABLE_FIELD_DEFAULT_IGNORED_LIST = 
			ValueUtil.newStringList(TABLE_FIELD_DOMAIN_ID, 
									TABLE_FIELD_CREATED_AT, 
									TABLE_FIELD_UPDATED_AT,
									TABLE_FIELD_CREATOR_ID, 
									TABLE_FIELD_UPDATER_ID,
									ENTITY_FIELD_CREATOR, 
									ENTITY_FIELD_UPDATER);
	
	/**
	 * String Data Type - 'string'
	 */
	public static final String DATA_TYPE_STRING = "string";
	/**
	 * Text Data Type - 'text'
	 */
	public static final String DATA_TYPE_TEXT = "text";
	/**
	 * Number Data Type - 'number'
	 */
	public static final String DATA_TYPE_NUMBER = "number";
	/**
	 * Integer Data Type - 'integer'
	 */
	public static final String DATA_TYPE_INTEGER = "integer";
	/**
	 * Decimal Data Type - 'decimal'
	 */
	public static final String DATA_TYPE_DECIMAL = "decimal";
	/**
	 * Double Data Type - 'double'
	 */
	public static final String DATA_TYPE_DOUBLE = "double";
	/**
	 * Float Data Type - 'float'
	 */
	public static final String DATA_TYPE_FLOAT = "float";
	/**
	 * Long Data Type - 'long'
	 */
	public static final String DATA_TYPE_LONG = "long";
	/**
	 * Date Data Type - 'date'
	 */
	public static final String DATA_TYPE_DATE = "date";
	/**
	 * Date Data Type - 'time'
	 */
	public static final String DATA_TYPE_TIME = "time";	
	/**
	 * Datetime Data Type - 'datetime'
	 */
	public static final String DATA_TYPE_DATETIME = "datetime";
	/**
	 * Timestamp Data Type - 'timestamp'
	 */
	public static final String DATA_TYPE_TIMESTAMP = "timestamp";	
	/**
	 * Boolean Data Type - 'boolean'
	 */
	public static final String DATA_TYPE_BOOLEAN = "boolean";
	/**
	 * Binary Data Type - 'binary'
	 */
	public static final String DATA_TYPE_BINARY = "binary";	
	
	/**
	 * select id from
	 */
	public static final String ENTITY_ID_QUERY_PREFIX = "select id from ";
	/**
	 * where 1=1
	 */
	public static final String ENTITY_ID_QUERY_WHERE = " where 1=1 ";
}