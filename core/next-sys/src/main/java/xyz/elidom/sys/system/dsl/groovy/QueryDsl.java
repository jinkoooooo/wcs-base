/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.dsl.groovy;

import java.util.List;
import java.util.Map;

import xyz.elidom.dbist.dml.Page;
import xyz.elidom.sys.system.dsl.BasicQueryDsl;

/**
 * Groovy Script Engine을 통해 실행되기 위한 Entity 기반의 Groovy용 SQL DSL
 * 
 * @author shortstop
 */
public class QueryDsl extends BasicQueryDsl {

	/**
	 * Default Constructor
	 */
	public QueryDsl() {
		super();
	}
	
	/**
	 * Constructor
	 * 
	 * @param entityClass
	 */
	public QueryDsl(Class<?> entityClass) {
		super(entityClass);
	}
	
	/**
	 * Constructor
	 * 
	 * @param entityClassName
	 */
	public QueryDsl(String entityClassName) {
		super(entityClassName);
	}
	
	/**
	 * create by HashMap
	 * 
	 * @param values
	 * @return
	 */
	public <T> T createBy(Map<String, Object> values) {
		return this.createByMap(values);
	}
	
	/**
	 * create batch by List of HashMap
	 * 
	 * @param params
	 * @return
	 */
	public <T> void createBatchBy(List<Map<String, Object>> params) {
		int count = params.size();
		for(int i = 0 ; i < count ; i++) {
			Map<String, Object> hash = (Map<String, Object>)params.get(i);
			this.createBy(hash);
		}
	}
	
	/**
	 * update by List of HashMap
	 * 
	 * @param data
	 * @param values
	 * @return
	 */
	public <T> T updateBy(T data, Map<String, Object> values) {
		return this.updateByMap(data, values);
	}
	
	/**
	 * select and update
	 * 
	 * @param key
	 * @param values
	 * @return
	 */
	public <T> T selectAndUpdateBy(Object key, Map<String, Object> values) {
		T data = this.find(key);
		this.updateByMap(data, values);
		return data;
	}
	
	/**
	 * delete by where and parameters (List)
	 * 
	 * @param data
	 * @param values
	 * @return
	 */
	public int deleteBy(String where, Object... values) {
		return this.deleteAllBy(where, values);
	}
	
	/**
	 * select by : selectStatement, whereStatement와 parameter로 쿼리를 구성하여 실행
	 * 
	 * @param selectStatement
	 * @param whereStatement
	 * @param orderStatement
	 * @param values
	 * @return
	 */
	public <T> List<T> selectBy(String selectStatement, String whereStatement, String orderStatement, Object... values) {
		return this.query(selectStatement, whereStatement, orderStatement, values);
	}
	
	/**
	 * query by : whereStatement와 parameter로 쿼리를 구성하여 실행
	 * 
	 * @param whereStatement
	 * @param orderStatement
	 * @param values
	 * @return
	 */
	public <T> List<T> queryBy(String whereStatement, String orderStatement, Object... values) {
		return this.query("*", whereStatement, orderStatement, values);
	}
	
	/**
	 * page by
	 * 
	 * @param selectStatement
	 * @param whereStatement ex) name = ? and description like ?
	 * @param orderStatement
	 * @param start
	 * @param limit
	 * @param values
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> Page<T> pageBy(String selectStatement, String whereStatement, String orderStatement, int start, int limit, Object... values) {
		StringBuffer sql = new StringBuffer();
		Map<String, Object> paramMap = this.buildQueryScript(sql, selectStatement, whereStatement, orderStatement, values);		
		return (Page<T>) this.getQueryDsl().selectPageBySql(sql.toString(), paramMap, this.getEntityClass(), start, limit);
	}
	
}
