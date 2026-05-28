/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.dsl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.groovy.runtime.GStringImpl;

import xyz.elidom.dbist.dml.Filter;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.dbist.metadata.Table;
import xyz.elidom.exception.server.ElidomScriptRuntimeException;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.SysMessageConstants;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.util.MessageUtil;
import xyz.elidom.sys.util.SysValueUtil;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ClassUtil;

/**
 * Query DSL의 상위 클래스
 * 
 * @author shortstop
 */
public class BasicQueryDsl extends QueryProcessor {

	/**
	 * Elidom Query Manager
	 */
	private IQueryManager queryDsl;
	/**
	 * Entity Class
	 */
	private Class<?> entityClass;	
	/**
	 * Table 메타 정보 
	 */
	private Table table;
	/**
	 * 모든 쿼리에 domainId를 자동으로 추가할 지 여부, 기본값 true
	 */
	private boolean isDomainRange = true;
	
	/**
	 * Default Constructor
	 */
	public BasicQueryDsl() {	
	}
	
	/**
	 * Default constructor
	 * 
	 * @param entityClassName
	 */
	public BasicQueryDsl(String entityClassName) {
		this.init(entityClassName);
	}
	
	/**
	 * Default constructor
	 * 
	 * @param entityClassName
	 */
	public BasicQueryDsl(Class<?> entityClass) {
		this.initByEntityClass(entityClass);
	}
	
	/**
	 * QueryDsl을 initialize
	 * 
	 * @param entityClassName
	 */
	public void initByEntityClass(Class<?> entityClass) {
		this.entityClass = entityClass;
		// Annotation으로 table 명을 찾은 다음 Table 얻기
		xyz.elidom.dbist.annotation.Table tableAnn = this.entityClass.getAnnotation(xyz.elidom.dbist.annotation.Table.class);
		if(SysValueUtil.isEmpty(tableAnn)) {
			throw new ElidomScriptRuntimeException(SysMessageConstants.IS_NOT_ENTITY, "Invalid entity name. [{0}]", MessageUtil.params(entityClass.getName()));
		}
		
		this.table = this.getQueryDsl().getTable(tableAnn.name());
		this.isDomainRange = ClassUtil.hasField(entityClass, SysConstants.ENTITY_FIELD_DOMAIN_ID);
	}
	
	/**
	 * QueryDsl을 initialize
	 * 
	 * @param entityClassName
	 */
	public void init(String entityClassName) {
		this.initByEntityClass(ClassUtil.forName(entityClassName));
	}

	/**
	 * @return the isDomainRange
	 */
	public boolean isDomainRange() {
		return isDomainRange;
	}

	/**
	 * @param isDomainRange the isDomainRange to set
	 */
	public void setDomainRange(boolean isDomainRange) {
		this.isDomainRange = isDomainRange;
	}

	/**
	 * @return the queryDsl
	 */
	public IQueryManager getQueryDsl() {
		if(this.queryDsl == null) {
			this.setQueryDsl(BeanUtil.get(IQueryManager.class));
		}
		
		return queryDsl;
	}

	/**
	 * @param queryDsl the queryDsl to set
	 */
	public void setQueryDsl(IQueryManager queryDsl) {
		this.queryDsl = queryDsl;
	}

	/**
	 * @return the entityClass
	 */
	public Class<?> getEntityClass() {
		return entityClass;
	}
	
	/**
	 * @return the entityClassName
	 */
	public String getEntityClassName() {
		return entityClass.getName();
	}

	/**
	 * @return the tableName
	 */
	public String getTableName() {
		return this.table.getName();
	}

	/**
	 * @return the table
	 */
	public Table getTable() {
		return table;
	}
	
	/**
	 * sql 실행 
	 * 
	 * @param sql
	 * @param paramMap
	 * @return
	 */
	public int executeSql(String sql, Map<String, Object> paramMap) {
		return this.queryDsl.executeBySql(sql, paramMap);
	}

	/**
	 * create one
	 * 
	 * @param data
	 * @return
	 */
	public <T> T create(T data) {
		this.queryDsl.insert(data);
		return data;
	}
	
	/**
	 * create one by map
	 * 
	 * @param keys
	 * @param values
	 * @return
	 */
	public <T> T createByKeyValues(String fieldNames, Object... values) {
		Map<String, Object> data = SysValueUtil.newMap(fieldNames, values);
		return this.createByMap(data);
	}
	
	/**
	 * create - only specified fieldNames
	 * 
	 * @param data
	 * @param fieldNames
	 * @return
	 */
	public <T> T createByPartialFields(T data, String... fieldNames) {
		this.queryDsl.insert(data, fieldNames);
		return data;
	}
	
	/**
	 * create by map
	 * 
	 * @param data
	 * @return
	 */
	public <T> T createByMap(Map<String, Object> data) {
		T record = this.newInstance();
		record = SysValueUtil.populate(data, record);
		this.queryDsl.insert(record);
		return record;
	}
	
	/**
	 * create batch - only specified fieldNames
	 * 
	 * @param list
	 * @param fieldNames
	 * @return
	 */
	public <T> void createBatchByPartialFields(List<T> list, String... fieldNames) {
		this.queryDsl.insertBatch(list, fieldNames);
	}
	
	/**
	 * create batch by map
	 * 
	 * @param list
	 * @return
	 */
	public <T> void createBatchByMap(List<Map<String, Object>> list) {
		for(Map<String, Object> data : list) {
			this.createByMap(data);
		}
	}
	
	/**
	 * create batch
	 * 
	 * @param list
	 */
	public <T> void createBatch(List<T> list) {
		this.queryDsl.insertBatch(list);
	}
		
	/**
	 * update one
	 * 
	 * @param data
	 * @return
	 */
	public <T> T update(T data) {
		this.queryDsl.update(data);
		return data;
	}
	
	/**
	 * update one by map
	 * 
	 * @param data
	 * @param values
	 * @return
	 */
	public <T> T updateByMap(T data, Map<String, Object> values) {
		data = SysValueUtil.populate(values, data);
		return this.update(data);
	}
	
	/**
	 * create by fieldNames
	 * 
	 * @param data
	 * @param fieldNames
	 * @return
	 */
	public <T> T updateByPartialFields(T data, String... fieldNames) {
		this.queryDsl.update(data, fieldNames);
		return data;
	}	
	
	/**
	 * update batch
	 * 
	 * @param list
	 * @return
	 */
	public <T> void updateBatch(List<T> list) {
		this.queryDsl.updateBatch(list);
	}
	
	/**
	 * update batch by list of map
	 * 
	 * @param dataList
	 * @param valueList
	 * @return
	 */
	public <T> void updateBatchByMap(List<T> dataList, List<Map<String, Object>> valueList) {
		for(int i = 0 ; i < dataList.size() ; i++) {
			T data = dataList.get(i);
			Map<String, Object> values = valueList.get(i);
			this.updateByMap(data, values);
		}
	}	
	
	/**
	 * update batch
	 * 
	 * @param list
	 * @return
	 */
	public <T> void updateBatchByPartialFields(List<T> list, String... fieldNames) {
		this.queryDsl.updateBatch(list, fieldNames);
	}	
	
	/**
	 * update all
	 * 
	 * @param setStatement
	 * @param whereStatement
	 * @return
	 */
	public int updateAll(String setStatement, String whereStatement) {
		StringBuffer sql = new StringBuffer("update ");
		sql.append(this.getTableName()).append(" set ").append(setStatement);
		sql.append(" where ").append(this.getDefaultStatement()).append(" and ").append(whereStatement);
		return this.queryDsl.executeBySql(sql.toString(), null);
	}
	
	/**
	 * Update or Insert
	 * 
	 * @param data
	 * @return
	 */
	public void upsert(Object data) {
		this.queryDsl.upsert(data);
	}
	
	/**
	 * Update or Insert
	 * @param clazz
	 * @param data
	 */
	public void upsert(Class<?> clazz, Object data) {
		this.queryDsl.upsert(clazz, data);
	}
	
	/**
	 * delete one
	 * 
	 * @param data
	 */
	public <T> void delete(T data) {
		this.queryDsl.delete(data);
	}
	
	/**
	 * delete one by key data
	 * 
	 * @param key
	 */
	public <T> void deleteByKey(T key) {
		this.queryDsl.delete(this.entityClass, key);
	}
	
	/**
	 * delete batch
	 * 
	 * @param list
	 */
	public <T> void deleteBatch(List<T> list) {
		this.queryDsl.deleteBatch(list);
	}
	
	/**
	 * delete batch by key data
	 * 
	 * @param key
	 */
	public <T> void deleteBatchByKey(List<T> keys) {
		for(T key : keys) {
			this.deleteByKey(key);
		}
	}	
	
	/**
	 * delete all by
	 * 
	 * @param where
	 * @param params
	 * @return
	 */
	public int deleteAllBy(String where, Object...params) {
		StringBuffer sql = new StringBuffer("delete from ").append(this.getTableName());
		Map<String, Object> paramMap = buildWhereScript(sql, where, params);
		return this.queryDsl.executeBySql(sql.toString(), paramMap);
	}
	
	/**
	 * find one
	 * 
	 * @param key
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> T find(Object key) {
		Object value;
		if (key instanceof Integer) {
			value = SysValueUtil.toInteger(key);
		} else if (key instanceof GStringImpl) {
			value = SysValueUtil.toString(key);
		} else {
			value = key;
		}
		return (T) this.queryDsl.select(this.entityClass, value);
	}
	
	/**
	 * find by 
	 * 
	 * @param name
	 * @param value
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> T findBy(String name, Object value) {
		Query query = this.getDefaultQuery();
		query.addFilter(new Filter(name, value));
		return (T) this.queryDsl.selectByCondition(this.entityClass, query);
	}
	
	/**
	 * list by
	 * 
	 * @param name
	 * @param value
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> listBy(String name, Object value) {
		Query query = this.getDefaultQuery();
		query.addFilter(new Filter(name, value));
		return (List<T>)this.queryDsl.selectList(this.entityClass, query);
	}
	
	/**
	 * query by : whereStatement와 parameter로 쿼리를 구성하여 실행
	 * 
	 * @param selectStatement
	 * @param whereStatement
	 * @param orderStatement
	 * @param params
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> query(String selectStatement, String whereStatement, String orderStatement, Object... params) {
		StringBuffer sql = new StringBuffer();
		Map<String, Object> paramMap = this.buildQueryScript(sql, selectStatement, whereStatement, orderStatement, params);
		return (List<T>) this.queryDsl.selectListBySql(sql.toString(), paramMap, this.entityClass, 0, 0);
	}
	
	/**
	 * 조건 없이 max 몇 개 조회 
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> all(int maxCount) {
		if(maxCount == 0) {
			maxCount = 100;
		}
		
		StringBuffer sql = new StringBuffer("select * from ").append(this.getTableName()).append(" where ").append(this.getDefaultStatement());
		return (List<T>)this.queryDsl.selectListBySql(sql.toString(), null, this.entityClass, 0, maxCount);
	}
	
	/**
	 * 조건에 맞는 카운트를 조회 
	 * 
	 * @param whereStatement
	 * @return
	 */
	public int count(String whereStatement) {
		String sql = this.createSql(null, whereStatement, null);
		return this.queryDsl.selectSizeBySql(sql, null);
	}
	
	/**
	 * 첫 번째 값을 조회
	 * 
	 * @param count
	 * @param where
	 * @param order
	 * @return
	 */
	public <T> T first(String where, String order) {
		List<T> list = this.firstMax(1, where, order);
		return SysValueUtil.isEmpty(list) ? null : list.get(0);
	}
	
	/**
	 * 첫 몇 개 조회 
	 * 
	 * @param maxCount
	 * @param where
	 * @param order
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> firstMax(int maxCount, String where, String order) {
		List<T> list = (List<T>)this.page(null, where, order, 1, maxCount).getList();
		return SysValueUtil.isEmpty(list) ? null : list;
	}
	
	/**
	 * 마지막 값을 조회 
	 * 
	 * @param where
	 * @param order
	 * @return
	 */
	public <T> T last(String where, String order) {
		List<T> list = this.lastMax(1, where, order);
		return SysValueUtil.isEmpty(list) ? null : list.get(0);
	}
	
	/**
	 * 마지막 몇 개 조회 
	 * 
	 * @param maxCount
	 * @param where
	 * @param order
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> lastMax(int maxCount, String where, String order) {
		int count = this.count(where);
		List<T> list = (List<T>)this.page(null, where, order, count - maxCount, maxCount).getList();
		return SysValueUtil.isEmpty(list) ? null : list;
	}	
	
	/**
	 * find list
	 * 
	 * @param selectStatement
	 * @param whereStatement
	 * @param orderStatement
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> list(String selectStatement, String whereStatement, String orderStatement) {
		String sql = this.createSql(selectStatement, whereStatement, orderStatement);
		return (List<T>) this.queryDsl.selectListBySql(sql, null, this.entityClass, 0, 0);
	}
	
	/**
	 * pagination
	 * 
	 * @param selectStatement
	 * @param whereStatement
	 * @param orderStatement
	 * @param start
	 * @param limit
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> Page<T> page(String selectStatement, String whereStatement, String orderStatement, int start, int limit) {
		String sql = this.createSql(selectStatement, whereStatement, orderStatement);
		return (Page<T>) this.queryDsl.selectPageBySql(sql, null, this.entityClass, start, limit);
	}
	
	/**
	 * sql script로 쿼리 실행
	 * 
	 * @param sql
	 * @param currentPage
	 * @param limit
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public List<Map> selectSql(String sql, int currentPage, int limit) {
		return (List<Map>) this.queryDsl.selectListBySql(sql, null, Map.class, currentPage, limit);
	}
	
	/**
	 * sql script로 쿼리 실행하여 number형으로 리턴
	 * 
	 * @param sql
	 * @param currentPage
	 * @param limit
	 * @return
	 */
	public Integer selectNumberBySql(String sql) {
		Integer result = this.queryDsl.selectBySql(sql, null, Integer.class);
		return result;
	}
	
	/**
	 * create sql statement 
	 * 
	 * @param select
	 * @param where
	 * @param order
	 * @param params
	 * @return
	 */
	public String createSql(String select, String where, String order, Object...params) {
		StringBuffer sql = new StringBuffer();
		this.buildQueryScript(sql, select, where, order);
		return sql.toString();
	}
	
	/************************************************************************************************
	 * 										Query Processs Implementation
	 ************************************************************************************************/
	
	/**
	 * Query Processor - process
	 */
	@Override
	public Object process() {
		StringBuffer sql = new StringBuffer();
		Map<String, Object> paramMap = this.buildQueryScript(sql, this._select, this._where, this._order, this._params);
		Object result = null;
		
		if(this._limit == 0 && this._currentPage == 0) {
			result = this.queryDsl.selectListBySql(sql.toString(), paramMap, this.entityClass, this._currentPage, this._limit);			
		} else {
			result = this.queryDsl.selectPageBySql(sql.toString(), paramMap, this.entityClass, this._currentPage, this._limit);
		}
		
		// 쿼리 파라미터 모두 초기화
		super.clearQuery();
		return result;
	}	
	
	/************************************************************************************************
	 * 										PRIVATE METHODS
	 ************************************************************************************************/
	/**
	 * domain 범위내 쿼리인지 체크하여 where문의 처음에 나오는 조건을 리턴한다.
	 * 
	 * @return
	 */
	protected String getDefaultStatement() {
		if(!this.isDomainRange) {
			return " 1 = 1 ";
		} else {
			return " domain_id = " + Domain.currentDomain().getId();
		}
	}
	
	/**
	 * domain 범위내 쿼리인지 체크하여 where문의 처음에 나오는 조건을 리턴한다.
	 * 
	 * @return
	 */
	protected Query getDefaultQuery() {
		Query query = new Query();
		
		if(this.isDomainRange) {
			query.addFilter(new Filter("domainId", Domain.currentDomain().getId()));			
		}
		
		return query;
	}

	/**
	 * create new instance of entity class 
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected <T> T newInstance() {
		return (T) ClassUtil.newInstance(entityClass);
	}
	
	/**
	 * select, where, order, parameters로 최종 쿼리를 만들고 parameter map을 리턴
	 * 
	 * @param sql 최종 쿼리가 담긴다.
	 * @param select
	 * @param where
	 * @param order
	 * @param params
	 * @return
	 */
	protected Map<String, Object> buildQueryScript(StringBuffer sql, String select, String where, String order, Object...params) {
		this.checkValidWhereStatement(where, params != null ? params.length : 0);
		sql.append("select ").append(SysValueUtil.isEmpty(select) ? "*" : select);
		sql.append(" from ").append(this.getTableName());
		Map<String, Object> paramMap = this.buildWhereScript(sql, where, params);
		
		if(!SysValueUtil.isEmpty(order)) {
			sql.append(" order by ").append(order);
		}
		
		return paramMap;
	}
	
	/**
	 * where 문에 파라미터 매핑한 후 where문을 만들고 매핑 결과를 리턴한다.
	 *  
	 * @param sql
	 * @param where
	 * @param params
	 * @return
	 */
	protected Map<String, Object> buildWhereScript(StringBuffer sql, String where, Object...params) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		int idx = 1;
		int lastIdx = 0;
		sql.append(" where ");
		
		if(!SysValueUtil.isEmpty(params)) {
			sql.append(this.getDefaultStatement()).append(" and ");
			int thisIdx = 0;
			for(int j = 0 ; j < params.length ; j++) {
				Object param = params[j];
				String paramName = "param_" + idx++;
				thisIdx = where.indexOf("?", lastIdx);
				sql.append(where.substring(lastIdx, thisIdx)).append(":" + paramName);
				paramMap.put(paramName, param);
				lastIdx = thisIdx + 1;
			}
			
			sql.append(where.substring(thisIdx + 1));
			
		} else {
			sql.append(where);
		}
		
		return paramMap;
	}
	
	/**
	 * where문의 ? 개수와 파라미터 개수가 일치하는 지 체크 
	 * 
	 * @param whereStatement
	 * @param paramCount
	 * @return
	 */
	protected void checkValidWhereStatement(String whereStatement, int paramCount) {
		int questionCount = 0;
		
		if(!SysValueUtil.isEmpty(whereStatement)) {
			for(int i = 0 ; i < whereStatement.length() ; i++) {
				char ch = whereStatement.charAt(i);
				if(ch == '?') {
					questionCount++;
				}
			}
		}
		
		if(paramCount != questionCount) {
			List<String> params = MessageUtil.params(Integer.toString(paramCount), Integer.toString(questionCount));
			throw new ElidomScriptRuntimeException(SysMessageConstants.MISMATCH_PARAM_COUNT, "Invalid query - Parameter count mismatch.[{0}:{1}]", params);
		}
	}
	
}
