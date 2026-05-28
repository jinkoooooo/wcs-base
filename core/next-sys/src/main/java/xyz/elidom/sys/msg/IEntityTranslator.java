/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.msg;

import java.util.List;

/**
 * 엔티티 기반의 다국어 번역 기능 인터페이스 
 * 
 * @author shortstop
 */
public interface IEntityTranslator {

	/**
	 * 이름이 entityName인 엔티티에 대한 필드명 columnName 다국어 번역
	 * 
	 * @param entityName
	 * @param columnName
	 * @return
	 */
	public String getTermByEntity(String entityName, String columnName);
	
	/**
	 * 이름이 entityName인 엔티티에 대한 필드명 (colNameList) 다국어 번역
	 * 
	 * @param entityName
	 * @param columnNameList
	 * @return
	 */
	public String getTermByEntity(String entityName, List<String> columnNameList);
	
}
