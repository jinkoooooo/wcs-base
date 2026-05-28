/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.orm.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.StringUtils;

import net.sf.common.util.ReflectionUtils;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.ddl.Ddl;
import xyz.elidom.dbist.dml.impl.DmlJdbc2;
import xyz.elidom.dbist.dml.jdbc.QueryMapper;
import xyz.elidom.dbist.metadata.TableCol;
import xyz.elidom.exception.server.ElidomDatabaseException;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.FormatUtil;
import xyz.elidom.util.ValueUtil;

/**
 * DDL 관련 유틸리티 
 * 
 * @author shortstop
 */
public class DdlUtil {
	
	/**
	 * Database Type Size Params
	 */
	private static final String DB_TYPE_SIZE_PARAM = "type,length";
	/**
	 * Logger
	 */
	protected static Logger logger = LoggerFactory.getLogger(DdlUtil.class);
	/**
	 * Table Column Row Mapper
	 */
	private static final RowMapper<TableCol> TABLECOLUMN_ROWMAPPER = new TableColumnRowMapper();
	
	/**
	 * TableColumn을 위한 RowMapper
	 * 
	 * @author shortstop
	 */
	static class TableColumnRowMapper implements RowMapper<TableCol> {
		public TableCol mapRow(ResultSet rs, int rowNum) throws SQLException {
			TableCol tableCol = new TableCol();
			tableCol.setName(rs.getString("name"));
			tableCol.setDataType(rs.getString("dataType"));
			tableCol.setLength(rs.getInt("length"));
			Boolean nullable = ValueUtil.isEqual(rs.getString("nullable"), "YES") ? true : false;
			tableCol.setNullable(nullable);
			return tableCol;
		}
	}	
	
	/**
	 * entity 클래스에는 필드가 존재하지만 테이블에는 존재하지 않는 필드들 자동 추가 
	 * 
	 * @param dbDomain
	 * @param entity
	 */
	public static int syncEntityColumns(String dbDomain, Class<?> entity) {
		Annotation tableAnn = AnnotationUtils.findAnnotation(entity, xyz.elidom.dbist.annotation.Table.class);
		Map<String, Object> tableInfo = (tableAnn == null) ? null : AnnotationUtils.getAnnotationAttributes(tableAnn);		
		
		// 1. Table Annotation이 없다면 스킵 
		if(tableAnn == null || tableInfo.get("name") == null) {
			return 0;
		} 

		// 2. 엔티티 클래스의 필드 정보 
		String tableName = (String)tableInfo.get("name");
		boolean ignoreDdl = (boolean)tableInfo.get("ignoreDdl");
		if(ignoreDdl) {
			return 0;
		}
		
		// 3. DDL, DML 추출  
		DmlJdbc2 dml = BeanUtil.get(DmlJdbc2.class);
		QueryMapper dmlQueryMapper = dml.getQueryMapper();
		List<Field> fieldList = ReflectionUtils.getFieldList(entity, false);
		
		// 4. 엔티티 클래스의 모든 필드 리스트를 돌면서 테이블에 존재하지 않은 컬럼 정보를 찾는다. 
		int changeCount = 0;
		List<String> skipFieldList = OrmConstants.ENTITY_FIELD_DEFAULT_IGNORED_LIST;
		
		for(Field field : fieldList) {
			if(!skipFieldList.contains(field.getName())) {
				Column columnAnn = field.getAnnotation(xyz.elidom.dbist.annotation.Column.class);
				if(columnAnn != null) {
					String sql = dmlQueryMapper.getQueryColumn();
					sql = StringUtils.replace(sql, "${domain}", dbDomain);
					changeCount += DdlUtil.syncTableColumn(tableName, columnAnn, field.getType().getSimpleName(), sql);
				}				
			}
		}
		
		// 5. 결과 로깅 
		if(changeCount > 0) {
			logger.info("Total [" + changeCount + "] columns changed at Table [" + tableName + "] ");
		}
		
		return changeCount;
	}
	
	/**
	 * 테이블에 컬럼이 존재하는지 체크하고 존재하지 않으면 컬럼을 추가 
	 * 
	 * @param tableName
	 * @param columnAnn
	 * @param colType
	 * @param columnCheckSql
	 * @return
	 */
	public static int syncTableColumn(String tableName, Column columnAnn, String colType, String columnCheckSql) {
		String colName = columnAnn.name().toLowerCase();
		
		try {
			String countSql = "select count(name) as cnt from (" + columnCheckSql + ") a";
			Integer count = DdlUtil.queryForObject(countSql, Integer.class, tableName, colName);
			
			// 1. 데이터베이스 테이블에 컬럼이 존재하지 않는다면 컬럼 추가 
			if(count == 0) {
				DdlUtil.addColumn(tableName, colName, colType, columnAnn.length(), columnAnn.nullable() ? "YES" : "NO");
				return 1;
				
			// 2. 데이터베이스 테이블에 컬럼이 존재한다면 컬럼 변경 : 사이즈가 이전 보다 커진 경우만 해당 
			} else {
				TableCol prevTabCol = DdlUtil.queryForObject(columnCheckSql, TABLECOLUMN_ROWMAPPER, tableName, colName);
				
				if(ValueUtil.isEqual(colType, "String") && ValueUtil.isNotEqual(xyz.elidom.dbist.annotation.ColumnType.TEXT, columnAnn.type())) {
					int prevColSize = prevTabCol.getLength();
					int newColSize = columnAnn.length();
					if(newColSize > prevColSize) {
//					if(newColSize != prevColSize) {
						DdlUtil.modifyColumn(tableName, colName, colType, newColSize);
						return 1;
					}
				}
			}

		} catch (Exception e) {
			logger.error("Error when sync table column : Table [" + tableName + "], Column [" + colName + "] - " + e.getMessage(), e);
		}
		
		return 0;
	}

	/**
	 * 컬럼 추가 
	 * 
	 * @param tableName
	 * @param colType
	 * @param colSize
	 * @param nullable
	 * @return
	 */
	public static String addColumn(String tableName, String colName, String colType, Integer colSize, String nullable) {
		Ddl ddl = BeanUtil.get(Ddl.class);
		String addColumnTemplate = ddl.getDdlMapper().addColumnTemplate();
		
		String toColDataType = ddl.getDdlMapper().toDatabaseType(ValueUtil.newMap(DB_TYPE_SIZE_PARAM, colType, colSize));
		Map<String, Object> addCol = ValueUtil.newMap("name,type,nullable", colName, toColDataType, nullable);
		List<Map<String, Object>> addColList = new ArrayList<Map<String, Object>>();
		addColList.add(addCol);
		
		Map<String, Object> paramMap = ValueUtil.newMap("tableName,addColumns", tableName, addColList);
		return executeDDL(tableName, addColumnTemplate, paramMap);
	}
	
	/**
	 * 컬럼 변경
	 * 
	 * @param tableName
	 * @param colName
	 * @param colType
	 * @param colSize
	 * @return
	 */
	public static String modifyColumn(String tableName, String colName, String colType, Integer colSize) {
		Ddl ddl = BeanUtil.get(Ddl.class);
		String changeTypeTemplate = ddl.getDdlMapper().modifyColumnTemplate();
		
		String toColDataType = ddl.getDdlMapper().toDatabaseType(ValueUtil.newMap(DB_TYPE_SIZE_PARAM, colType, colSize));
		Map<String, Object> changeTypeCol = ValueUtil.newMap("name,type", colName, toColDataType);
		List<Map<String, Object>> changeTypeColList = new ArrayList<Map<String, Object>>();
		changeTypeColList.add(changeTypeCol);
		
		Map<String, Object> paramMap = ValueUtil.newMap("tableName,modifyColumns", tableName, changeTypeColList);
		return executeDDL(tableName, changeTypeTemplate, paramMap);
	}
	
	/**
	 * 컬럼 nullable 변경
	 * 
	 * @param tableName
	 * @param colName
	 * @param colType
	 * @param nullable 'YES' or 'NO'
	 * return 
	 */
	public static String nullableColumn(String tableName, String colName, String nullable) {
		Ddl ddl = BeanUtil.get(Ddl.class);
		String nullableTemplate = ddl.getDdlMapper().modifyNullableColumnTemplate();
		
		Map<String, Object> nullableCol = ValueUtil.newMap("name,nullable", colName, nullable);
		List<Map<String, Object>> nullableColList = new ArrayList<Map<String, Object>>();
		nullableColList.add(nullableCol);
		
		Map<String, Object> paramMap = ValueUtil.newMap("tableName,nullableColumns", tableName, nullableColList);
		return executeDDL(tableName, nullableTemplate, paramMap);
	}
	
	/**
	 * 컬럼명 변경 
	 * 
	 * @param tableName
	 * @param fromColumn
	 * @param toColumn
	 * @return
	 */
	public static String renameColumn(String tableName, String fromColumn, String toColumn) {
		Ddl ddl = BeanUtil.get(Ddl.class);
		String renameTemplate = ddl.getDdlMapper().renameColumnTemplate();
		Map<String, Object> renameCol = ValueUtil.newMap("nameFrom,nameTo", FormatUtil.toUnderScore(fromColumn), FormatUtil.toUnderScore(toColumn));
		List<Map<String, Object>> renameColList = new ArrayList<Map<String, Object>>();
		renameColList.add(renameCol);
		
		Map<String, Object> paramMap = ValueUtil.newMap("tableName,renameColumns", tableName, renameColList);
		return executeDDL(tableName, renameTemplate, paramMap);
	}
	
	/**
	 * DDL 수행 
	 * 
	 * @param tableName
	 * @param templateDdl
	 * @param params
	 * @return
	 */
	public static String executeDDL(String tableName, String templateDdl, Map<String, Object> params) {
		try {
			DmlJdbc2 dml = BeanUtil.get(DmlJdbc2.class);
			String script = dml.getPreprocessor().process(templateDdl, params);
			String[] sqlArr = StringUtils.tokenizeToStringArray(script, OrmConstants.SEMI_COLON);
		
			for(String sql : sqlArr) {
				logger.info(sql);
				dml.getJdbcOperations().execute(sql);
			}
		} catch (Exception e) {
			String message = e.getCause().getMessage();
			logger.error(message + "[" + tableName + "]");
			throw new ElidomDatabaseException(message);
		}
		
		return null;		
	}
	
	/**
	 * query 수행 - 하나의 결과만 리턴 
	 * 
	 * @param sql
	 * @param requiredClass
	 * @param args
	 * @return
	 */
	public static <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
		DmlJdbc2 dml = BeanUtil.get(DmlJdbc2.class);
		return dml.getJdbcOperations().queryForObject(sql, requiredType, args);
	}
	
	/**
	 * query 수행 - 하나의 결과만 리턴 
	 * 
	 * @param sql
	 * @param rowMapper
	 * @param args
	 * @return
	 */
	public static <T> T queryForObject(String sql, RowMapper<T> rowMapper, Object... args) {
		DmlJdbc2 dml = BeanUtil.get(DmlJdbc2.class);
		return dml.getJdbcOperations().queryForObject(sql, rowMapper, args);
	}
	
	/**
	 * field, fieldAnnotation과 매핑된 entity Column의 타입을 리턴 
	 * 
	 * @param field
	 * @param fieldAnnotation
	 * @return
	 */
	public static String javaTypeToEntityColType(Field field, xyz.elidom.dbist.annotation.Column fieldAnnotation) {
		Class<?> javaType = field.getType();
		
		if(fieldAnnotation != null && fieldAnnotation.type() != null) {
			if(javaType == String.class && ValueUtil.isEqualIgnoreCase(fieldAnnotation.type().toString(), OrmConstants.DATA_TYPE_TEXT)) {
				return OrmConstants.DATA_TYPE_TEXT;
			}
			
			if(javaType == Date.class && ValueUtil.isEqualIgnoreCase(fieldAnnotation.type().toString(), OrmConstants.DATA_TYPE_DATE)) {
				return OrmConstants.DATA_TYPE_DATE;
			}
			
			if(javaType == Date.class && ValueUtil.isEqualIgnoreCase(fieldAnnotation.type().toString(), OrmConstants.DATA_TYPE_TIME)) {
				return OrmConstants.DATA_TYPE_TIME;
			}
		}
		
		if(javaType == String.class) {
			return OrmConstants.DATA_TYPE_STRING;
			
		} else if(javaType == Integer.class) {
			return OrmConstants.DATA_TYPE_INTEGER;
			
		} else if(javaType == Long.class) {
			return OrmConstants.DATA_TYPE_LONG;
			
		} else if(javaType == Double.class) {
			return OrmConstants.DATA_TYPE_DOUBLE;
			
		} else if(javaType == Float.class) {
			return OrmConstants.DATA_TYPE_FLOAT;
			
		} else if(javaType == BigDecimal.class) {
			return OrmConstants.DATA_TYPE_DECIMAL;
			
		} else if(javaType == Date.class || javaType == Timestamp.class) {
			return OrmConstants.DATA_TYPE_DATETIME;
			
		} else if(javaType == Boolean.class) {
			return OrmConstants.DATA_TYPE_BOOLEAN;
			
		} else {
			return OrmConstants.DATA_TYPE_STRING;
		}
	}
	
}
