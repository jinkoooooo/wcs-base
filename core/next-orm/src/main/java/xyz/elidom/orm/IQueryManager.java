/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.orm;

import java.util.List;
import java.util.Map;

import xyz.elidom.dbist.dml.Dml;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.dbist.metadata.Table;
import xyz.elidom.dbist.processor.Preprocessor;

/**
 * 데이터베이스 쿼리 수행을 위해 DBIST 구현을 Adapting한 API 
 * 
 * @author shortstop
 */
public interface IQueryManager {
	
	/**
	 * DBIST Dml을 리턴 
	 * 
	 * @return
	 */
	Dml getDml();

	/**
	 * Database 종류를 리턴 
	 * 
	 * @return
	 */
	String getDbType();
	
	/**
	 * Database Table Name과 매핑된 Entity 클래스를 리턴한다.
	 *  
	 * @param tableName
	 * @return
	 */
	Class<?> getClass(String tableName);
	
	/**
	 * Entity Object로 매핑된 테이블을 찾아 리턴 
	 * 
	 * @param entityObj
	 * @return
	 */
	Table getTable(Object entityObj);
	
	/**
	 * Entity 명으로 테이블을 찾아 리턴 
	 * 
	 * @param name
	 * @return
	 */
	Table getTable(String name);
	
	/**
	 * Preprocessor를 설정 
	 * 
	 * @param preprocessor
	 */
	void setPreprocessor(Preprocessor preprocessor);

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
	<T> T select(T data);
	
	/**
	 * Select With Exception
	 * 
	 * @param data
	 * @param withException
	 * @return
	 */
	<T> T select(boolean withException, T data);

	/**
	 * The same as the <i>select</i> method above.<br>
	 * But the selected data row will be locked during the current transaction scope.
	 * 
	 * @param data
	 * @return The data selected
	 */
	<T> T selectWithLock(T data);

	/**
	 * Select a data row from the database table mapped to T class by PK condition parameters.<br>
	 * The data type of the condition parameters can be an instance of primary key value(a value, a Data Model Object, array, List, or
	 * HttpServletRequest), Map, Query, Filters, or Filter
	 * 
	 * @param <T>
	 *            The object class mapped to a database table
	 * @param clazz
	 *            The object class mapped to a database table
	 * @param pkCondition
	 *            The primary key condition wanted to select
	 * @return The data selected
	 */
	<T> T select(Class<T> clazz, Object... pkCondition);
	
	/**
	 * Select With Exception
	 * 
	 * @param clazz
	 * @param withException
	 * @param pkCondition
	 * @return
	 */
	<T> T select(boolean withException, Class<T> clazz, Object... pkCondition);

	/**
	 * The same as the <i>select</i> method above.<br>
	 * But the selected data row will be locked during the current transaction scope.
	 * 
	 * @param clazz
	 * @param pkCondition
	 * @return The data selected
	 */
	<T> T selectWithLock(Class<T> clazz, Object... pkCondition);
	
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
	<T> T simpleSelect(T data);
	
	/**
	 * Select Simply With Exception
	 * 
	 * @param data
	 * @param withException
	 * @return
	 */
	<T> T simpleSelect(boolean withException, T data);
	
	/**
	 * Select Simply With Exception
	 * 
	 * @param clazz
	 * @param withException
	 * @param pkCondition
	 * @return
	 */
	<T> T simpleSelect(boolean withException, Class<T> clazz, Object... pkCondition);

	/**
	 * Select a data row from the database table mapped to T class by condition parameter.<br>
	 * The data type of the condition parameter can be an instance of Data Model, Map, Query, Filters, or Filter
	 * 
	 * @param <T>
	 *            The object class mapped to a database table
	 * @param clazz
	 *            The object class mapped to a database table
	 * @param condition
	 *            The condition wanted to select
	 * @return The data selected
	 */
	<T> T selectByCondition(Class<T> clazz, Object condition);
	
	/**
	 * Select By Condition With Exception
	 * @param clazz
	 * @param condition
	 * @param withException
	 * @return
	 */
	<T> T selectByCondition(boolean withException, Class<T> clazz, Object condition);

	/**
	 * The same as the <i>selectByCondition</i> method above.<br>
	 * But the selected data row will be locked during the current transaction scope.
	 * 
	 * @param clazz
	 * @param condition
	 * @return The data selected
	 */
	<T> T selectByConditionWithLock(Class<T> clazz, Object condition);

	/**
	 * Select a data row from the database table by PK condition parameter.<br>
	 * And return an instance of requiredType.<br>
	 * The data type of the condition parameters can be an instance of primary key value(a value, a Data Model Object, array, List, or
	 * HttpServletRequest), Map, Query, Filters, or Filter
	 * 
	 * @param tableName
	 * @param pkCondition
	 * @param requiredType
	 * @return The data selected
	 */
	<T> T select(String tableName, Object pkCondition, Class<T> requiredType);
	
	/**
	 * Select With Exception
	 * @param tableName
	 * @param pkCondition
	 * @param requiredType
	 * @param withException
	 * @return
	 */
	<T> T select(boolean withException, String tableName, Object pkCondition, Class<T> requiredType);

	/**
	 * The same as the <i>select</i> method above.<br>
	 * But the selected data row will be locked during the current transaction scope.
	 * 
	 * @param tableName
	 * @param pkCondition
	 * @param requiredType
	 * @return The data selected
	 */
	<T> T selectWithLock(String tableName, Object pkCondition, Class<T> requiredType);

	/**
	 * Select a data row from the database table by PK condition parameter.<br>
	 * And return an instance of requiredType.<br>
	 * The data type of the condition parameter can be an instance of Data Model, Map, Query, Filters, or Filter
	 * 
	 * @param tableName
	 * @param condition
	 * @param requiredType
	 * @return The data selected
	 */
	<T> T selectByCondition(String tableName, Object condition, Class<T> requiredType);

	/**
	 * The same as the <i>selectByCondition</i> method above.<br>
	 * But the selected data row will be locked during the current transaction scope.
	 * 
	 * @param tableName
	 * @param condition
	 * @param requiredType
	 * @return The data selected
	 */
	<T> T selectByConditionWithLock(String tableName, Object condition, Class<T> requiredType);

	/**
	 * Select a data row by the ql and the paramMap.<br>
	 * And return an instance of requiredType.<br>
	 * In case of DmlJdbc ql means SQL query. In case of DmlHibernate ql means HQL query. ...
	 * 
	 * @param ql
	 * @param paramMap
	 * @param requiredType
	 * @return The data selected
	 */
	<T> T selectByQl(String ql, Map<String, ?> paramMap, Class<T> requiredType);

	/**
	 * Select a data row by the qlPath and the paramMap.<br>
	 * And return an instance of requiredType.<br>
	 * qlPath can be a classpath or filepath of ql file<br>
	 * If the target of the path is a directory, it'll find the <i>&lt;dbType&gt;</i>.sql or ansi.sql file in the directory.<br>
	 * In case of DmlJdbc ql means SQL query. In case of DmlHibernate query means HQL query. ...
	 * 
	 * @param qlPath
	 * @param paramMap
	 * @param requiredType
	 * @return The data selected
	 */
	<T> T selectByQlPath(String qlPath, Map<String, ?> paramMap, Class<T> requiredType);

	/**
	 * Select a data row by the sql and the paramMap.<br>
	 * And return an instance of requiredType.<br>
	 * 
	 * @param sql
	 * @param paramMap
	 * @param requiredType
	 * @return The data selected
	 */
	<T> T selectBySql(String sql, Map<String, ?> paramMap, Class<T> requiredType);

	/**
	 * Select a data row by the sqlPath and the paramMap.<br>
	 * And return an instance of requiredType.<br>
	 * sqlPath can be a classpath or filepath of sql file<br>
	 * If the target of the path is a directory, it'll find the <i>&lt;dbType&gt;</i>.sql or ansi.sql file in the directory.<br>
	 * 
	 * @param sqlPath
	 * @param paramMap
	 * @param requiredType
	 * @return The data selected
	 */
	<T> T selectBySqlPath(String sqlPath, Map<String, ?> paramMap, Class<T> requiredType);

	/**
	 * Insert a data row to the database table mapped to the data class.
	 * 
	 * @param data
	 *            The data to insert
	 */
	void insert(Object data);

	/**
	 * Insert data list to the database table mapped to the list item class.
	 * 
	 * @param list
	 *            The data list to insert
	 */
	void insertBatch(List<?> list);

	/**
	 * Insert some fields of a data row to the database table mapped to the data class.
	 * 
	 * @param data
	 * @param fieldNames
	 */
	void insert(Object data, String... fieldNames);

	/**
	 * Insert some fields of data list to the database table mapped to the list item class.
	 * 
	 * @param list
	 *            The data list to insert
	 * @param fieldNames
	 */
	void insertBatch(List<?> list, String... fieldNames);

	/**
	 * Insert a data row to the database table mapped to T class.<br>
	 * And return the inserted data instance.
	 * 
	 * @param clazz
	 *            The class mapped to a database table
	 * @param data
	 *            The data to insert
	 * @return The data inserted
	 */
	<T> T insert(Class<T> clazz, Object data);

	/**
	 * Insert data list to the database table mapped to the clazz.
	 * 
	 * @param clazz
	 *            The class mapped to a database table
	 * @param list
	 *            The data list to insert
	 */
	void insertBatch(Class<?> clazz, List<?> list);

	/**
	 * Insert some fields of data list to the database table mapped to the clazz.
	 * 
	 * @param clazz
	 * @param data
	 * @param fieldNames
	 */
	void insert(Class<?> clazz, Object data, String... fieldNames);

	/**
	 * Insert some fields of data list to the database table mapped to the clazz.
	 * 
	 * @param clazz
	 * @param list
	 * @param fieldNames
	 */
	void insertBatch(Class<?> clazz, List<?> list, String... fieldNames);

	/**
	 * Insert a data row to the database table.
	 * 
	 * @param tableName
	 * @param data
	 */
	void insert(String tableName, Object data);

	/**
	 * Insert data list to the database table.
	 * 
	 * @param tableName
	 * @param list
	 */
	void insertBatch(String tableName, List<?> list);

	/**
	 * Insert some fields of a data row to the database table.
	 * 
	 * @param tableName
	 * @param data
	 * @param fieldNames
	 */
	void insert(String tableName, Object data, String... fieldNames);

	/**
	 * Insert some fields of data list to the database table.
	 * 
	 * @param tableName
	 * @param list
	 * @param fieldNames
	 */
	void insertBatch(String tableName, List<?> list, String... fieldNames);

	/**
	 * Update a data row to the database table mapped to the data class.
	 * 
	 * @param data
	 *            The data to update
	 */
	void update(Object data);

	/**
	 * Update data list to the database table mapped to the list item class.
	 * 
	 * @param list
	 */
	void updateBatch(List<?> list);

	/**
	 * Update some fields of a data row to the database table mapped to the data class.
	 * 
	 * @param data
	 * @param fieldNames
	 */
	void update(Object data, String... fieldNames);

	/**
	 * Update some fields of data list to the database table mapped to the list item class.
	 * 
	 * @param list
	 * @param fieldNames
	 */
	void updateBatch(List<?> list, String... fieldNames);

	/**
	 * Update a data row to the database table mapped to T class.<br>
	 * And return the updated data instance.
	 * 
	 * @param clazz
	 *            The class mapped to a database table
	 * @param data
	 *            The data to update
	 * @return The data updated
	 */
	<T> T update(Class<T> clazz, Object data);

	/**
	 * Update data list to the database table mapped to the clazz.
	 * 
	 * @param clazz
	 * @param list
	 */
	void updateBatch(Class<?> clazz, List<?> list);

	/**
	 * Update some fields of a data row to the database table mapped to T class.<br>
	 * And return the updated data instance.
	 * 
	 * @param clazz
	 * @param data
	 * @param fieldNames
	 * @return The data updated
	 */
	<T> T update(Class<T> clazz, Object data, String... fieldNames);

	/**
	 * Update some fields of data list to the database table mapped to the clazz.
	 * 
	 * @param clazz
	 * @param list
	 * @param fieldNames
	 */
	void updateBatch(Class<?> clazz, List<?> list, String... fieldNames);

	/**
	 * Update a data row to the database table.<br>
	 * 
	 * @param tableName
	 * @param data
	 */
	void update(String tableName, Object data);

	/**
	 * Update data list to the database table.
	 * 
	 * @param tableName
	 * @param list
	 */
	void updateBatch(String tableName, List<?> list);

	/**
	 * Update some fields of a data row to the database table.
	 * 
	 * @param tableName
	 * @param data
	 * @param fieldNames
	 */
	void update(String tableName, Object data, String... fieldNames);

	/**
	 * Update some fields of data list to the database table.
	 * 
	 * @param tableName
	 * @param list
	 * @param fieldNames
	 */
	void updateBatch(String tableName, List<?> list, String... fieldNames);

	/**
	 * Upsert (Insert or update) a data row to the database table mapped to the data class.
	 * 
	 * @param data
	 *            The data to upsert
	 */
	void upsert(Object data);

	/**
	 * Upsert (Insert or update) data list to the database table mapped to the list item class.
	 * 
	 * @param list
	 */
	void upsertBatch(List<?> list);

	/**
	 * Upsert (Insert or update) some fields of a data row to the database table mapped to the data class.
	 * 
	 * @param data
	 * @param fieldNames
	 */
	void upsert(Object data, String... fieldNames);

	/**
	 * Upsert (Insert or update) some fields of data list to the database table mapped to the list item class.
	 * 
	 * @param list
	 * @param fieldNames
	 */
	void upsertBatch(List<?> list, String... fieldNames);

	/**
	 * Upsert (Insert or update) a data row to the database table mapped to T class.<br>
	 * And return the upserted data instance.
	 * 
	 * @param clazz
	 *            The class mapped to a database table
	 * @param data
	 *            The data to upsert
	 * @return The data upserted
	 */
	<T> T upsert(Class<T> clazz, Object data);

	void upsertBatch(Class<?> clazz, List<?> list);
	void upsert(Class<?> clazz, Object data, String... fieldNames);
	void upsertBatch(Class<?> clazz, List<?> list, String... fieldNames);

	void upsert(String tableName, Object data);
	void upsertBatch(String tableName, List<?> list);
	void upsert(String tableName, Object data, String... fieldNames);
	void upsertBatch(String tableName, List<?> list, String... fieldNames);

	/**
	 * Delete a data row from the database table mapped to the data class.
	 * 
	 * @param data
	 *            The data to delete
	 */
	void delete(Object data);
	void deleteBatch(List<?> list);

	/**
	 * Delete a data to the database table mapped to T class. by condition parameter.<br>
	 * The data type of condition parameter can be primary key value (a value, array, List, or HttpServletRequest), Map, Query, Filters, Filter
	 * 
	 * @param <T>
	 *            The object class mapped to a database table
	 * @param clazz
	 *            The object class mapped to a database table
	 * @param pkCondition
	 *            The PK condition wanted to delete
	 * @return The data deleted
	 */
	<T> T delete(Class<T> clazz, Object... pkCondition);
	void deleteBatch(Class<?> clazz, List<?> list);
	<T> T deleteByCondition(Class<T> clazz, Object condition);

	void delete(String tableName, Object... pkCondition);
	void deleteBatch(String tableName, List<?> list);
	void deleteByCondition(String tableName, Object condition);

	int selectSize(Class<?> clazz, Object condition);

	/**
	 * Select some data from the database table mapped to T class<br>
	 * by condition parameter<br>
	 * The data type of condition parameter can be Map, Query, Filters, Filter
	 * 
	 * @param <T>
	 *            The object class mapped to a database table
	 * @param clazz
	 *            The object class mapped to a database table
	 * @param condition
	 *            The condition wanted to select
	 * @return The data list selected
	 */
	<T> List<T> selectList(Class<T> clazz, Object condition);
	<T> List<T> selectList(boolean withException, Class<T> clazz, Object condition);
	<T> List<T> selectListWithLock(Class<T> clazz, Object condition);
	<T> Page<T> selectPage(Class<T> clazz, Query query);

	<T> int selectSize(String tableName, Object condition);
	<T> List<T> selectList(String tableName, Object condition, Class<T> requiredType);
	<T> List<T> selectList(boolean withException, String tableName, Object condition, Class<T> requiredType);
	<T> List<T> selectListWithLock(String tableName, Object condition, Class<T> requiredType);
	<T> Page<T> selectPage(String tableName, Query query, Class<T> requiredType);

	/**
	 * Select some data as the requiredType by the query statement and the paramMap<br>
	 * In case of DmlJdbc query means SQL query. In case of DmlHibernate query means HQL query. ...<br>
	 * If you don't want pagination, you would input pageIndex: 0 and pageSize: 0
	 * 
	 * @param ql
	 * @param paramMap
	 * @param requiredType
	 * @param pageIndex
	 * @param pageSize
	 * @return The data list selected
	 */
	<T> List<T> selectListByQl(String ql, Map<String, ?> paramMap, Class<T> requiredType, int pageIndex, int pageSize);
	<T> List<T> selectListByQl(String ql, Map<String, ?> paramMap, Class<T> requiredType, int pageIndex, int pageSize, int firstResultIndex,
			int maxResultSize);
	<T> Page<T> selectPageByQl(String ql, Map<String, ?> paramMap, Class<T> requiredType, int pageIndex, int pageSize);
	<T> Page<T> selectPageByQl(String ql, Map<String, ?> paramMap, Class<T> requiredType, int pageIndex, int pageSize, int firstResultIndex,
			int maxResultSize);
	int selectSizeByQl(String ql, Map<String, ?> paramMap);
	/**
	 * Select some data as the requiredType by the query statement (which is in the path) and the paramMap<br>
	 * The path means classpath or filepath<br>
	 * In case of DmlJdbc query means SQL query. In case of DmlHibernate query means HQL query. ...<br>
	 * If you don't want pagination, you would input pageIndex: 0 and pageSize: 0
	 * 
	 * @param qlPath
	 * @param paramMap
	 * @param requiredType
	 * @param pageIndex
	 * @param pageSize
	 * @return The data list selected
	 */
	<T> List<T> selectListByQlPath(String qlPath, Map<String, ?> paramMap, Class<T> requiredType, int pageIndex, int pageSize);
	<T> List<T> selectListByQlPath(String qlPath, Map<String, ?> paramMap, Class<T> requiredType, int pageIndex, int pageSize, int firstResultIndex,
			int maxResultSize);
	<T> Page<T> selectPageByQlPath(String qlPath, Map<String, ?> paramMap, Class<T> requiredType, int pageIndex, int pageSize);
	<T> Page<T> selectPageByQlPath(String qlPath, Map<String, ?> paramMap, Class<T> requiredType, int pageIndex, int pageSize, int firstResultIndex,
			int maxResultSize);
	int selectSizeByQlPath(String qlPath, Map<String, ?> paramMap);

	/**
	 * Select some data as the requiredType by the query statement (SQL query) and the paramMap
	 * 
	 * @param sql
	 * @param paramMap
	 * @param requiredType
	 * @param pageIndex
	 * @param pageSize
	 * @return The data list selected
	 */
	<T> List<T> selectListBySql(String sql, Map<String, ?> paramMap, Class<T> requiredType, int pageIndex, int pageSize);
	<T> List<T> selectListBySql(String sql, Map<String, ?> paramMap, Class<T> requiredType, int pageIndex, int pageSize, int firstResultIndex,
			int maxResultSize);
	<T> Page<T> selectPageBySql(String sql, Map<String, ?> paramMap, Class<T> requiredType, int pageIndex, int pageSize);
	<T> Page<T> selectPageBySql(String sql, Map<String, ?> paramMap, Class<T> requiredType, int pageIndex, int pageSize, int firstResultIndex,
			int maxResultSize);
	int selectSizeBySql(String sql, Map<String, ?> paramMap);

	/**
	 * Select some data as the requiredType by the query statement (SQL query) and the paramMap<br>
	 * The path means classpath or filepath
	 * 
	 * @param sqlPath
	 * @param paramMap
	 * @param requiredType
	 * @param pageIndex
	 * @param pageSize
	 * @return The data list selected
	 */
	<T> List<T> selectListBySqlPath(String sqlPath, Map<String, ?> paramMap, Class<T> requiredType, int pageIndex, int pageSize);
	<T> List<T> selectListBySqlPath(String sqlPath, Map<String, ?> paramMap, Class<T> requiredType, int pageIndex, int pageSize,
			int firstResultIndex, int maxResultSize);
	<T> Page<T> selectPageBySqlPath(String sqlPath, Map<String, ?> paramMap, Class<T> requiredType, int pageIndex, int pageSize);
	<T> Page<T> selectPageBySqlPath(String sqlPath, Map<String, ?> paramMap, Class<T> requiredType, int pageIndex, int pageSize,
			int firstResultIndex, int maxResultSize);
	int selectSizeBySqlPath(String sqlPath, Map<String, ?> paramMap);

	/**
	 * Delete some data from the database table mappedt to T class<br>
	 * by condition parameter<br>
	 * The data type of condition parameter can be Map, Query, Filters, Filter
	 * 
	 * @param clazz
	 *            The object class mapped to a database table
	 * @param condition
	 *            The condition wanted to delete
	 */
	int deleteList(Class<?> clazz, Object condition);

	/**
	 * Execute CUD (insert, update, or delete) by query statement<br>
	 * In case of DmlJdbc query means SQL query. In case of DmlHibernate query means HQL query. ...
	 * 
	 * @param ql
	 * @param paramMap
	 * @return The number of rows affected
	 */
	int executeByQl(String ql, Map<String, ?> paramMap);
	/**
	 * Execute CUD (insert, update, or delete) by query statement (which is in the path) and the paramMap<br>
	 * In case of DmlJdbc query means SQL query. In case of DmlHibernate query means HQL query. ...
	 * 
	 * @param qlPath
	 * @param paramMap
	 * @return The number of rows affected
	 */
	int executeByQlPath(String qlPath, Map<String, ?> paramMap);

	/**
	 * Execute CUD (insert, update, or delete) by query statement (SQL) and the paramMap<br>
	 * The path means classpath or filepath<br>
	 * In case of DmlJdbc query means SQL query. In case of DmlHibernate query means HQL query. ...<br>
	 * 
	 * @param sql
	 * @param paramMap
	 * @return The number of rows affected
	 */
	int executeBySql(String sql, Map<String, ?> paramMap);
	int executeBySqlPath(String sqlPath, Map<String, ?> paramMap);
	
	/**
	 * Call Procedure
	 * 
	 * @param name
	 */
	void callProcedure(String name);
	void callProcedure(String name, Map<String, ?> paramMap);
	<T> T callReturnProcedure(String name, Class<T> requiredType);
	<T> T callReturnProcedure(String name, Map<String, ?> paramMap, Class<T> requiredType);
	<T> List<T> callReturnListProcedure(String name, Class<T> requiredType);
	<T> List<T> callReturnListProcedure(String name, Map<String, ?> paramMap, Class<T> requiredType);
	
	/**
	 * Call Return Procedure
	 * 
	 * @param sql
	 */
	void callProcedureBySql(String sql);
	void callProcedureBySql(String sql, Map<String, ?> paramMap);
	<T> T callReturnProcedureBySql(String sql, Class<T> requiredType);
	<T> T callReturnProcedureBySql(String sql, Map<String, ?> paramMap, Class<T> requiredType);
	<T> List<T> callReturnListProcedureBySql(String sql, Class<T> requiredType);
	<T> List<T> callReturnListProcedureBySql(String sql, Map<String, ?> paramMap, Class<T> requiredType);
	
	/**
	 * Get Procedure Parameters
	 * 
	 * @param name
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	List<Map> getProcedureParameters(String name);
}