/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.util;

import java.lang.reflect.Method;
import java.util.Map;

import org.springframework.core.annotation.AnnotationUtils;

import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.AbstractStamp;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.util.ValueUtil;

/**
 * Entity 관련 유틸리티 
 * 
 * @author shortstop
 */
public class EntityUtil {
	
	/**
	 * CUD Flag Method 이름 : 'getCudFlag_'
	 */
	private static final String GET_CUD_METHOD = "getCudFlag_";
	/**
	 * Underscore ID : '_id' 
	 */
	private static final String UNDERSCORE_ID = "_id";

	/**
	 * Class에서 getCudFlag_ 메소드를 찾는다.
	 * 
	 * @param clazz
	 * @return
	 */
	public static Method getCudMethod(Class<?> clazz) {
		try {
			return clazz.getMethod(GET_CUD_METHOD);
		} catch (Exception e) {
			throw ThrowUtil.newNotFoundCudFlagMethod(clazz.getName());
		}
	}

	/**
	 * data에서 c/u/d 값을 추출한다.
	 * 
	 * @param data
	 * @param cudMethod
	 * @return
	 */
	public static <T> String getCudValue(T data, Method cudMethod) {
		if(data instanceof AbstractStamp) {
			return ((AbstractStamp)data).getCudFlag_();
		} else {
			try {
				return (String) cudMethod.invoke(data);
			} catch (Exception e) {
				throw ThrowUtil.newNotFoundCudFlagValue();
			}			
		}
	}
	
	/**
	 * Table metadata에 따른 type mapping 
	 * 
	 * @param colName
	 * @param colType
	 * @return
	 */
	public static String mapColumnType(String colName, String colType) {
		if(colName.equalsIgnoreCase(SysConstants.ENTITY_FIELD_ID) || colName.endsWith(UNDERSCORE_ID)) {
			return "integer";
		} else if(colType.startsWith("timestamp")) {
			return "datetime";
		} else if(colType.startsWith("character")) {
			return "string";
		} else {
			return colType;
		}
	}
	
	/**
	 * column name으로 Reference Entity명을 매핑 
	 * 
	 * @param colName
	 * @return
	 */
	public static String mapRefName(String colName) {
		String entityName = null;
		
		if(colName.equalsIgnoreCase(SysConstants.TABLE_FIELD_CREATOR_ID) || colName.equalsIgnoreCase(SysConstants.TABLE_FIELD_UPDATER_ID)) {
			entityName = "User";
		} else {
			entityName = colName.substring(0, colName.lastIndexOf('_'));
			entityName = ValueUtil.toCamelCase(entityName, '_', true);							
		}
		
		return entityName;
	}
	
	/**
	 * Entity의 ID Type이 Complex-key Type인지 확인.
	 * 
	 * @param entityClass
	 * @return
	 */
	public static boolean isComplexKeyType(Class<?> entityClass) {
		Map<String, Object> attributeMap = AnnotationUtils.getAnnotationAttributes(entityClass.getAnnotation(Table.class));
		String strategy = ValueUtil.toString(attributeMap.get("idStrategy"));
		return ValueUtil.isEqual(strategy, GenerationRule.COMPLEX_KEY);
	}
}