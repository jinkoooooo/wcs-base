/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.orm.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xyz.elidom.dbist.dml.Dml;
import xyz.elidom.dbist.dml.Filter;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.dbist.dml.impl.DmlJdbc2;
import xyz.elidom.dbist.metadata.Table;
import xyz.elidom.dbist.processor.Preprocessor;
import xyz.elidom.exception.ElidomException;
import xyz.elidom.exception.client.ElidomRecordNotFoundException;
import xyz.elidom.exception.server.ElidomDatabaseException;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.entity.basic.IEntityHook;
import xyz.elidom.orm.model.ReturnDefaultMessage;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.util.EntityUtil;
import xyz.elidom.util.ClassUtil;
import xyz.elidom.util.FormatUtil;
import xyz.elidom.util.ValueUtil;

/**
 * IQueryManager 구현 부모 클래스
 * 
 * @author shortstop
 */
public abstract class AbstractQueryManager implements IQueryManager {
	
	/**
	 * dml
	 */
	public abstract Dml getDml();
	
	@Override
	public String getDbType() {
		return this.getDml().getDbType();
	}

	@Override
	public Class<?> getClass(String tableName) {
		return this.getDml().getClass();
	}

	@Override
	public Table getTable(Object entityObj) {
		return this.getDml().getTable(entityObj);
	}

	@Override
	public Table getTable(String name) {
		return this.getDml().getTable(name);
	}

	@Override
	public void setPreprocessor(Preprocessor preprocessor) {
		this.getDml().setPreprocessor(preprocessor);
	}

	@Override
	public <T> T select(T data) {
		return select(false, data);
	}
	
	@Override
	public <T> T select(boolean withException, T data) {
		try {
			this.beforeFind(data);
			data = this.getDml().select(data);
			this.afterFind(data);
			
		} catch (ElidomException ee) {
			throw ee;
			
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
		
		if (withException && data == null) {
			throw new ElidomRecordNotFoundException();
		}
		
		return data;
	}

	@Override
	public <T> T selectWithLock(T data) {
		try {
			this.beforeFind(data);
			data = this.getDml().selectWithLock(data);
			this.afterFind(data);
			return data;
			
		} catch (ElidomException ee) {
			throw ee;
			
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public <T> T select(Class<T> clazz, Object... pkCondition) {
		return select(false, clazz, pkCondition);
	}

	@Override
	public <T> T select(boolean withException, Class<T> clazz, Object... pkCondition) {
		T data = null;
		
		try {
			if (EntityUtil.isComplexKeyType(clazz) && pkCondition.length == 1) {
				data = clazz.getDeclaredConstructor().newInstance();
				ClassUtil.setFieldValue(data, SysConstants.ENTITY_FIELD_ID, ValueUtil.toString(pkCondition[0]));

				this.beforeFind(data);

				data = this.getDml().select(data);
			} else {
				data = this.getDml().select(clazz, pkCondition);
			}

			this.afterFind(data);

		} catch (ElidomException ee) {
			throw ee;

		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}

		if (withException && data == null) {
			throw new ElidomRecordNotFoundException();
		}

		return data;
	}

	@Override
	public <T> T selectWithLock(Class<T> clazz, Object... pkCondition) {
		try {
			T data = this.getDml().selectWithLock(clazz, pkCondition);
			this.afterFind(data);
			return data;
			
		} catch (ElidomException ee) {
			throw ee;
			
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}
	
	/**
	 * Select a data row from the database table mapped to T class by primary key fields' value of data parameter.<br>
	 * The data parameter must be set primary key fields' value.
	 * 
	 * @param <T>
	 *            The object class mapped to a database table
	 * @param data
	 *            The data wanted to select
	 * @return The data selected
	 */
	@Override
	public <T> T simpleSelect(T data) {
		return this.simpleSelect(false, data);
	}
	
	/**
	 * Select Simply With Exception
	 * 
	 * @param data
	 * @param withException
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T simpleSelect(boolean withException, T data) {
		try {
			this.beforeFind(data);
			Query query = new Query();
			query.setUnselect(OrmConstants.ENTITY_FIELD_DEFAULT_IGNORED_LIST);
			Table table = this.getTable(data);
			String[] pkFields = table.getPkFieldNames();
			for(int i = 0 ; i < pkFields.length ; i++) {
				query.addFilter(new Filter(pkFields[i], ClassUtil.getFieldValue(data, pkFields[i])));
			}
			
			data = (T)this.selectByCondition(data.getClass(), query);
			
		} catch(Exception e) {
			throw new ElidomDatabaseException(e);
		}
		
		if (data == null && withException) {
			throw new ElidomRecordNotFoundException();
		}
		
		this.afterFind(data);
		return data;
	}
	
	/**
	 * Select Simply With Exception
	 * 
	 * @param withException
	 * @param clazz
	 * @param pkCondition
	 * @return
	 */
	@Override
	public <T> T simpleSelect(boolean withException, Class<T> clazz, Object... pkCondition) {
		T data = null;
		
		try {
			Query query = new Query();
			query.setUnselect(OrmConstants.ENTITY_FIELD_DEFAULT_IGNORED_LIST);
			Table table = this.getTable(ClassUtil.newInstance(clazz));
			String[] pkFields = table.getPkFieldNames();
			for(int i = 0 ; i < pkFields.length ; i++) {
				query.addFilter(new Filter(pkFields[i], pkCondition[i]));
			}
			
			data = (T) this.selectByCondition(clazz, query);
			
		} catch(Exception e) {
			throw new ElidomDatabaseException(e);
		}
		
		if (withException && data == null) {
			throw new ElidomRecordNotFoundException();
		}
		
		this.afterFind(data);
		return data;
	}
	
	@Override
	public <T> T selectByCondition(Class<T> clazz, Object condition) {
		return selectByCondition(false, clazz, condition);
	}
	
	@Override
	public <T> T selectByCondition(boolean withException, Class<T> clazz, Object condition) {
		T data = null;
		
		try {
			this.beforeFind(condition);
			data = this.getDml().selectByCondition(clazz, condition);
			this.afterFind(data);
			
		} catch (ElidomException ee) {
			throw ee;
			
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}

		if (withException && data == null) {
			throw new ElidomRecordNotFoundException();
		}
		
		return data;
	}

	@Override
	public <T> T selectByConditionWithLock(Class<T> clazz, Object condition) {
		try {
			T data = this.getDml().selectByConditionWithLock(clazz, condition);
			this.afterFind(data);
			return data;
			
		} catch (ElidomException ee) {
			throw ee;
			
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public <T> T select(String tableName, Object pkCondition, Class<T> requiredType) {
		return select(false, tableName, pkCondition, requiredType);
	}

	@Override
	public <T> T select(boolean withException, String tableName, Object pkCondition, Class<T> requiredType) {
		T data = null;
		try {
			data = this.getDml().select(tableName, pkCondition, requiredType);
			this.afterFind(data);
			
		} catch (ElidomException ee) {
			throw ee;
			
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}

		if (withException && data == null) {
			throw new ElidomRecordNotFoundException();
		}
		
		return data;
	}

	@Override
	public <T> T selectWithLock(String tableName, Object pkCondition, Class<T> requiredType) {
		try {
			T data = this.getDml().selectWithLock(tableName, pkCondition, requiredType);
			this.afterFind(data);
			return data;
			
		} catch (ElidomException ee) {
			throw ee;
		
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public <T> T selectByCondition(String tableName, Object condition, Class<T> requiredType) {
		try {
			T data = this.getDml().selectByCondition(tableName, condition, requiredType);
			this.afterFind(data);
			return data;
			
		} catch (ElidomException ee) {
			throw ee;
		
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public <T> T selectByConditionWithLock(String tableName, Object condition, Class<T> requiredType) {
		try {
			T data = this.getDml().selectByCondition(tableName, condition, requiredType);
			this.afterFind(data);
			return data;
			
		} catch (ElidomException ee) {
			throw ee;
			
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public <T> T selectByQl(String ql, Map<String, ?> paramMap, Class<T> requiredType) {
		try {
			T data = this.getDml().selectByQl(ql, paramMap, requiredType);
			this.afterFind(data);
			return data;
			
		} catch (ElidomException ee) {
			throw ee;
			
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}		
	}

	@Override
	public <T> T selectByQlPath(String qlPath, Map<String, ?> paramMap, Class<T> requiredType) {
		try {
			T data = this.getDml().selectByQlPath(qlPath, paramMap, requiredType);
			this.afterFind(data);
			return data;
			
		} catch (ElidomException ee) {
			throw ee;
			
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public <T> T selectBySql(String sql, Map<String, ?> paramMap, Class<T> requiredType) {
		try {
			T data = this.getDml().selectBySql(sql, paramMap, requiredType);
			this.afterFind(data);
			return data;
			
		} catch (ElidomException ee) {
			throw ee;
			
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public <T> T selectBySqlPath(String sqlPath, Map<String, ?> paramMap, Class<T> requiredType) {
		try {
			T data = this.getDml().selectBySqlPath(sqlPath, paramMap, requiredType);
			this.afterFind(data);
			return data;
			
		} catch (ElidomException ee) {
			throw ee;
			
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public void insert(Object data) {
		this.beforeCreate(data);
		
		try {
			this.getDml().insert(data);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
		
		this.afterCreate(data);
	}

	@Override
	public void insertBatch(List<?> list) {
		this.beforeCreateList(list);
		
		try {
			this.getDml().insertBatch(list);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
		
		this.afterCreateList(list);
	}

	@Override
	public void insert(Object data, String... fieldNames) {
		this.beforeCreate(data);
		
		try {
			this.getDml().insert(data, fieldNames);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
		
		this.afterCreate(data);
	}

	@Override
	public void insertBatch(List<?> list, String... fieldNames) {
		this.beforeCreateList(list);
		
		try {
			this.getDml().insertBatch(list, fieldNames);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
		
		this.afterCreateList(list);
	}

	@Override
	public <T> T insert(Class<T> clazz, Object data) {
		try {
			this.beforeCreate(data);
			T createdOne = this.getDml().insert(clazz, data);
			this.afterCreate(createdOne);
			return createdOne;
			
		} catch (ElidomException ee) {
			throw ee;
			
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public void insertBatch(Class<?> clazz, List<?> list) {
		this.beforeCreateList(list);
		
		try {
			this.getDml().insertBatch(clazz, list);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
		
		this.afterCreateList(list);
	}

	@Override
	public void insert(Class<?> clazz, Object data, String... fieldNames) {
		this.beforeCreate(data);
		
		try {
			this.getDml().insert(clazz, data, fieldNames);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
		
		this.afterCreate(data);
	}

	@Override
	public void insertBatch(Class<?> clazz, List<?> list, String... fieldNames) {
		this.beforeCreateList(list);
		
		try {
			this.getDml().insertBatch(clazz, list, fieldNames);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
		
		this.afterCreateList(list);
	}

	@Override
	public void insert(String tableName, Object data) {
		this.beforeCreate(data);
		
		try {
			this.getDml().insert(tableName, data);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
		
		this.afterCreate(data);
	}

	@Override
	public void insertBatch(String tableName, List<?> list) {
		this.beforeCreateList(list);
		
		try {
			this.getDml().insertBatch(tableName, list);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
		
		this.afterCreateList(list);
	}

	@Override
	public void insert(String tableName, Object data, String... fieldNames) {
		this.beforeCreate(data);
		
		try {
			this.getDml().insert(tableName, data, fieldNames);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
		
		this.afterCreate(data);
	}

	@Override
	public void insertBatch(String tableName, List<?> list, String... fieldNames) {
		this.beforeCreateList(list);
		
		try {
			this.getDml().insertBatch(tableName, list, fieldNames);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
		
		this.afterCreateList(list);
	}

	@Override
	public void update(Object data) {
		this.beforeUpdate(data);
		
		try {
			this.getDml().update(data);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
		
		this.afterUpdate(data);
	}

	@Override
	public void updateBatch(List<?> list) {
		this.beforeUpdateList(list);
		
		try {
			this.getDml().updateBatch(list);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
		
		this.afterUpdateList(list);
	}

	@Override
	public void update(Object data, String... fieldNames) {
		this.beforeUpdate(data);
		
		try {
			this.getDml().update(data, fieldNames);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
		
		this.afterUpdate(data);
	}

	@Override
	public void updateBatch(List<?> list, String... fieldNames) {
		this.beforeUpdateList(list);
		
		try {
			this.getDml().updateBatch(list, fieldNames);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
		
		this.afterUpdateList(list);
	}

	@Override
	public <T> T update(Class<T> clazz, Object data) {
		try {
			this.beforeUpdate(data);
			T updatedOne = this.getDml().update(clazz, data);
			this.afterUpdate(updatedOne);
			return updatedOne;
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public void updateBatch(Class<?> clazz, List<?> list) {
		this.beforeUpdateList(list);
		
		try {
			this.getDml().updateBatch(clazz, list);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
		
		this.afterUpdateList(list);
	}

	@Override
	public <T> T update(Class<T> clazz, Object data, String... fieldNames) {
		try {
			this.beforeUpdate(data);
			T updatedOne = this.getDml().update(clazz, data, fieldNames);
			this.afterUpdate(updatedOne);
			return updatedOne;
			
		} catch (ElidomException ee) {
			throw ee;
			
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public void updateBatch(Class<?> clazz, List<?> list, String... fieldNames) {
		this.beforeUpdateList(list);
		
		try {
			this.getDml().updateBatch(clazz, list, fieldNames);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
		
		this.afterUpdateList(list);
	}

	@Override
	public void update(String tableName, Object data) {
		this.beforeUpdate(data);
		
		try {
			this.getDml().update(tableName, data);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
		
		this.afterUpdate(data);
	}

	@Override
	public void updateBatch(String tableName, List<?> list) {
		try {
			this.beforeUpdateList(list);
			this.getDml().updateBatch(tableName, list);
			this.afterUpdateList(list);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public void update(String tableName, Object data, String... fieldNames) {
		this.beforeUpdate(data);
		
		try {
			this.getDml().update(tableName, data, fieldNames);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
		
		this.afterUpdate(data);
	}

	@Override
	public void updateBatch(String tableName, List<?> list, String... fieldNames) {
		this.beforeUpdateList(list);
		
		try {
			this.getDml().updateBatch(tableName, list, fieldNames);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
		
		this.afterUpdateList(list);
	}

	@Override
	public void upsert(Object data) {
		if (this.isInsert(data)) {
			this.insert(data);
		} else {
			this.update(data);
		}
	}

	@Override
	public void upsertBatch(List<?> list) {
		List<Object> insertList = new ArrayList<Object>();
		List<Object> updateList = new ArrayList<Object>();

		for (Object data : list) {
			if (this.isInsert(data)) {
				insertList.add(data);
			} else {
				updateList.add(data);
			}
		}

		if(!insertList.isEmpty()) {
			this.insertBatch(insertList);
		}
		
		if(!updateList.isEmpty()) {
			this.updateBatch(updateList);
		}
	}

	@Override
	public void upsert(Object data, String... fieldNames) {
		if (this.isInsert(data)) {
			this.insert(data, fieldNames);
		} else {
			this.update(data, fieldNames);
		}
	}

	@Override
	public void upsertBatch(List<?> list, String... fieldNames) {
		List<Object> insertList = new ArrayList<Object>();
		List<Object> updateList = new ArrayList<Object>();

		for (Object data : list) {
			if (this.isInsert(data)) {
				insertList.add(data);
			} else {
				updateList.add(data);
			}
		}

		if(!insertList.isEmpty()) {
			this.insertBatch(insertList, fieldNames);
		}
		
		if(!updateList.isEmpty()) {
			this.updateBatch(updateList, fieldNames);
		}
	}

	@Override
	public <T> T upsert(Class<T> clazz, Object data) {
		if(this.isInsert(data)) {
			return this.insert(clazz, data);
		} else {
			return this.update(clazz, data);
		}
	}

	@Override
	public void upsertBatch(Class<?> clazz, List<?> list) {
		List<Object> insertList = new ArrayList<Object>();
		List<Object> updateList = new ArrayList<Object>();

		for (Object data : list) {
			if (this.isInsert(data)) {
				insertList.add(data);
			} else {
				updateList.add(data);
			}
		}

		if(!insertList.isEmpty()) {
			this.insertBatch(clazz, insertList);
		}
		
		if(!updateList.isEmpty()) {
			this.updateBatch(clazz, updateList);
		}
	}

	@Override
	public void upsert(Class<?> clazz, Object data, String... fieldNames) {
		if(this.isInsert(data)) {
			this.insert(clazz, data);
		} else {
			this.update(clazz, data);
		}
	}

	@Override
	public void upsertBatch(Class<?> clazz, List<?> list, String... fieldNames) {
		List<Object> insertList = new ArrayList<Object>();
		List<Object> updateList = new ArrayList<Object>();

		for (Object data : list) {
			if (this.isInsert(data)) {
				insertList.add(data);
			} else {
				updateList.add(data);
			}
		}

		if(!insertList.isEmpty()) {
			this.insertBatch(clazz, insertList, fieldNames);
		}
		
		if(!updateList.isEmpty()) {
			this.updateBatch(clazz, updateList, fieldNames);
		}
	}

	@Override
	public void upsert(String tableName, Object data) {
		if(this.isInsert(data)) {
			this.insert(tableName, data);
		} else {
			this.update(tableName, data);
		}
	}

	@Override
	public void upsertBatch(String tableName, List<?> list) {
		List<Object> insertList = new ArrayList<Object>();
		List<Object> updateList = new ArrayList<Object>();

		for (Object data : list) {
			if (this.isInsert(data)) {
				insertList.add(data);
			} else {
				updateList.add(data);
			}
		}

		if(!insertList.isEmpty()) {
			this.insertBatch(tableName, insertList);
		}
		
		if(!updateList.isEmpty()) {
			this.updateBatch(tableName, updateList);
		}
	}

	@Override
	public void upsert(String tableName, Object data, String... fieldNames) {
		if(this.isInsert(data)) {
			this.insert(tableName, data, fieldNames);
		} else {
			this.update(tableName, data, fieldNames);
		}
	}
	
	@Override
	public void upsertBatch(String tableName, List<?> list, String... fieldNames) {
		List<Object> insertList = new ArrayList<Object>();
		List<Object> updateList = new ArrayList<Object>();

		for (Object data : list) {
			if (this.isInsert(data)) {
				insertList.add(data);
			} else {
				updateList.add(data);
			}
		}

		if(!insertList.isEmpty()) {
			this.insertBatch(tableName, insertList, fieldNames);
		}
		
		if(!updateList.isEmpty()) {
			this.updateBatch(tableName, updateList, fieldNames);
		}
	}

	@Override
	public void delete(Object data) {
		this.beforeDelete(data);
		
		try {
			this.getDml().delete(data);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
		
		this.afterDelete(data);
	}

	@Override
	public void deleteBatch(List<?> list) {
		this.beforeDeleteList(list);
		
		try {
			this.getDml().deleteBatch(list);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
		
		this.afterDeleteList(list);
	}

	@Override
	public <T> T delete(Class<T> clazz, Object... pkCondition) {
		try {
			T deletedOne = this.getDml().delete(clazz, pkCondition);
			this.afterDelete(deletedOne);
			return deletedOne;
			
		} catch (ElidomException ee) {
			throw ee;
			
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public void deleteBatch(Class<?> clazz, List<?> list) {
		this.beforeDeleteList(list);
		
		try {
			this.getDml().deleteBatch(clazz, list);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
		
		this.afterDeleteList(list);
	}

	@Override
	public <T> T deleteByCondition(Class<T> clazz, Object condition) {
		try {
			T deletedOne = this.getDml().deleteByCondition(clazz, condition);
			this.afterDelete(deletedOne);
			return deletedOne;
			
		} catch (ElidomException ee) {
			throw ee;
			
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public void delete(String tableName, Object... pkCondition) {
		try {
			this.getDml().delete(tableName, pkCondition);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
		
		this.afterDelete(pkCondition);
	}

	@Override
	public void deleteBatch(String tableName, List<?> list) {
		this.beforeDeleteList(list);
		
		try {
			this.getDml().deleteBatch(tableName, list);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
		
		this.afterDeleteList(list);
	}

	@Override
	public void deleteByCondition(String tableName, Object condition) {
		try {
			this.getDml().deleteByCondition(tableName, condition);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
		
		this.afterDelete(condition);
	}

	@Override
	public int selectSize(Class<?> clazz, Object condition) {
		try {
			return this.getDml().selectSize(clazz, condition);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public <T> List<T> selectList(Class<T> clazz, Object condition) {
		return this.selectList(false, clazz, condition);
	}
	
	@Override
	public <T> List<T> selectList(boolean withException, Class<T> clazz, Object condition) {
		List<T> list = null;
		
		this.beforeSearch(condition);
		
		try {
			list = this.getDml().selectList(clazz, condition);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
		
		if (withException && ValueUtil.isEmpty(list)) {
			throw new ElidomRecordNotFoundException();
		}
		
		this.afterSearch(list);
		
		return list;		
	}

	@Override
	public <T> List<T> selectListWithLock(Class<T> clazz, Object condition) {
		try {
			return this.getDml().selectListWithLock(clazz, condition);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public <T> Page<T> selectPage(Class<T> clazz, Query query) {
		try {
			Page<T> page = this.getDml().selectPage(clazz, query);
			this.afterSearch(page.getList());
			return page;
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public <T> int selectSize(String tableName, Object condition) {
		try {
			return this.getDml().selectSize(tableName, condition);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public <T> List<T> selectList(String tableName, Object condition, Class<T> requiredType) {
		return this.selectList(false, tableName, condition, requiredType);
	}
	
	@Override
	public <T> List<T> selectList(boolean withException, String tableName, Object condition, Class<T> requiredType) {
		List<T> list = null;
		
		try {
			list = this.getDml().selectList(tableName, condition, requiredType);
			this.afterSearch(list);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
		
		if (withException && ValueUtil.isEmpty(list)) {
			throw new ElidomRecordNotFoundException();
		}
		
		return list;		
	}

	@Override
	public <T> List<T> selectListWithLock(String tableName, Object condition, Class<T> requiredType) {
		try {
			List<T> list = this.getDml().selectListWithLock(tableName, condition, requiredType);
			this.afterSearch(list);
			return list;
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public <T> Page<T> selectPage(String tableName, Query query, Class<T> requiredType) {
		try {
			Page<T> page = this.getDml().selectPage(tableName, query, requiredType);
			this.afterSearch(page.getList());
			return page;
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public <T> List<T> selectListByQl(String ql, Map<String, ?> paramMap, Class<T> requiredType, int pageIndex, int pageSize) {
		try {
			List<T> list = this.getDml().selectListByQl(ql, paramMap, requiredType, pageIndex, pageSize);
			this.afterSearch(list);
			return list;
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public <T> List<T> selectListByQl(String ql, Map<String, ?> paramMap, Class<T> requiredType, int pageIndex,	int pageSize, int firstResultIndex, int maxResultSize) {
		try {
			List<T> list = this.getDml().selectListByQl(ql, paramMap, requiredType, pageIndex, pageSize, firstResultIndex, maxResultSize); 
			this.afterSearch(list);
			return list; 
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public <T> Page<T> selectPageByQl(String ql, Map<String, ?> paramMap, Class<T> requiredType, int pageIndex,	int pageSize) {
		try {
			Page<T> page = this.getDml().selectPageByQl(ql, paramMap, requiredType, pageIndex, pageSize);
			this.afterSearch(page.getList());
			return page;
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public <T> Page<T> selectPageByQl(String ql, Map<String, ?> paramMap, Class<T> requiredType, int pageIndex, int pageSize, int firstResultIndex, int maxResultSize) {
		try {
			Page<T> page = this.getDml().selectPageByQl(ql, paramMap, requiredType, pageIndex, pageSize, firstResultIndex, maxResultSize);
			this.afterSearch(page.getList());
			return page;
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public int selectSizeByQl(String ql, Map<String, ?> paramMap) {
		try {
			return this.getDml().selectSizeByQl(ql, paramMap);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public <T> List<T> selectListByQlPath(String qlPath, Map<String, ?> paramMap, Class<T> requiredType, int pageIndex,	int pageSize) {
		try {
			List<T> list = this.getDml().selectListByQlPath(qlPath, paramMap, requiredType, pageIndex, pageSize);
			this.afterSearch(list);
			return list;
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public <T> List<T> selectListByQlPath(String qlPath, Map<String, ?> paramMap, Class<T> requiredType, int pageIndex, int pageSize, int firstResultIndex, int maxResultSize) {
		try {
			List<T> list = this.getDml().selectListByQlPath(qlPath, paramMap, requiredType, pageIndex, pageSize, firstResultIndex, maxResultSize); 
			this.afterSearch(list);
			return list; 
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public <T> Page<T> selectPageByQlPath(String qlPath, Map<String, ?> paramMap, Class<T> requiredType, int pageIndex,	int pageSize) {
		try {
			Page<T> page = this.getDml().selectPageByQlPath(qlPath, paramMap, requiredType, pageIndex, pageSize);
			this.afterSearch(page.getList());
			return page;
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public <T> Page<T> selectPageByQlPath(String qlPath, Map<String, ?> paramMap, Class<T> requiredType, int pageIndex, int pageSize, int firstResultIndex, int maxResultSize) {
		try {
			Page<T> page = this.getDml().selectPageByQlPath(qlPath, paramMap, requiredType, pageIndex, pageSize, firstResultIndex, maxResultSize);
			this.afterSearch(page.getList());
			return page;
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public int selectSizeByQlPath(String qlPath, Map<String, ?> paramMap) {
		try {
			return this.getDml().selectSizeByQlPath(qlPath, paramMap);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public <T> List<T> selectListBySql(String sql, Map<String, ?> paramMap, Class<T> requiredType, int pageIndex, int pageSize) {
		try {
			List<T> list = this.getDml().selectListBySql(sql, paramMap, requiredType, pageIndex, pageSize);
			this.afterSearch(list);
			return list;
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public <T> List<T> selectListBySql(String sql, Map<String, ?> paramMap, Class<T> requiredType, int pageIndex, int pageSize, int firstResultIndex, int maxResultSize) {
		try {
			List<T> list = this.getDml().selectListBySql(sql, paramMap, requiredType, pageIndex, pageSize, firstResultIndex, maxResultSize);
			this.afterSearch(list);
			return list;
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public <T> Page<T> selectPageBySql(String sql, Map<String, ?> paramMap, Class<T> requiredType, int pageIndex, int pageSize) {
		try {
			Page<T> page = this.getDml().selectPageBySql(sql, paramMap, requiredType, pageIndex, pageSize);
			this.afterSearch(page.getList());
			return page; 
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public <T> Page<T> selectPageBySql(String sql, Map<String, ?> paramMap, Class<T> requiredType, int pageIndex, int pageSize, int firstResultIndex, int maxResultSize) {
		try {
			Page<T> page = this.getDml().selectPageBySql(sql, paramMap, requiredType, pageIndex, pageSize, firstResultIndex, maxResultSize);
			this.afterSearch(page.getList());
			return page;
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public int selectSizeBySql(String sql, Map<String, ?> paramMap) {
		try {
			return this.getDml().selectSizeBySql(sql, paramMap);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public <T> List<T> selectListBySqlPath(String sqlPath, Map<String, ?> paramMap, Class<T> requiredType, int pageIndex, int pageSize) {
		try {
			List<T> list = this.getDml().selectListBySqlPath(sqlPath, paramMap, requiredType, pageIndex, pageSize);
			this.afterSearch(list);
			return list;
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public <T> List<T> selectListBySqlPath(String sqlPath, Map<String, ?> paramMap, Class<T> requiredType, int pageIndex, int pageSize, int firstResultIndex, int maxResultSize) {
		try {
			List<T> list = this.getDml().selectListBySqlPath(sqlPath, paramMap, requiredType, pageIndex, pageSize, firstResultIndex, maxResultSize);
			this.afterSearch(list);
			return list;
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public <T> Page<T> selectPageBySqlPath(String sqlPath, Map<String, ?> paramMap, Class<T> requiredType, int pageIndex, int pageSize) {
		try {
			Page<T> page = this.getDml().selectPageBySqlPath(sqlPath, paramMap, requiredType, pageIndex, pageSize);
			this.afterSearch(page.getList());
			return page;
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public <T> Page<T> selectPageBySqlPath(String sqlPath, Map<String, ?> paramMap, Class<T> requiredType, int pageIndex, int pageSize, int firstResultIndex, int maxResultSize) {
		try {
			Page<T> page = this.getDml().selectPageBySqlPath(sqlPath, paramMap, requiredType, pageIndex, pageSize, firstResultIndex, maxResultSize);
			this.afterSearch(page.getList());
			return page; 
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public int selectSizeBySqlPath(String sqlPath, Map<String, ?> paramMap) {
		try {
			return this.getDml().selectSizeBySqlPath(sqlPath, paramMap);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public int deleteList(Class<?> clazz, Object condition) {
		try {
			return this.getDml().deleteList(clazz, condition);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public int executeByQl(String ql, Map<String, ?> paramMap) {
		try {
			return this.getDml().executeByQl(ql, paramMap);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public int executeByQlPath(String qlPath, Map<String, ?> paramMap) {
		try {
			return this.getDml().executeByQlPath(qlPath, paramMap);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public int executeBySql(String sql, Map<String, ?> paramMap) {
		try {
			return this.getDml().executeBySql(sql, paramMap);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public int executeBySqlPath(String sqlPath, Map<String, ?> paramMap) {
		try {
			return this.getDml().executeBySqlPath(sqlPath, paramMap);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}
	
	/**
	 * Call Procedure
	 * 
	 * @param name
	 */
	@Override
	public void callProcedure(String name) {
		this.callProcedure(name, new HashMap<String, Object>());
	}

	@Override
	public void callProcedure(String name, Map<String, ?> paramMap) {
		try {
			this.getDml().callProcedure(name, paramMap);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public <T> T callReturnProcedure(String name, Class<T> requiredType) {
		return callReturnProcedure(name, new HashMap<String, Object>(), requiredType);
	}

	@Override
	public <T> T callReturnProcedure(String name, Map<String, ?> paramMap, Class<T> requiredType) {
		try {
			if(requiredType.equals(Map.class)) {
				return this.getDml().callReturnProcedure(name, paramMap, requiredType);
			}
			
			Map<?, ?> retMap = this.getDml().callReturnProcedure(name, paramMap, Map.class);
			String parseString =FormatUtil.toJsonString(retMap);
			
			T result = FormatUtil.jsonToObject(parseString, requiredType);
			
			ReturnDefaultMessage retMsg = (ReturnDefaultMessage)result;
			// Result 코드가 0 이 아닌 경우 ( 정상 처리 아님 ) exception 발생 
		    if(retMsg.getpOutResultCode() != 0 ) {
		        throw new ElidomRuntimeException(retMsg.getpOutMessage());
		    }
		    
		    return result;
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public <T> List<T> callReturnListProcedure(String name, Class<T> requiredType) {
		return callReturnListProcedure(name, new HashMap<String, Object>(), requiredType);
	}

	@Override
	public <T> List<T> callReturnListProcedure(String name, Map<String, ?> paramMap, Class<T> requiredType) {
		try {
			return this.getDml().callReturnListProcedure(name, paramMap, requiredType);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	/**
	 * Call Return Procedure
	 * 
	 * @param sql
	 */
	@Override
	public void callProcedureBySql(String sql) {
		this.callProcedureBySql(sql, new HashMap<String, Object>());
	}

	@Override
	public void callProcedureBySql(String sql, Map<String, ?> paramMap) {
		try {
			this.getDml().callProcedureBySql(sql, paramMap);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public <T> T callReturnProcedureBySql(String sql, Class<T> requiredType) {
		return callReturnProcedureBySql(sql, new HashMap<String, Object>(), requiredType);
	}

	@Override
	public <T> T callReturnProcedureBySql(String sql, Map<String, ?> paramMap, Class<T> requiredType) {
		try {
			return this.getDml().callReturnProcedureBySql(sql, paramMap, requiredType);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	public <T> List<T> callReturnListProcedureBySql(String sql, Class<T> requiredType) {
		return callReturnListProcedureBySql(sql, new HashMap<String, Object>(), requiredType);
	}

	@Override
	public <T> List<T> callReturnListProcedureBySql(String sql, Map<String, ?> paramMap, Class<T> requiredType) {
		try {
			return this.getDml().callReturnListProcedureBySql(sql, paramMap, requiredType);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

	@Override
	@SuppressWarnings("rawtypes")
	public List<Map> getProcedureParameters(String name) {
		try {
			return this.getDml().getProcedureParameters(name);
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}
	
	/**
	 * 생성 전 액션
	 * 
	 * @param data
	 */	
	protected <T> void beforeCreate(T data) {
		IEntityHook hook = this.checkEntityHook(data);
		if(hook != null) {
			hook.beforeCreate();
		}
	}
	
	/**
	 * 생성 후 액션
	 * 
	 * @param data
	 */
	protected <T> void afterCreate(T data) {
		IEntityHook hook = this.checkEntityHook(data);
		if(hook != null) {
			hook.afterCreate();
		}		
	}
	
	/**
	 * 생성 전 액션(List)
	 * 
	 * @param list
	 */
	protected <T> void beforeCreateList(List<T> list) {
		if(!ValueUtil.isEmpty(list)) {
			IEntityHook hook = this.checkEntityHook(list.get(0));
			if(hook != null) {
				for(T data : list) {
					IEntityHook hookObj = (IEntityHook)data;
					hookObj.beforeCreate();
				}
			}
		}
	}
	
	/**
	 * 생성 후 액션(List)
	 * 
	 * @param list
	 */
	protected <T> void afterCreateList(List<T> list) {
		if(!ValueUtil.isEmpty(list)) {
			IEntityHook hook = this.checkEntityHook(list.get(0));
			if(hook != null) {
				for(T data : list) {
					IEntityHook hookObj = (IEntityHook)data;
					hookObj.afterCreate();
				}
			}
		}
	}
	
	/**
	 * 업데이트 전 액션
	 * 
	 * @param data
	 */
	protected <T> void beforeUpdate(T data) {
		IEntityHook hook = this.checkEntityHook(data);
		if(hook != null) {
			hook.beforeUpdate();
		}
	}
		
	/**
	 * 업데이트 후 액션
	 * 
	 * @param data
	 */
	protected <T> void afterUpdate(T data) {
		IEntityHook hook = this.checkEntityHook(data);
		if(hook != null) {
			hook.afterUpdate();
		}		
	}
	
	/**
	 * 업데이트 전 액션(List)
	 * 
	 * @param list
	 */
	protected <T> void beforeUpdateList(List<T> list) {
		if(!ValueUtil.isEmpty(list)) {
			IEntityHook hook = this.checkEntityHook(list.get(0));
			if(hook != null) {
				for(T data : list) {
					IEntityHook hookObj = (IEntityHook)data;
					hookObj.beforeUpdate();
				}
			}
		}
	}
	
	/**
	 * 업데이트 후 액션 (List)
	 * 
	 * @param list
	 */
	protected <T> void afterUpdateList(List<T> list) {
		if(!ValueUtil.isEmpty(list)) {
			IEntityHook hook = this.checkEntityHook(list.get(0));
			if(hook != null) {
				for(T data : list) {
					IEntityHook hookObj = (IEntityHook)data;
					hookObj.afterUpdate();
				}
			}
		}
	}
	
	/**
	 * 생성 혹은 업데이트 전 액션 
	 */	
	protected <T> void beforeSave(T data) {
		IEntityHook hook = this.checkEntityHook(data);
		if(hook != null) {
			if(this.isInsert(data)) {
				hook.beforeCreate();
			} else {
				hook.beforeUpdate();
			}
		}
	}
	
	/**
	 * 생성 혹은 업데이트 후 액션 
	 */	
	protected <T> void afterSave(T data) {
		IEntityHook hook = this.checkEntityHook(data);
		if(hook != null) {
			// FIXME 저장 후에 호출이 되므로 아이템이 insert인지 update인지 알 수가 없다. 따라서 이 메소드는 사용하면 안된다.
			hook.afterUpdate();
		}
	}
	
	/**
	 * 생성 혹은 업데이트 전 액션(List)
	 * 
	 * @param list
	 */
	protected <T> void beforeSaveList(List<T> list) {
		if(!ValueUtil.isEmpty(list)) {
			IEntityHook hook = this.checkEntityHook(list.get(0));
			if(hook != null) {
				for(T data : list) {
					IEntityHook hookObj = (IEntityHook)data;
					if(this.isInsert(data)) {
						hookObj.beforeCreate();
					} else {
						hookObj.beforeUpdate();
					}
				}
			}
		}
	}
	
	/**
	 * 생성 혹은 업데이트 후 액션 (List)
	 * 
	 * @param list
	 */
	protected <T> void afterSaveList(List<T> list) {
		if(!ValueUtil.isEmpty(list)) {
			IEntityHook hook = this.checkEntityHook(list.get(0));
			if(hook != null) {
				for(T data : list) {
					IEntityHook hookObj = (IEntityHook)data;
					// FIXME 저장 후에 호출되므로 list의 개별 아이템이 insert인지 update인지 알 수가 없다. 따라서 이 메소드는 사용하면 안된다.
					hookObj.afterUpdate();
				}
			}
		}
	}
	
	/**
	 * 삭제 전 액션 
	 */
	protected <T> void beforeDelete(T data) {
		IEntityHook hook = this.checkEntityHook(data);
		if(hook != null) {
			hook.beforeDelete();
		}
	}
	
	/**
	 * 삭제 후 액션 
	 */	
	protected <T> void afterDelete(T data) {
		IEntityHook hook = this.checkEntityHook(data);
		if(hook != null) {
			hook.afterDelete();
		}
	}
	
	/**
	 * 삭제 전 액션(List)
	 * 
	 * @param list
	 */
	protected <T> void beforeDeleteList(List<T> list) {
		if(!ValueUtil.isEmpty(list)) {
			IEntityHook hook = this.checkEntityHook(list.get(0));
			if(hook != null) {
				for(T data : list) {
					IEntityHook hookObj = (IEntityHook)data;
					hookObj.beforeDelete();
				}
			}
		}
	}
	
	/**
	 * 삭제 후 액션 (List)
	 * 
	 * @param list
	 */
	protected <T> void afterDeleteList(List<T> list) {
		if(!ValueUtil.isEmpty(list)) {
			IEntityHook hook = this.checkEntityHook(list.get(0));
			if(hook != null) {
				for(T data : list) {
					IEntityHook hookObj = (IEntityHook)data;
					hookObj.afterDelete();
				}
			}
		}
	}
	
	/**
	 * 조회(Single) 전 액션
	 * 
	 *  @param data
	 */	
	protected <T> void beforeFind(T data) {
		IEntityHook hook = this.checkEntityHook(data);
		if (hook != null) {
			hook.beforeFind();
		}
	}
	
	/**
	 * 조회(Sinlge) 후 액션 
	 */	
	protected <T> void afterFind(T data) {
		IEntityHook hook = this.checkEntityHook(data);
		if(hook != null) {
			hook.afterFind();
		}
	}
	
	/**
	 * 조회(Multi) 전 액션
	 * 
	 *  @param data
	 */	
	protected <T> void beforeSearch(T data) {
		IEntityHook hook = this.checkEntityHook(data);
		if (hook != null) {
			hook.beforeSearch();
		}
	}
	
	/**
	 * 조회(Multi) 후 액션 
	 */	
	protected <T> void afterSearch(List<T> datas) {
		if (ValueUtil.isEmpty(datas))
			return;

		for (T t : datas) {
			IEntityHook hook = this.checkEntityHook(t);
			if (hook != null)
				hook.afterSearch();
		}
	}
	
	/**
	 * data가 IEntityHook을 상속했는지 체크
	 *  
	 * @param data
	 * @return
	 */
	protected <T> IEntityHook checkEntityHook(T data) {
		return (data instanceof IEntityHook) ? (IEntityHook)data : null; 
	}
	
	/**
	 * 키 값이 비어있는지 체크
	 * 
	 * @param data
	 * @return
	 */
	protected boolean isEmptyKeyValue(Object data) {
		Query query = null;
		try {
			query = ((DmlJdbc2)this.getDml()).toPkQuery(data);
		} catch (Exception e) {
			throw new ElidomRuntimeException(e.getMessage(), e);
		}

		List<Filter> filterList = query.getFilter();
		for (Filter filter : filterList) {
			if (filter.getRightOperand() == null)
				return true;
		}

		return false;
	}
	
	/**
	 * insert 모드 인지 체크
	 * 
	 * @return
	 */
	private <T> boolean isInsert(T data) throws ElidomDatabaseException {
		try {
			return this.isEmptyKeyValue(data) || this.getDml().select(data) == null;
		} catch(Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}

}