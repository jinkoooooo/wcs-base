/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.dsl.ruby;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jruby.RubyArray;
import org.jruby.RubyHash;

import xyz.elidom.dbist.dml.Page;
import xyz.elidom.sys.system.dsl.BasicQueryDsl;

/**
 * Ruby Script Engine을 통해 실행되기 위한 Entity 기반의 Ruby용 SQL DSL
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
	 * @param entityClassName
	 */
	public QueryDsl(String entityClassName) {
		super(entityClassName);
	}
	
	/**
	 * create by RubyHash
	 * 
	 * @param values
	 * @return
	 */
	public <T> T createBy(RubyHash values) {
		Map<String, Object> map = this.convertToMap(values);
		return this.createByMap(map);
	}
	
	/**
	 * create batch by RubyArray of RubyHash
	 * 
	 * @param params
	 * @return
	 */
	public <T> void createBatchBy(org.jruby.RubyArray params) {
		int count = params.size();
		for(int i = 0 ; i < count ; i++) {
			RubyHash hash = (RubyHash)params.get(i);
			this.createBy(hash);	
		}
	}
	
	/**
	 * update by RubyHash
	 * 
	 * @param data
	 * @param values
	 * @return
	 */
	public <T> T updateBy(T data, RubyHash values) {
		Map<String, Object> map = this.convertToMap(values);
		return this.updateByMap(data, map);
	}
	
	/**
	 * delete by where and parameters (RubyArray)
	 * 
	 * @param data
	 * @param values
	 * @return
	 */
	public <T> int deleteBy(String where, RubyArray values) {
		Object[] params = this.convertToArray(values);
		return this.deleteAllBy(where, params);
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
	public <T> List<T> selectBy(String selectStatement, String whereStatement, String orderStatement, RubyArray values) {
		Object[] jParams = this.convertToArray(values);
		return this.query(selectStatement, whereStatement, orderStatement, jParams);
	}
	
	/**
	 * query by : whereStatement와 parameter로 쿼리를 구성하여 실행
	 * 
	 * @param whereStatement
	 * @param orderStatement
	 * @param values
	 * @return
	 */
	public <T> List<T> queryBy(String whereStatement, String orderStatement, RubyArray values) {
		Object[] jParams = this.convertToArray(values);
		return this.query("*", whereStatement, orderStatement, jParams);
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
	public <T> Page<T> pageBy(String selectStatement, String whereStatement, String orderStatement, int start, int limit, RubyArray values) {
		StringBuffer sql = new StringBuffer();
		Object[] params = this.convertToArray(values);
		Map<String, Object> paramMap = this.buildQueryScript(sql, selectStatement, whereStatement, orderStatement, params);		
		return (Page<T>) this.getQueryDsl().selectPageBySql(sql.toString(), paramMap, this.getEntityClass(), start, limit);
	}	
	
	/**
	 * RubyArray를 Object[]로 변환 
	 * 
	 * @param values
	 * @return
	 */
	protected <T> Object[] convertToArray(RubyArray values) {
		Object[] arr = new Object[values.size()];
		for(int i = 0 ; i < values.size() ; i++) {
			arr[i] = values.get(i);
		}
		
		return arr;
	}
	
	/**
	 * RubyHash를 Map으로 변환 
	 * 
	 * @param hash
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected Map<String, Object> convertToMap(RubyHash hash) {
		Map<String, Object> map = new HashMap<String, Object>();
		Iterator<String> iter = hash.keySet().iterator();
		
		while(iter.hasNext()) {
			String key = iter.next();
			Object value = hash.get(key);
			map.put(key, value);
		}
		
		return map;
	}

}
