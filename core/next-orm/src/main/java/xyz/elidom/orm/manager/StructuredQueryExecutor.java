/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.orm.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import xyz.elidom.exception.client.ElidomInvalidParamsException;
import xyz.elidom.orm.IDataSourceManager;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.orm.IStructuredQueryExecutor;
import xyz.elidom.orm.OrmMessageConstants;
import xyz.elidom.util.ValueUtil;

/**
 * IStructuredQueryExecutor 구현 
 * 
 * @author shortstop
 */
@Component
public class StructuredQueryExecutor implements IStructuredQueryExecutor {
	
	@Autowired
	IQueryManager defaultQueryManager;
	
	@Autowired
	IDataSourceManager dataSourceManager;

	@Override
	public IQueryManager getQueryManager(String dataSource) {
		if(ValueUtil.isEmpty(dataSource)) {
			return this.defaultQueryManager;
		} else {
			return this.dataSourceManager.getQueryManager(dataSource);
		}
	}

	@Override
	public List<?> queryForList(String dataSource, String query, Map<String, Object> params, Class<?> requiredType) {
		IQueryManager queryManager = this.getQueryManager(dataSource);
		List<?> list = queryManager.selectListBySql(query, params, requiredType, 0, 0);
		return list;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public List<Map> queryForMapList(String dataSource, String query, Map<String, Object> params) {
		IQueryManager queryManager = this.getQueryManager(dataSource);
		List<Map> list = queryManager.selectListBySql(query, params, Map.class, 0, 0);
		return list;
	}

	@SuppressWarnings("rawtypes")
	public Map<String, List> queryForMapByVar(String dataSource, String query, Map<String, Object> params, String variable) {
		IQueryManager queryManager = this.getQueryManager(dataSource);
		List<Map> list = queryManager.selectListBySql(query, params, Map.class, 0, 0);
		Map<String, List> result = new HashMap<String, List>();
		result.put(variable, list);
		return result;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Map<String, Object> queryForMapByKeyVal(String dataSource, String query, Map<String, Object> params, String keyField, String valueField) {
		IQueryManager queryManager = this.getQueryManager(dataSource);
		List<Map> list = queryManager.selectListBySql(query, params, Map.class, 0, 0);
		
		Map<String, Object> result = new HashMap<String, Object>();
		for(Map data : list) {
			if(!data.containsKey(keyField)) {
				throw new ElidomInvalidParamsException(OrmMessageConstants.INVALID_KEYS, ValueUtil.toList(keyField));
			}
			
			if(!data.containsKey(valueField)) {
				throw new ElidomInvalidParamsException(OrmMessageConstants.INVALID_KEYS, ValueUtil.toList(valueField));
			}
			
			result.put(data.get(keyField).toString(), data.get(valueField));
		}
		
		return result;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public List<Map> queryForObjectMapByKey(String dataSource, String query, Map<String, Object> params, String keyField) {
		IQueryManager queryManager = this.getQueryManager(dataSource);
		List<Map> list = queryManager.selectListBySql(query, params, Map.class, 0, 0);
		
		List<Map> result = new ArrayList<Map>();
		for(Map data : list) {
			if(!data.containsKey(keyField)) {
				throw new ElidomInvalidParamsException(OrmMessageConstants.INVALID_KEYS, ValueUtil.toList(keyField));
			}
			
			String keyData = data.remove(keyField).toString();
			Map<String, Object> obj = new HashMap<String, Object>();
			obj.put(keyData, data);
			result.add(obj);
		}
		
		return result;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Map<String, Object> queryForSingleByVar(String dataSource, String query, Map<String, Object> params, String variable) {
		IQueryManager queryManager = this.getQueryManager(dataSource);
		Map values = queryManager.selectBySql(query, params, Map.class);
		
		if(ValueUtil.isNotEmpty(variable)) {
			return ValueUtil.newMap(variable, values);
		} else {	
			return values;
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Map<String, Object> queryFormCustomStructure(String dataSource, Map<String, Map> structuredQuery) {
		// IQueryManager queryManager = this.getQueryManager(dataSource);
		// TODO Auto-generated method stub
		return null;
	}
}
