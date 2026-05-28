/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.base.rest;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import xyz.elidom.base.BaseConfigConstants;
import xyz.elidom.base.entity.Resource;
import xyz.elidom.base.entity.ResourceColumn;
import xyz.elidom.base.model.EntityColumn;
import xyz.elidom.base.model.EntityIndex;
import xyz.elidom.base.util.ResourceUtil;
import xyz.elidom.dbist.ddl.Ddl;
import xyz.elidom.dbist.metadata.Table;
import xyz.elidom.dbist.metadata.TableCol;
import xyz.elidom.dbist.metadata.TableIdx;
import xyz.elidom.exception.server.ElidomServiceException;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.system.service.params.BasicOutput;
import xyz.elidom.sys.util.AssertUtil;
import xyz.elidom.sys.util.SettingUtil;
import xyz.elidom.sys.util.ThrowUtil;
import xyz.elidom.sys.util.SysValueUtil;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ClassUtil;

@RestController
@Transactional
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/ddl")
@ServiceDesc(description = "Handling Database Table, Index Service API")
public class DdlController {
	/**
	 * Logger
	 */
	protected Logger logger = LoggerFactory.getLogger(DdlController.class);

	@Autowired
	private IQueryManager queryManager;
	
	@Autowired
	private Ddl ddl;

	@GetMapping(value = "/tables", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Search all tables owned by database account")
	public List<Map<String, Object>> allTables(@RequestParam(name = "name", required = false) String tableName) {
		List<String> allTables = this.ddl.getAllTables();
		
		List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
		for(String table : allTables) {
			results.add(SysValueUtil.newMap("id,table_name", table.toLowerCase(), table));
		}
		
		return results;
	}
	
	@GetMapping(value = "/columns/table/{table_name}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Search all table columns of table")
	public List<TableCol> tableColumn(@PathVariable(name = "table_name") String tableName) {
		AssertUtil.assertNotEmpty("terms.label.table_name", tableName);
		Table tableMeta = this.ddl.getTable(tableName);
		return this.ddl.getTableCols(tableMeta);
	}
	
	@GetMapping(value = "/indexes/table/{table_name}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Search table index list of table")
	public List<TableIdx> tableIndexes(@PathVariable("table_name") String tableName) {
		AssertUtil.assertNotEmpty("terms.label.table_name", tableName);
		List<TableIdx> tableIndexes = this.ddl.getTableIndexes(tableName);
		return tableIndexes;
	}
	
	@GetMapping(value = "/table/columns/{entity_id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Search Table Columns of Entity")
	public List<TableCol> tableColumns(@PathVariable("entity_id") String entityId) {
		
		Resource resource = this.findAndCheckEntity(entityId);
		String tableName = resource.getTableName();
		AssertUtil.assertNotEmpty("terms.label.table_name", tableName);
		
		Table tableMeta = null;
		try {
			tableMeta = this.ddl.getTable(tableName);
		} catch(Exception e) {
			return new ArrayList<TableCol>();
		}
		
		List<TableCol> columnList = this.ddl.getTableCols(tableMeta);
		int rank = 10;
		
		for(TableCol col : columnList) {
			col.setRank(rank);
			rank += 10;
		}
		
		return columnList;
	}
	
	@GetMapping(value = "/table/diff_columns/{entity_id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Search Different Columns between Entity and Table")
	public List<EntityColumn> diffColumns(@PathVariable("entity_id") String entityId) {
		
		Resource resource = BeanUtil.get(ResourceController.class).resourceColumns(entityId);
		List<ResourceColumn> entityColumns = resource.getItems();
		List<TableCol> tableColumns = this.tableColumns(entityId);
		
		List<EntityColumn> diffList = new ArrayList<EntityColumn>();
		for(ResourceColumn resCol : entityColumns) {
			EntityColumn ec = new EntityColumn(resCol, null);
			diffList.add(ec);
		}
		
		for(TableCol tableCol : tableColumns) {
			EntityColumn ec = this.findEntityColumn(diffList, tableCol.getName());
			
			if(ec == null) {
				ec = new EntityColumn(null, tableCol);
				diffList.add(ec);
			} else {
				ec.applyTableColumn(tableCol);
			}
			
			ec.checkTypeDiff(this.ddl.getDdlMapper().toDatabaseType(ec.getEntityColDataType()));
			ec.checkSizeDiff();
		}
		
		return diffList;
	}
	
	private EntityColumn findEntityColumn(List<EntityColumn> entityColumns, String columnName) {		
		for(EntityColumn ec : entityColumns) {
			if(SysValueUtil.isEqual(columnName, ec.getEntityColName())) {
				return ec;
			}
		}
		
		return null;
	}
	
	@PostMapping(value = "/table/generate/{entity_id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@ApiDesc(description = "Generate Table By Entity Id")
	public BasicOutput createTable(@PathVariable("entity_id") String entityId) {
		
		Resource resource = this.findAndCheckEntity(entityId);
		String tableName = resource.getTableName();
		AssertUtil.assertNotEmpty("terms.label.table_name", tableName);
		this.checkTableAlreadyExist(tableName);
		
		String entityClassName = ResourceUtil.getEntityClassName(resource);
		return this.createTableByEntityName(entityClassName);
	}
	
	@PostMapping(value = "/table/create/{entity_class_name}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@ApiDesc(description = "Create Table By Entity Class Name")
	public BasicOutput createTableByEntityName(@PathVariable("entity_class_name") String entityClassName) {
		
		Class<?> entityClass = ClassUtil.forName(entityClassName);
		Annotation tableAnn = AnnotationUtils.findAnnotation(entityClass, xyz.elidom.dbist.annotation.Table.class);
		String tableName = (String)AnnotationUtils.getAnnotationAttributes(tableAnn).get("name");
		this.checkTableAlreadyExist(tableName);
		
		String resultStr = this.ddl.createTable(entityClass);
		BasicOutput result = new BasicOutput();
		if(resultStr == null) {
			result.setMsg("Table [" + tableName + "] is created!");
			return result;
		} else {
			throw new ElidomServiceException("Failed to generate table!", resultStr);
		}
	}
	
	@DeleteMapping(value = "/table/{entity_id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Delete Table By Entity Id")
	public BasicOutput deleteTable(@PathVariable("entity_id") String entityId) {

		Resource resource = this.findAndCheckEntity(entityId);
		String tableName = resource.getTableName();
		AssertUtil.assertNotEmpty("terms.label.table_name", tableName);
		String sequenceName = this.getSequenceName(tableName);
		
		BasicOutput result = new BasicOutput();
		String resultStr = this.ddl.dropTable(tableName, sequenceName, null);
		
		if(resultStr == null) {
			result.setMsg("Table [" + tableName + "] was dropped.");
		} else {
			throw new ElidomServiceException("Failed to drop table!", resultStr);
		}
		
		return result;
	}
	
	@SuppressWarnings("unchecked")
	@PutMapping(value = "/table/{entity_id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Modify Table By Entity Id")
	public BasicOutput alterTable(@PathVariable("entity_id") String entityId) {
		
		// 1. Entity 조회 
		Resource resource = this.findAndCheckEntity(entityId);
		String tableName = resource.getTableName();
		
		// 2. Table이 존재하는지 체크 
		if(!this.ddl.isTableExist(tableName)) {
			throw new ElidomServiceException("Table is not exist! So you should create table first!");
		}
		
		// 3. 어떤 컬럼이 다른지 조회 
		List<EntityColumn> allColumns = this.diffColumns(entityId);
		List<Map<String, Object>> modifiedColumns = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> addedColumns = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> removedColumns = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> nullableColumns = new ArrayList<Map<String, Object>>();
		
		for(EntityColumn ec : allColumns) {
			String tableColumnType = this.ddl.getDdlMapper().toDatabaseType(ec.getEntityColDataType());
			String alterType = ec.checkAlterType(tableColumnType);
			Map<String, Object> columnMap = ec.toAlterMap();
			String columnType = this.toDatabaseType(ec.getEntityColDataType(), ec.getEntityColLength());
			
			if(alterType == EntityColumn.ALTER_TYPE_ADD) {
				columnMap.put("type", columnType);
				addedColumns.add(columnMap);
				
			} else if(alterType == EntityColumn.ALTER_TYPE_DROP) {
				removedColumns.add(columnMap);
				
			} else if(alterType == EntityColumn.ALTER_TYPE_MODIFY_TYPE) {
				columnMap.put("type", columnType);
				modifiedColumns.add(columnMap);
				
			} else if(alterType == EntityColumn.ALTER_TYPE_MODIFY_NULLABLE) {
				columnMap.put("nullable", ec.getEntityColNullable() ? SysConstants.CAP_YES_STRING : SysConstants.CAP_NO_STRING);
				nullableColumns.add(columnMap);
			}
		}
		
		// 4. Alter 문 실행 ...
		@SuppressWarnings("rawtypes")
		Map<String, List<Map<String, Object>>> alterParams = (Map)SysValueUtil.newMap("addColumns,removeColumns,modifyColumns,nullableColumns", addedColumns, removedColumns, modifiedColumns, nullableColumns);
		String resultStr = this.ddl.alterTable(tableName, alterParams);
		BasicOutput result = new BasicOutput();
		
		if(resultStr == null) {
			result.setMsg("Table [" + tableName + "] was altered.");
		} else {
			throw new ElidomServiceException("Failed to alter table!", resultStr);
		}
		
		return result;
	}
	
	@PostMapping(value = "/table/{entity_id}/column/add", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Add Column")
	public BasicOutput addColumn(@PathVariable("entity_id") String entityId, @RequestBody EntityColumn column) {
		// 1. 체크 
		if(SysValueUtil.isEmpty(column.getEntityColDataType())) {
			throw ThrowUtil.newNotAllowedEmptyInfo("Entity Column"); 
		}
		
		// 2. Entity 조회 
		Resource resource = this.findAndCheckEntity(entityId);
		String tableName = resource.getTableName();
		
		// 3. Add column script
		String addColumnTemplate = this.ddl.getDdlMapper().addColumnTemplate();
		String toColDataType = this.ddl.getDdlMapper().toDatabaseType(SysValueUtil.newMap("type,length", column.getEntityColDataType(), column.getEntityColLength()));
		Map<String, Object> addCol = SysValueUtil.newMap("name,type,nullable", column.getEntityColName(), toColDataType, column.getEntityColNullable() ? "YES" : "NO");
		List<Map<String, Object>> addColList = new ArrayList<Map<String, Object>>();
		addColList.add(addCol);
		
		// 4. Add column 호출 
		Map<String, Object> paramMap = SysValueUtil.newMap("tableName,addColumns", tableName, addColList);
		String msg = this.ddl.executeDDL(tableName, addColumnTemplate, paramMap);
		
		if(msg != null) {
			throw new ElidomServiceException("Failed to add column type!", msg);
		}
		
		return new BasicOutput();
	}
	
	@PutMapping(value = "/table/{entity_id}/column/change_type", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Alter Column Type")
	public BasicOutput changeColumnType(@PathVariable("entity_id") String entityId, @RequestBody EntityColumn column) {
		// 1. 체크 
		if(SysValueUtil.isEmpty(column.getEntityColDataType())) {
			throw ThrowUtil.newNotAllowedEmptyInfo("Entity Column Data Type"); 
		}
		
		// 2. Entity 조회 
		Resource resource = this.findAndCheckEntity(entityId);
		String tableName = resource.getTableName();
		
		// 3. Change type script
		String changeTypeTemplate = this.ddl.getDdlMapper().modifyColumnTemplate();
		String toColDataType = this.ddl.getDdlMapper().toDatabaseType(SysValueUtil.newMap("type,length", column.getEntityColDataType(), column.getEntityColLength()));
		Map<String, Object> changeTypeCol = SysValueUtil.newMap("name,type", column.getEntityColName(), toColDataType);
		List<Map<String, Object>> changeTypeColList = new ArrayList<Map<String, Object>>();
		changeTypeColList.add(changeTypeCol);
		
		// 4. Change type 호출 
		Map<String, Object> paramMap = SysValueUtil.newMap("tableName,modifyColumns", tableName, changeTypeColList);
		String msg = this.ddl.executeDDL(tableName, changeTypeTemplate, paramMap);
		
		if(msg != null) {
			throw new ElidomServiceException("Failed to change column type!", msg);
		}
		
		return new BasicOutput();
	}
	
	@PutMapping(value = "/table/{entity_id}/column/change_nullable", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Alter Column Type")
	public BasicOutput changeColumnNullable(@PathVariable("entity_id") String entityId, @RequestBody EntityColumn column) {
		// 1. 체크 
		if(SysValueUtil.isEmpty(column.getTableColName())) {
			throw ThrowUtil.newNotAllowedEmptyInfo("Table Column"); 
		}		
		
		// 2. Entity 조회 
		Resource resource = this.findAndCheckEntity(entityId);
		String tableName = resource.getTableName();
		
		// 3. Alter script
		String nullableTemplate = this.ddl.getDdlMapper().modifyNullableColumnTemplate();
		Map<String, Object> nullableCol = SysValueUtil.newMap("name,type,nullable", column.getTableColName(), column.getTableColDataType(), column.getTableColNullable() ? "NO" : "YES");
		List<Map<String, Object>> nullableColList = new ArrayList<Map<String, Object>>();
		nullableColList.add(nullableCol);
		
		// 4. Alter 호출 
		Map<String, Object> paramMap = SysValueUtil.newMap("tableName,nullableColumns", tableName, nullableColList);
		String msg = this.ddl.executeDDL(tableName, nullableTemplate, paramMap);
		
		if(msg != null) {
			throw new ElidomServiceException("Failed to change column nullable!", msg);
		}
		
		return new BasicOutput();
	}
	
	@PutMapping(value = "/table/{entity_id}/column/rename", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Rename Column")
	public BasicOutput renameColumn(@PathVariable("entity_id") String entityId, @RequestBody EntityColumn column) {
		// 1. 체크 
		if(SysValueUtil.isEmpty(column.getEntityColName())) {
			throw ThrowUtil.newNotAllowedEmptyInfo("Entity Column"); 
		}
		
		if(SysValueUtil.isEmpty(column.getTableColName())) {
			throw ThrowUtil.newNotAllowedEmptyInfo("Table Column"); 
		}		
		
		// 2. Entity 조회 
		Resource resource = this.findAndCheckEntity(entityId);
		String tableName = resource.getTableName();
		
		// 3. Rename script
		String renameTemplate = this.ddl.getDdlMapper().renameColumnTemplate();
		Map<String, Object> renameCol = SysValueUtil.newMap("nameFrom,nameTo", column.getTableColName(), column.getEntityColName());
		List<Map<String, Object>> renameColList = new ArrayList<Map<String, Object>>();
		renameColList.add(renameCol);
		
		// 4. Rename 호출 
		Map<String, Object> paramMap = SysValueUtil.newMap("tableName,renameColumns", tableName, renameColList);
		String msg = this.ddl.executeDDL(tableName, renameTemplate, paramMap);
		
		if(msg != null) {
			throw new ElidomServiceException("Failed to rename column!", msg);
		}
		
		return new BasicOutput();
	}
	
	@GetMapping(value = "/table/{entity_id}/indexes", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Search Table Index List that found by Entity Id")
	public List<EntityIndex> indexList(@PathVariable("entity_id") String entityId) {
		// 1. Entity 조회 
		Resource resource = this.findAndCheckEntity(entityId);
		String entityClassName = ResourceUtil.getEntityClassName(resource);
		Class<?> entityClass = ClassUtil.forName(entityClassName);
		String tableName = resource.getTableName();
		
		// 2. Entity Indexes, Table Indexes 조회 
		List<TableIdx> entityIndexes = this.ddl.getTableIndexes(entityClass);
		List<TableIdx> tableIndexes = this.ddl.getTableIndexes(tableName);
		
		// 3. Entity Index와 Table Index를 믹스 
		List<EntityIndex> indexList = new ArrayList<EntityIndex>();
		for(TableIdx entityIdx : entityIndexes) {
			EntityIndex index = new EntityIndex();
			index.setEntityName(resource.getName());
			index.setEntityIdxName(entityIdx.getIndexName());
			index.setEntityIdxUnique(entityIdx.getUnique());
			index.setEntityIdxFields(entityIdx.getIndexFields());
			indexList.add(index);
		}
		
		for(TableIdx tableIdx : tableIndexes) {
			EntityIndex prevIdx = this.findIdx(indexList, tableIdx);
			if(prevIdx == null) {
				prevIdx = new EntityIndex();
				indexList.add(prevIdx);
			}
			
			prevIdx.setTableName(tableName);
			prevIdx.setTableIdxName(tableIdx.getIndexName());
			prevIdx.setTableIdxUnique(tableIdx.getUnique());
			prevIdx.setTableIdxFields(tableIdx.getIndexFields());
		}
		
		// 4. 다른 점이 있는지 비교 
		for(EntityIndex idx : indexList) {
			idx.checkDifference();
		}
		
		return indexList;
	}
	
	private EntityIndex findIdx(List<EntityIndex> indexList, TableIdx tableIdx) {
		for(EntityIndex idx : indexList) {
			if(SysValueUtil.isEqual(tableIdx.getIndexName(), idx.getEntityIdxName())) {
				return idx;
			}
		}
		
		return null;
	}
	
	@PostMapping(value = "/table/{entity_id}/indexes", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Create Index to Table that found by Entity Id")
	public BasicOutput createIndex(@PathVariable("entity_id") String entityId, @RequestBody EntityIndex index) {
		Resource resource = this.findAndCheckEntity(entityId);
		String tableName = resource.getTableName();
		String indexTemplate = this.ddl.getDdlMapper().indexTemplate();
		Map<String, Object> indexMap = index.toMapByEntity();
		List<Map<String, Object>> indexMapList = new ArrayList<Map<String, Object>>(1);
		indexMapList.add(indexMap);
		Map<String, Object> paramMap = SysValueUtil.newMap("tableName,indexes,idxTableSpaceName", tableName, indexMapList, this.ddl.getIndexTableSpace());
		String msg = this.ddl.executeDDL(tableName, indexTemplate, paramMap);
		
		if(msg != null) {
			throw new ElidomServiceException("Failed to create index!", msg);
		}
		
		return new BasicOutput();
	}
	
	@DeleteMapping(value = "/table/{entity_id}/indexes/{index_name}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Remove Index of Table that found by Entity Id")
	public BasicOutput dropIndex(@PathVariable("entity_id") String entityId, @PathVariable("index_name") String indexName) {
		Resource resource = this.findAndCheckEntity(entityId);
		String tableName = resource.getTableName();
		String dropIndexTemplate = this.ddl.getDdlMapper().dropIndexTemplate();
		List<String> indexList = SysValueUtil.newStringList(indexName);
		Map<String, Object> paramMap = SysValueUtil.newMap("tableName,indexes", tableName, indexList);
		String msg = this.ddl.executeDDL(tableName, dropIndexTemplate, paramMap);
		
		if(msg != null) {
			throw new ElidomServiceException("Failed to drop index!", msg);
		}
		
		return new BasicOutput();
	}
	
	@GetMapping(value = "/table/query", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Execute Query and Return Results by Map List")
	public Object query(@RequestParam(name = "query", required = true) String query) {
		SysValueUtil.assertNotEmpty("terms.label.query", query);
		query = query.trim().toLowerCase();
		
		if(query.startsWith("select")) {
			return this.selectQuery(query);
		} else {
			return this.executeQuery(query);
		}		
	}
	
	@SuppressWarnings("rawtypes")
	@PostMapping(value = "/table/select_query", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Execute Query and Return Results by Map List")
	public List<Map> selectQuery(@RequestBody String query) {
		SysValueUtil.assertNotEmpty("terms.label.query", query);
		query = query.trim().toLowerCase();
		List<Map> results = this.queryManager.selectListBySql(query, null, Map.class, 0, 0);
		return results;
	}
	
	@SuppressWarnings("rawtypes")
	@GetMapping(value = "/table/execute_query", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Execute Query and Return Results by Map List")
	public Object executeQuery(@RequestParam(name = "query", required = true) String query) {
		SysValueUtil.assertNotEmpty("terms.label.query", query);
		query = query.trim().toLowerCase();
		List<Map> results = this.queryManager.selectListBySql(query, null, Map.class, 0, 0);
		return results;
	}
	
	/**
	 * Validation Check
	 * 
	 * @param id
	 * @return
	 */
	private Resource findAndCheckEntity(String id) {
		Resource resource = queryManager.select(true, Resource.class, id);
		String notModifiableList = SettingUtil.getValue(BaseConfigConstants.MODULE_NOT_MODIFIABLE_LIST, "base");
		List<String> moduleList = Arrays.asList(StringUtils.tokenizeToStringArray(notModifiableList, OrmConstants.COMMA));

		if (moduleList.contains(resource.getBundle())) {
			// throw new ElidomValidationException("Module [" + resource.getBundle() + "] can't be modified!");
		}
		
		return resource;
	}
	
	/**
	 * tableName이 존재하는지 체크 - 존재하면 에러 발생 
	 * 
	 * @param tableName
	 */
	private void checkTableAlreadyExist(String tableName) {
		if (this.ddl.isTableExist(tableName)) {
			throw ThrowUtil.newDataDuplicated("Table", tableName);
		}
	}
	
	/**
	 * javaType, length로 Type을 Postgres DB Type으로 변경.
	 * 
	 * @param javaType
	 * @param length
	 * @return
	 */
	private String toDatabaseType(String javaType, Integer length) {
		Map<String, Object> map = SysValueUtil.newMap("type,length", javaType, length);
		return this.ddl.getDdlMapper().toDatabaseType(map);
	}	
	
	/**
	 * sequence name
	 * 
	 * @param tableName
	 * @return
	 */
	private String getSequenceName(String tableName) {
		return tableName + "_id_seq";
	}
	
}