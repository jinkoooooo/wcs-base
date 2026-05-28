/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.base.rest;

import java.io.BufferedReader;
import java.sql.Clob;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.dbist.ddl.Ddl;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.dbist.metadata.TableCol;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.util.AssertUtil;
import xyz.elidom.sys.util.SysValueUtil;
import xyz.elidom.util.BeanUtil;

@RestController
@Transactional
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/dml")
@ServiceDesc(description = "Handling Database DML API")
public class DmlController {
	/**
	 * Logger
	 */
	protected Logger logger = LoggerFactory.getLogger(DdlController.class);

	/**
	 * Table Data Query
	 */
	private static final String TABLE_DATA_SQL = "select * from ";
	/**
	 * Select 쿼리 타입  
	 */
	private static final String SELECT_DML_TYPE = "select";
	/**
	 * Select를 제외한 수행 DML 리스트 
	 */
	private static final List<String> EXECUTE_DML_TYPE = SysValueUtil.newStringList("insert", "delete", "update");
	
	@Autowired
	private IQueryManager queryManager;
	
	@Autowired
	private Ddl ddl;
	
	@SuppressWarnings("unchecked")
	@GetMapping(value = "/table/{table_name}/data", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Search table data list of table")
	public Page<?> tableData(
		@PathVariable("table_name") String tableName,
		@RequestParam(name = "page", required = false) Integer page,
		@RequestParam(name = "limit", required = false) Integer limit) {
		
		AssertUtil.assertNotEmpty("terms.label.table_name", tableName);
		
		// 1. 테이블 컬럼 조회 
		DdlController ddlCtrl = BeanUtil.get(DdlController.class);
		List<TableCol> columns = ddlCtrl.tableColumn(tableName);
		String orderField = null;
		
		// 2. order by 컬럼 설정 
		for(TableCol col : columns) {
			if(SysValueUtil.isEqualIgnoreCase(SysConstants.TABLE_FIELD_ID, col.getName())) {
				orderField = SysConstants.TABLE_FIELD_ID;
				break;
			}
		}
		
		if(orderField == null) {
			for(TableCol col : columns) {
				if(SysValueUtil.isEqualIgnoreCase(SysConstants.TABLE_FIELD_CREATED_AT, col.getName())) {
					orderField = SysConstants.TABLE_FIELD_CREATED_AT;
					break;
				}
			}
		}
		
		if(orderField == null) {
			orderField = columns.get(0).getName();
		}
		
		// 3. 테이블 데이터 조회
		String sql = new StringBuffer(TABLE_DATA_SQL).append(tableName).append(" order by ").append(orderField).toString();
		Page<?> result = this.queryManager.selectPageBySql(sql, null, Map.class, (page == null) ? 1 : page, (limit == null) ? 100 : limit);
		List<Map<String, Object>> list = (List<Map<String, Object>>)result.getList();
		
		// 4. 각 row 별 필드 데이터 정제 
		this.fieldValueToString(list);
		return result;
	}
	
	@PostMapping(value = "/table/query", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Execute Query and Return Results by Map List")
	public Object query(@RequestBody Map<String, String> params) {
		String query = params.get("sql");
		SysValueUtil.assertNotEmpty("terms.label.query", query);
		query = query.trim();
		String queryType = query.substring(0, 6).toLowerCase();
		
		// 1. DML : Select
		if(SysValueUtil.isEqual(queryType, SELECT_DML_TYPE)) {
			return this.selectQuery(query, params.get("page"), params.get("limit"));
			
		// 2. DML : Insert, Update, Delete
		} else if(EXECUTE_DML_TYPE.contains(queryType)) {
			return this.executeQuery(queryType, query);
			
		// 3. DDL : Alter table ....
		} else {
			return this.executeDdl(params);
		}
	}
	
	/**
	 * 조회 쿼리를 실행한 후 결과를 리턴 
	 * 
	 * @param params
	 * @return
	 */
	@PostMapping(value = "/select_query", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Execute Query and Return Results by Map List")
	public Page<?> selectQuery(@RequestBody Map<String, String> params) {
		String query = params.get("sql");
		SysValueUtil.assertNotEmpty("terms.label.query", query);
		query = query.trim();
		Page<?> results = this.selectQuery(query, params.get("page"), params.get("limit"));
		return results;
	}
	
	/**
	 * Insert, Update, Delete 쿼리를 수행한 후 적용 결과 메시지를 리턴 
	 * 
	 * @param params
	 * @return
	 */
	@PostMapping(value = "/execute_query", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Execute Query and Return Results by Map List")
	public List<?> executeQuery(@RequestBody Map<String, String> params) {
		String query = params.get("sql");
		SysValueUtil.assertNotEmpty("terms.label.query", query);
		query = query.trim();
		
		String queryType = query.substring(0, 6).toLowerCase();
		List<?> result = this.executeQuery(queryType, query);
		return result;
	}
	
	/**
	 * DDL 쿼리를 수행한 후 적용 결과 메시지를 리턴 
	 * 
	 * @param params
	 * @return
	 */
	@PostMapping(value = "/execute_ddl", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Execute Query and Return Results by Map List")
	public List<?> executeDdl(@RequestBody Map<String, String> params) {
		String query = params.get("sql");
		SysValueUtil.assertNotEmpty("terms.label.query", query);
		query = query.trim();
		
		String message = this.ddl.executeDDL(OrmConstants.EMPTY_STRING, query, null);
		Map<String, Object> result = SysValueUtil.newMap("_type_,_message_", "DDL", message == null ? "DDL was executed successfully!" : message);
		return SysValueUtil.newList(result);
	}
	
	/**
	 * select query 수행 
	 * 
	 * @param query
	 * @param pageObj
	 * @param limitObj
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Page<?> selectQuery(String query, Object pageObj, Object limitObj) {
		SysValueUtil.assertNotEmpty("terms.label.query", query);
		query = query.trim().toLowerCase();
		int page = (pageObj != null) ? SysValueUtil.toInteger(pageObj) : 1;
		int limit = (limitObj != null) ? SysValueUtil.toInteger(limitObj) : 100;
		
		// 1. query 데이터 조회 
		Page<?> pageResult = this.queryManager.selectPageBySql(query, null, Map.class, page, limit);
		// 2. 각 row별 필드값 정제 
		List<Map<String, Object>> list = (List<Map<String, Object>>)pageResult.getList();
		this.fieldValueToString(list);
		// 3. 리턴 
		return pageResult;
	}
	
	/**
	 * 쿼리 (insert, update, delete) 수행 
	 * 
	 * @param queryType
	 * @param query
	 * @return
	 */
	private List<?> executeQuery(String queryType, String query) {
		int applyCount = this.queryManager.executeBySql(query, null);
		StringBuffer message = new StringBuffer("[").append(applyCount).append("] ").append(applyCount > 1 ? "records are " : "record is ").append(queryType).append("d!");
		Map<String, Object> result = SysValueUtil.newMap("_type_,_message_", queryType, message.toString());
		return SysValueUtil.newList(result);
	}
	
	/**
	 * Clob to String
	 * 
	 * @param clob
	 * @return
	 */
	private String clobToString(Clob clob) {
		StringBuffer strOut = new StringBuffer();
		
		try {
			BufferedReader br = new BufferedReader(clob.getCharacterStream());
			String str = null;
		
			while ((str = br.readLine()) != null) {
				strOut.append(str);
			}
		} catch (Exception e) {
			this.logger.error("Failed to clob to string " + e.getMessage());
		}
		
		return strOut.toString();
	}
	
	/**
	 * 필드 값을 모두 String으로 변환 
	 * 
	 * @param list
	 */
	private void fieldValueToString(List<Map<String, Object>> list) {
		for(Map<String, Object> data : list) {
			Iterator<String> keyIter = data.keySet().iterator();
			while(keyIter.hasNext()) {
				String key = keyIter.next();
				Object value = data.get(key);
				
				if(value != null) {
					if(value instanceof Clob) {
						Clob clob = (Clob)value;
						data.put(key, this.clobToString(clob));
						
					} else if(!(value instanceof String)) {
						data.put(key, value.toString());
					}					
				}
			}
		}		
	}
	
}
