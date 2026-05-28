/* Copyright © Nearsolution Inc. All rights reserved. */
/**
 * 
 */
package xyz.elidom.core;

import java.util.Arrays;
import java.util.List;

import xyz.elidom.sys.SysConstants;

/**
 * Core 모듈의 상수 정의
 * 
 * @author Minu.Kim
 */
public class CoreConstants extends SysConstants {

	public static final String FILE_ID = "FILE_ID";
	public static final String FILE_ON_ID = "FILE_ON_ID";
	public static final String FILE_ON_TYPE = "FILE_ON_TYPE";
	public static final String FILE_SAVE_PATH_NAME = "FILE_SAVE_PATH_NAME";
	public static final String FILE_SAVE_CUSTOM_PATH = "FILE_UPLOAD_CUSTOM_PATH";
	public static final String REQUEST_PARAM_MAP = "REQUEST_PARAM_MAP";

	public static final String SYSTEM_LABEL = "label";
	public static final String SYSTEM_SCENE = "scene";
	public static final String SYSTEM_XMES = "xmes";
	
	public static final String DATA_TYPE = "data_type";
	public static final String PARAM_FIELD = "param";
	public static final String PARAMETER_ALIAS = "parameter_alias";

	// DB Column Types
	public static final List<String> STRING_DB_COLUMN_TYPES = Arrays.asList("varchar","varchar2","text","longtext","clob","character varing","nvarchar","ntext");
	public static final List<String> NUMBER_DB_COLUMN_TYPES = Arrays.asList("integer","int","long","number","double","float","double precision","decimal","bigint","numeric","real");
	public static final List<String> BOOLEAN_DB_COLUMN_TYPES = Arrays.asList("boolean","bit");
	public static final List<String> DATE_DB_COLUMN_TYPES = Arrays.asList("date","datetime","timestamp");
}