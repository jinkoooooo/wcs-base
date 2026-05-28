/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.orm;

import java.util.List;
import java.util.Map;

/**
 * 쿼리를 실행한 후 리턴 데이터를 구조적으로 변형하여 리턴할 수 있는 기능의 쿼리 실행기
 *    
 * @author shortstop
 */
public interface IStructuredQueryExecutor {

	/**
	 * QueryManager 리턴 
	 * 
	 * @param dataSource
	 * @return
	 */
	public IQueryManager getQueryManager(String dataSource);
	
	/**
	 * query를 params에 바인딩하여 쿼리를 수행한 후 requiredType 오브젝트 리스트로 리턴  
	 * 
	 * @param dataSource
	 * @param query
	 * @param params
	 * @param requiredType
	 * @return
	 */
	public List<?> queryForList(String dataSource, String query, Map<String, Object> params, Class<?> requiredType);

	/**
	 * query를 params에 바인딩하여 쿼리를 수행한 후 Map List로 리턴 
	 * 
	 * @param dataSource
	 * @param query
	 * @param params
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public List<Map> queryForMapList(String dataSource, String query, Map<String, Object> params);

	/**
	 * query를 params에 바인딩하여 쿼리를 수행한 후 List로 리턴하는데 List의 묶음을 variable로 구조화해서 리턴한다. 
	 * 
	 * @param dataSource
	 * @param query
	 * @param params
	 * @param variable
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public Map<String, List> queryForMapByVar(String dataSource, String query, Map<String, Object> params, String variable);
	
	/**
	 * key, value를 받아서 key 필드의 값이 Map의 key로 value 필드의 값이 Map의 value로 매핑된 구조의 데이터를 리턴 
	 * 
	 * @param dataSource
	 * @param query
	 * @param params
	 * @param keyField
	 * @param valueField
	 * @return
	 */
	public Map<String, Object> queryForMapByKeyVal(String dataSource, String query, Map<String, Object> params, String keyField, String valueField);
	
	/**
	 * query를 params에 바인딩하여 쿼리를 수행한 후 Map (key: String, value: Map)으로 리턴
	 * 
	 * @param dataSource
	 * @param query
	 * @param params
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public List<Map> queryForObjectMapByKey(String dataSource, String query, Map<String, Object> params, String key);
	
	/**
	 * 싱글 값을 리턴하는 query를 params에 바인딩하여 쿼리를 수행한 후 Map (key: variable, value: 싱글값)으로 리턴
	 * 
	 * @param dataSource
	 * @param query
	 * @param params
	 * @param variable
	 * @return
	 */
	public Map<String, Object> queryForSingleByVar(String dataSource, String query, Map<String, Object> params, String variable);
	
	/**
	 * query에 이미 리턴할 구조 정보가 포함되어 있는 쿼리를 수행하여 구조를 만들어서 리턴  
	 * 
	 * @param dataSource
	 * @param structuredQuery
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public Map<String, Object> queryFormCustomStructure(String dataSource, Map<String, Map> structuredQuery);
}
