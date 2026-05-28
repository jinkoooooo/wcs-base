/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.base.rest;

import java.io.File;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.base.entity.Resource;
import xyz.elidom.base.entity.ResourceColumn;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.ddl.Ddl;
import xyz.elidom.dbist.metadata.Column;
import xyz.elidom.dbist.metadata.Table;
import xyz.elidom.exception.server.ElidomServiceException;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.entity.basic.ElidomStamp;
import xyz.elidom.orm.entity.basic.ElidomStampHook;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sys.SysConfigConstants;
import xyz.elidom.sys.SysMessageConstants;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.engine.ITemplateEngine;
import xyz.elidom.sys.system.engine.velocity.VelocityTemplateEngine;
import xyz.elidom.sys.system.service.params.BasicOutput;
import xyz.elidom.sys.util.AssertUtil;
import xyz.elidom.sys.util.FileUtil;
import xyz.elidom.sys.util.MessageUtil;
import xyz.elidom.sys.util.SettingUtil;
import xyz.elidom.sys.util.ThrowUtil;
import xyz.elidom.sys.util.SysValueUtil;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ClassUtil;
import xyz.elidom.util.ValueUtil;

@RestController
@Transactional
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/code_generator")
@ServiceDesc(description = "Code Generation Service API")
public class CodeGeneratorController {

	/**
	 * Logger
	 */
	protected Logger logger = LoggerFactory.getLogger(CodeGeneratorController.class);

	@Autowired
	private IQueryManager queryManager;

	@Autowired
	private Ddl ddl;

	/**
	 * Line Separator
	 */
	private static final String ENTER = "\n";
	/**
	 * Tab Character
	 */
	private static final String TAB = "\t";
	/**
	 * Space Character
	 */
	private static final String SPACE = " ";

	/**
	 * packageName, fileName으로 템플릿 파일을 읽은 후 variables로 Velocity Template을 실행하여 결과를 리턴한다.
	 * 
	 * @param packageName
	 * @param fileName
	 * @param variables
	 * @return
	 */
	private String processTemplate(String packageName, String fileName, Map<String, Object> variables) {
		String templatePath = packageName.replace(OrmConstants.DOT, OrmConstants.SLASH);
		String templateContent = FileUtil.readClasspathFile(templatePath, fileName);
		ITemplateEngine templateEngine = BeanUtil.get(VelocityTemplateEngine.class);
		StringWriter writer = new StringWriter();
		templateEngine.processTemplate(templateContent, writer, variables, null);
		return writer.toString();
	}

	@PostMapping(value = "/code/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Generate JAVA Business Logic (Entity, Database Table, Controller).")
	public boolean generateAllByEntity(@PathVariable("id") String id) throws Exception {
		// Entity Generate
		this.generateEntity(id);
		// Table Generate
		this.generateTableByEntityName(id);
		// Controller Generate
		this.generateController(id);
		return true;
	}

	@SuppressWarnings("unchecked")
	@PostMapping("/code/entity/{id}")
	@ApiDesc(description = "Generate Entity by Resource Id.")
	public BasicOutput generateEntity(@PathVariable("id") String id) throws Exception {
		// Validation Check
		Resource resource = this.validationCheck(id);

		String tableName = resource.getTableName();
		AssertUtil.assertNotEmpty("terms.label.table_name", tableName);

		Map<String, Object> velocityParamMap = new HashMap<String, Object>();
		
		StringBuilder fieldCont = new StringBuilder();
		StringBuilder getterSetterCont = new StringBuilder();
		StringBuilder refFieldCont = new StringBuilder();
		StringBuilder refGetterSetterCont = new StringBuilder();
		
		List<String> refFieldList = new ArrayList<String>();
		Set<String> refPackagePahList = new HashSet<String>();
		StringJoiner uniqueFields = new StringJoiner(",");
		StringJoiner uniqueIndexFields = new StringJoiner(",");

		String idStrategy = resource.getIdType();
		List<ResourceColumn> columns = resource.resourceColumns();

		for (ResourceColumn column : columns) {
			String columnName = column.getName();
			String fieldName = SysValueUtil.toCamelCase(columnName, '_');

			// Unique Index
			if (column.getUniqRank() > 0) {
				if (SysValueUtil.isNotEqual(fieldName, "domainId")) {
					refFieldList.add(fieldName);
				}

				uniqueFields.add(fieldName);
				uniqueIndexFields.add(columnName);
			}
		}

		if (!refFieldList.isEmpty()) {
			String fieldName = SysValueUtil.toCamelCase(resource.getIdField(), '_');
			refFieldList.add(fieldName);
		} else {
			if (SysValueUtil.isNotEmpty(resource.getTitleField())) {
				String fieldNameId = SysValueUtil.toCamelCase(resource.getIdField(), '_');
				String fieldNameTitle = SysValueUtil.toCamelCase(resource.getTitleField(), '_');
				refFieldList.add(fieldNameId);
				refFieldList.add(fieldNameTitle);
			}
		}

		String idDataType = "Long";
		String module = resource.getBundle();
		String entityName = resource.getName();
		String sequence = tableName + "_id_seq";

		// Title이 비어 있을 경우, Ref Class를 생성하지 않음.
		boolean isDetail = SysValueUtil.isNotEmpty(resource.getMasterId());
		// boolean isGenerateRefClass = !isDetail && !refFieldList.isEmpty();
		boolean isGenerateRefClass = false;

		// Master - ElidomStamp, Detail - UserTimeStamp
		Map<String, Object> parentsInfo = parentsInfo(columns);
		String parentsName = (String) parentsInfo.get("parentsName");
		List<String> parentsFieldList = (List<String>) parentsInfo.get("fields");

		int indexSeq = 0;
		for (ResourceColumn column : columns) {
			String columnName = column.getName();
			String fieldName = SysValueUtil.toCamelCase(columnName, '_');

			// 상속받은 Field 제외
			if (parentsFieldList != null && parentsFieldList.contains(fieldName)) {
				continue;
			}

			String methodName = parseHeadCase(fieldName, true);
			int length = column.getColSize();
			boolean isId = fieldName.equalsIgnoreCase("id");
			boolean nullable = column.getNullable();

			String type = "Long";
			if ((isId || fieldName.endsWith("Id")) && column.getColType().startsWith("string")) {
				type = "String";
			} else {
				type = parseResourceType(column.getColType());
			}

			// ID와 Title 컬럼의 값이 동일 할 경우, Ref Class 생성을 위한 정보 추출 여부 설정.
			boolean isRefField = isGenerateRefClass && refFieldList.contains(fieldName);

			/**
			 * Set Annotation
			 */
			if (isId) {
				idDataType = type;
				if (idStrategy.equals(GenerationRule.COMPLEX_KEY)) {
					appendWithTabAndEnter(fieldCont, "@Ignore");
					velocityParamMap.put("ignore", true);
				} else {
					appendWithTabAndEnter(fieldCont, "@PrimaryKey");

					if (idStrategy.equals(GenerationRule.AUTO_INCREMENT)) {
						appendWithTabAndEnter(fieldCont, "@Sequence(name = \"", sequence, "\")");
					} else if (idDataType.equals("String")) {
						String idColumnAnno = this.appendColumnAnnotation(columnName, false, length);
						appendWithTabAndEnter(fieldCont, idColumnAnno);
					}

					if (isRefField) {
						appendWithTabAndEnter(refFieldCont, "@PrimaryKey");
						if (idStrategy.equals(GenerationRule.AUTO_INCREMENT)) {
							appendWithTabAndEnter(refFieldCont, "@Sequence(name = \"", sequence, "\")");
						}
					}
				}
			} else {
				// Complex-key 타입일 경우, Unique Field에 @PrimaryKey 추가.
				if (idStrategy.equals(GenerationRule.COMPLEX_KEY)) {
					List<String> uniqueFieldList = Arrays.asList(StringUtils.tokenizeToStringArray(uniqueFields.toString(), ","));

					if (uniqueFieldList.contains(fieldName))
						appendWithTabAndEnter(fieldCont, "@PrimaryKey");
				}

				String value = null;
				
				if (column.getVirtualField()) {
					value = "@Ignore";
					velocityParamMap.put("ignore", true);
				} else {
					value = this.appendColumnAnnotation(columnName, column.getColType(), nullable, length);
				}

				appendWithTabAndEnter(fieldCont, value);

				if (isRefField) {
					appendWithTabAndEnter(refFieldCont, value);
				}
			}

			/**
			 * Set Field
			 */
			appendWithTabAndEnter(fieldCont, "private ", type, SPACE, fieldName, ";", ENTER);

			/**
			 * Set Getter
			 */
			appendWithTabAndEnter(getterSetterCont, "public ", type, " get", methodName, "() {");
			appendWithTabAndEnter(getterSetterCont, TAB, "return ", fieldName, ";");
			appendWithTabAndEnter(getterSetterCont, "}", ENTER);

			/**
			 * Set Setter
			 */
			appendWithTabAndEnter(getterSetterCont, "public void set", methodName, "(", type, SPACE, fieldName, ")", " {");
			appendWithTabAndEnter(getterSetterCont, TAB, "this.", fieldName, " = ", fieldName, ";");
			appendWithTabAndEnter(getterSetterCont, "}", ENTER);

			// ClassRef 생성을 위한 Logic
			if (isRefField) {
				// Field
				appendWithTabAndEnter(refFieldCont, "private ", type, SPACE, fieldName, ";", ENTER);

				// Getter
				appendWithTabAndEnter(refGetterSetterCont, "public ", type, " get", methodName, "() {");
				appendWithTabAndEnter(refGetterSetterCont, TAB, "return ", fieldName, ";");
				appendWithTabAndEnter(refGetterSetterCont, "}", ENTER);

				// Setter
				appendWithTabAndEnter(refGetterSetterCont, "public void set", methodName, "(", type, SPACE, fieldName, ")", " {");
				appendWithTabAndEnter(refGetterSetterCont, TAB, "this.", fieldName, " = ", fieldName, ";");
				appendWithTabAndEnter(refGetterSetterCont, "}", ENTER);
			}

			// @Relation 정보 생성
			String refType = column.getRefType();
			if (SysValueUtil.isNotEmpty(refType) && (refType.equals("Entity") || refType.equals("Menu")) && columnName.contains("_")) {
				String refName = column.getRefName();

				if (SysValueUtil.isEmpty(refName)) {
					continue;
				}

				String refFieldName = columnName.substring(0, columnName.lastIndexOf("_"));
				refFieldName = SysValueUtil.toCamelCase(refFieldName, '_');
				String refMethodName = parseHeadCase(refFieldName, true);

				Resource resourceParam = new Resource(Domain.currentDomain().getId(), refName);
				List<Resource> refResourceList = queryManager.selectList(true, Resource.class, resourceParam);

				if (refResourceList.size() > 1) {
					throw ThrowUtil.newDataDuplicated("terms.menu.Entity", refName);
				}

				Resource refResource = refResourceList.get(0);
				String refBasePath = SysValueUtil.getBasePath(refResource.getBundle());
				String refPackage = refBasePath + ".entity.relation." + refName + "Ref";
				refPackagePahList.add(refPackage);

				appendWithTabAndEnter(fieldCont, "@Relation(field = \"" + fieldName + "\")");
				appendWithTabAndEnter(fieldCont, "private ", refName + "Ref", SPACE, refFieldName, ";", ENTER);

				// Getter
				appendWithTabAndEnter(getterSetterCont, "public ", refName + "Ref", " get", refMethodName, "() {");
				appendWithTabAndEnter(getterSetterCont, TAB, "return ", refFieldName, ";");
				appendWithTabAndEnter(getterSetterCont, "}", ENTER);

				// Setter
				appendWithTabAndEnter(getterSetterCont, "public void set", refMethodName, "(", refName + "Ref", SPACE, refFieldName, ")", " {");
				appendWithTabAndEnter(getterSetterCont, TAB, "this.", refFieldName, " = ", refFieldName, ";", "\n");

				appendWithTabAndEnter(getterSetterCont, TAB, "if(this.", refFieldName, " != null) {");
				appendWithTabAndEnter(getterSetterCont, TAB, TAB, type, " refId = this.", refFieldName, ".getId();");

				if (type.equals("Long")) {
					appendWithTabAndEnter(getterSetterCont, TAB, TAB, "if (refId != null && refId > 0L)");
				} else {
					appendWithTabAndEnter(getterSetterCont, TAB, TAB, "if (refId != null)");
				}

				appendWithTabAndEnter(getterSetterCont, TAB, TAB, TAB, "this.", fieldName, " = refId;");
				appendWithTabAndEnter(getterSetterCont, TAB, "}");
				if (!type.equals("Long")) {
					appendWithTabAndEnter(getterSetterCont, ENTER, TAB, TAB, "if(this.", fieldName, " == null) {");
					appendWithTabAndEnter(getterSetterCont, TAB, TAB, "this.", fieldName, " = \"\";");
					appendWithTabAndEnter(getterSetterCont, TAB, "}");
				}
				appendWithTabAndEnter(getterSetterCont, "}", ENTER);
			}

			velocityParamMap.put(parseHeadCase(type, false), type);
		}

		StringBuilder indexCont = new StringBuilder();
		if (uniqueIndexFields.length() > 0) {
			indexCont.append(", indexes = {").append(ENTER).append(TAB).append("@Index(name = \"ix_");
			indexCont.append(tableName).append("_").append(Integer.toString(indexSeq++)).append("\"");
			indexCont.append(", columnList = \"").append(uniqueIndexFields.toString()).append("\"");
			indexCont.append(", unique = true)").append(ENTER).append("}");
			velocityParamMap.put("addIndex", true);
		}
		indexCont.append(")");

		velocityParamMap.put("index", indexCont.toString());
		velocityParamMap.put("parentsEntity", parentsName);
		velocityParamMap.put("module", SysValueUtil.getBasePath(module));
		velocityParamMap.put("tableName", tableName);
		velocityParamMap.put("entity", entityName);
		velocityParamMap.put("fieldCont", TAB + fieldCont.toString().trim());
		velocityParamMap.put("getterSetterCont", TAB + getterSetterCont.toString().trim());
		velocityParamMap.put("idSequence", SysValueUtil.isEqual(idStrategy, GenerationRule.AUTO_INCREMENT));
		velocityParamMap.put("uid", SysValueUtil.generateUid(18));
		velocityParamMap.put("meaningfulFields", "");
		velocityParamMap.put("uniqueFields", "");
		velocityParamMap.put("isExtEntity", resource.getExtEntity());

		if (uniqueFields.length() > 0) {
			velocityParamMap.put("uniqueFields", ", uniqueFields=\"" + uniqueFields.toString() + "\"");
		}

		if (refPackagePahList.size() > 0) {
			velocityParamMap.put("refPackagePath", refPackagePahList);
		}

		String idType = "";
		if (SysValueUtil.isNotEmpty(idStrategy)) {
			StringBuilder sb = new StringBuilder(", idStrategy = ");
			if (SysValueUtil.isEqual(idStrategy, GenerationRule.AUTO_INCREMENT)) {
				idType = sb.append("GenerationRule.AUTO_INCREMENT").toString();
			} else if (SysValueUtil.isEqual(idStrategy, GenerationRule.MEANINGFUL)) {
				idType = sb.append("GenerationRule.MEANINGFUL").toString();
				velocityParamMap.put("meaningfulFields", ", meaningfulFields=\"" + uniqueFields.toString() + "\"");
			} else if (SysValueUtil.isEqual(idStrategy, GenerationRule.UUID)) {
				idType = sb.append("GenerationRule.UUID").toString();
			} else if (SysValueUtil.isEqual(idStrategy, GenerationRule.COMPLEX_KEY)) {
				idType = sb.append("GenerationRule.COMPLEX_KEY").toString();
			} else {
				idType = sb.append("\"").append(idStrategy).append("\"").toString();
			}
		}

		velocityParamMap.put("idType", idType);
		BasicOutput result = new BasicOutput();

		try {
			String content = this.processTemplate("templates.code", "entity.vm", velocityParamMap);

			// File 생성
			StringJoiner contextPath = new StringJoiner(File.separator);
			List<String> modulePath = getModulePath(module);
			for (String path : modulePath) {
				contextPath.add(path);
			}

			contextPath.add("src").add("main").add("java");

			String detailPath = SysValueUtil.getBasePath(module);
			String[] values = StringUtils.tokenizeToStringArray(detailPath, ".");
			for (String value : values) {
				contextPath.add(value);
			}

			contextPath.add("entity");
			String dirPath = getDirPath(module, contextPath.toString(), false);
			FileUtil.createFile(dirPath, entityName + ".java", content);

			// Master 객체일 경우, Ref Class 생성.
//			if (isGenerateRefClass) {
//				Map<String, Object> refMap = new HashMap<String, Object>();
//				refMap.put("module", SysValueUtil.getBasePath(module));
//				refMap.put("tableName", tableName);
//
//				// auto-increment 타입이고 uniqueFields가 존재하면 UniqueFieldsNumberIdRef
//				if (SysValueUtil.isEqual(idStrategy, GenerationRule.AUTO_INCREMENT) && uniqueFields.length() > 0) {
//					refMap.put("parentsEntity", "xyz.elidom.orm.entity.relation.UniqueFieldsNumberIdRef");
//				}
//				refMap.put("isRef", true);
//				refMap.put("entity", entityName + "Ref");
//				refMap.put("fieldCont", TAB + refFieldCont.toString().trim());
//				refMap.put("getterSetterCont", TAB + refGetterSetterCont.toString().trim());
//				refMap.put("uid", SysValueUtil.generateUid(18));
//				refMap.put("idType", idType);
//				refMap.put("idSequence", SysValueUtil.isEqual(idStrategy, GenerationRule.AUTO_INCREMENT));
//				refMap.put("hasUniqueFields", uniqueFields.length() > 0);
//
//				String refContent = this.processTemplate("templates.code", "entity.vm", refMap);
//				StringJoiner refPath = new StringJoiner(File.separator);
//				refPath.add(dirPath).add("relation");
//				FileUtil.createFile(refPath.toString(), entityName + "Ref.java", refContent);
//			}

			result.setMsg(content);

		} catch (Exception e) {
			throw new ElidomServiceException(SysMessageConstants.GENERATE_FAIL, "Failed to generate entity!", e);
		}

		return result;
	}

	@DeleteMapping("/code/entity/{id}")
	@ApiDesc(description = "Delete Entity by Resource Id.")
	public boolean deleteEntity(@PathVariable("id") String id) throws Exception {
		// Validation Check
		Resource resource = this.validationCheck(id);
		AssertUtil.assertNotEmpty("terms.label.resource_name", resource.getName());

		// Module Project 기본 경로 추출
		StringJoiner path = new StringJoiner(File.separator);
		List<String> modulePath = getModulePath(resource.getBundle());
		for (String detail : modulePath) {
			path.add(detail);
		}

		path.add("src").add("main").add("java");

		String detailPath = SysValueUtil.getBasePath(resource.getBundle());
		String[] values = StringUtils.tokenizeToStringArray(detailPath, ".");
		for (String str : values) {
			path.add(str);
		}

		path.add("entity");

		String fullPath = new StringBuilder().append(path).append(File.separator).append(resource.getName()).append(".java").toString();
		File file = new File(fullPath);

		if (file.isFile()) {
			file.delete();
		}

		return true;
	}

	@PostMapping(value = "/code/table_by_entity/{entity_class_name}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	@ApiDesc(description = "Generate Table By Entity Class Name")
	public BasicOutput generateTableByEntityName(@PathVariable("entity_class_name") String entityClassName) throws Exception {
		Class<?> entityClass = ClassUtil.forName(entityClassName);
		Annotation tableAnn = AnnotationUtils.findAnnotation(entityClass, xyz.elidom.dbist.annotation.Table.class);
		String tableName = (String) AnnotationUtils.getAnnotationAttributes(tableAnn).get("name");
		this.checkTableAlreadyExist(tableName);

		String resultStr = this.ddl.createTable(entityClass);
		BasicOutput result = new BasicOutput();
		if (resultStr == null) {
			result.setMsg("Table [" + tableName + "] is created!");
			return result;
		} else {
			throw new ElidomServiceException("Failed to generate table!", resultStr);
		}
	}

	@PostMapping(value = "/code/table/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	@ApiDesc(description = "Generate Table By Resource Id")
	public BasicOutput generateTable(@PathVariable("id") String id) throws Exception {
		// Validation Check
		Resource resource = this.validationCheck(id);
		String tableName = resource.getTableName();
		AssertUtil.assertNotEmpty("terms.label.table_name", tableName);
		this.checkTableAlreadyExist(tableName);

		String sequenceName = tableName + "_id_seq";
		StringJoiner primaryFields = new StringJoiner(",");
		StringJoiner uniqueIndexFields = new StringJoiner(",");
		List<ResourceColumn> columns = resource.resourceColumns();
		// List<ResourceColumn> columns = BeanUtil.get(ResourceController.class).resourceColumns(id).getItems();

		if (SysValueUtil.isEmpty(columns)) {
			throw new ElidomServiceException("Entity Column is not exist!");
		}

		List<Map<String, String>> dbColumnsInfo = new ArrayList<Map<String, String>>();
		for (ResourceColumn column : columns) {
			Map<String, String> dbColumnInfo = new HashMap<String, String>();
			String colName = column.getName();
			boolean nullable = (column.getNullable() != null && column.getNullable() == true);

			// Set Primary Key && Sequence
			if (SysValueUtil.isEqual(colName, "id")) {
				dbColumnInfo.put("primaryKey", colName);
				primaryFields.add(colName);

				if (SysValueUtil.isEqual(GenerationRule.AUTO_INCREMENT, resource.getIdType())) {
					dbColumnInfo.put("sequenceAble", "true");
				}
			}

			// Unique Index 정보 추출
			if (column.getUniqRank() != null && column.getUniqRank() > 0) {
				uniqueIndexFields.add(colName);
			}

			// Set Column Info
			dbColumnInfo.put("name", colName);
			dbColumnInfo.put("nullable", nullable ? "" : "NOT NULL");
			dbColumnInfo.put("col_type", this.toDatabaseType(column));
			dbColumnsInfo.add(dbColumnInfo);
		}

		// unique index 처리 ...
		int idxSeq = 0;
		List<Map<String, Object>> indexList = new ArrayList<Map<String, Object>>();
		if (uniqueIndexFields.length() > 0) {
			Map<String, Object> uniqueIndexMap = new HashMap<String, Object>();
			uniqueIndexMap.put("unique", true);
			uniqueIndexMap.put("name", "ix_" + tableName + "_" + idxSeq++);
			uniqueIndexMap.put("columnList", uniqueIndexFields.toString());
			indexList.add(uniqueIndexMap);
		}

		Map<String, Object> map = new HashMap<String, Object>();
		map.put("account", this.ddl.getAccount());
		map.put("tableName", tableName);
		map.put("sequenceName", sequenceName);
		map.put("columns", dbColumnsInfo);
		map.put("primaryKeys", primaryFields.toString());
		map.put("pkExist", primaryFields.length()>1);
		map.put("indexes", indexList);

		BasicOutput result = new BasicOutput();
		String template = this.ddl.getDdlMapper().getDdlTemplate();
		String resultStr = this.ddl.executeDDL(tableName, template, map);

		if (resultStr == null) {
			result.setMsg("Table [" + tableName + "] is created.");
		} else {
			throw new ElidomServiceException("Failed to generate table!", resultStr);
		}

		return result;
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

	@DeleteMapping(value = "/code/table/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	@ApiDesc(description = "Delete Table By Resource Id")
	public BasicOutput deleteTable(@PathVariable("id") String id) throws Exception {
		// Validation Check
		Resource resource = this.validationCheck(id);
		String tableName = resource.getTableName();
		AssertUtil.assertNotEmpty("terms.label.table_name", tableName);
		String sequenceName = tableName + "_id_seq";

		BasicOutput result = new BasicOutput();
		String resultStr = this.ddl.dropTable(tableName, sequenceName, null);

		if (resultStr == null) {
			result.setMsg("Table [" + tableName + "] was dropped.");
		} else {
			throw new ElidomServiceException("Failed to drop table!", resultStr);
		}

		return result;
	}

	@PutMapping(value = "/code/table/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	@ApiDesc(description = "Alter Table By Resource Id")
	public boolean alterTable(@PathVariable("id") String id) throws Exception {
		// Validation Check
		Resource resource = this.validationCheck(id);
		String tableName = resource.getTableName();
		AssertUtil.assertNotEmpty("terms.label.table_name", tableName);

		/**
		 * Entity Columns Info
		 */
		Map<String, ResourceColumn> entityColumnsMap = new HashMap<String, ResourceColumn>();
		List<ResourceColumn> entityColumns = resource.resourceColumns();

		for (ResourceColumn entityColumn : entityColumns) {
			entityColumnsMap.put(entityColumn.getName().toLowerCase(), entityColumn);
		}

		/**
		 * Table Columns Info
		 */
		Map<String, Column> tableColumnsMap = new HashMap<String, Column>();
		Table table = this.queryManager.getTable(tableName);
		List<Column> tableColumns = table.getColumnList();

		for (Column tableColumn : tableColumns) {
			tableColumnsMap.put(tableColumn.getName(), tableColumn);
		}

		List<ResourceColumn> addColumnList = new ArrayList<ResourceColumn>();
		List<String> deleteColumnList = new ArrayList<String>();

		// Add Column
		{
			Set<String> keys = entityColumnsMap.keySet();
			for (String key : keys) {
				Object value = tableColumnsMap.get(key);
				if (value == null)
					addColumnList.add(entityColumnsMap.get(key));
			}
		}

		StringBuilder addSQL = new StringBuilder();
		for (ResourceColumn column : addColumnList) {
			addSQL.append("ALTER TABLE ").append(tableName);
			addSQL.append(" ADD COLUMN ");
			addSQL.append(column.getName()).append(" ");
			addSQL.append(this.toDatabaseType(column)).append(";").append("\n");
		}

		// Delete Column
		{
			Set<String> keys = tableColumnsMap.keySet();
			for (String key : keys) {
				Object value = entityColumnsMap.get(key);
				if (value == null)
					deleteColumnList.add(tableColumnsMap.get(key).getName());
			}
		}

		StringBuilder deleteSQL = new StringBuilder();
		for (String columnName : deleteColumnList) {
			deleteSQL.append("ALTER TABLE ").append(tableName);
			deleteSQL.append(" DROP COLUMN ");
			deleteSQL.append(columnName).append(";").append("\n");
		}

		if (deleteSQL.length() > 0) {
			this.ddl.executeDDL(tableName, deleteSQL.toString(), null);
		}

		if (addSQL.length() > 0) {
			this.ddl.executeDDL(tableName, addSQL.toString(), null);
		}

		return true;
	}

	@PostMapping("/code/controller/{id}")
	@ApiDesc(description = "Generate Service Controller by Resource Id.")
	public BasicOutput generateController(@PathVariable("id") String id) throws Exception {
		// Validation Check
		Resource resource = this.validationCheck(id);
		String tableName = resource.getTableName();
		AssertUtil.assertNotEmpty("terms.label.table_name", tableName);
		String idDataType = SysValueUtil.isEqual(resource.getIdType(), GenerationRule.AUTO_INCREMENT) ? "Long" : "String";

		// Detail
		Resource detailParam = new Resource();
		detailParam.setMasterId(resource.getId());
		detailParam.setDomainId(Domain.currentDomain().getId());
		List<Resource> detailResources = queryManager.selectList(Resource.class, detailParam);

		// Parse URL(:id -> {id}, Add prefix : /)
		// Master
		this.setUrl(resource, false);
		// Detail
		for (Resource detailResource : detailResources) {
			String refField = detailResource.getRefField();
			this.setUrl(detailResource, true);
			detailResource.setRefField(SysValueUtil.toCamelCase(refField, '_'));
			detailResource.setDescription(SysValueUtil.toCamelCase(refField, '_', true));
		}

		// Entity Name
		String entityName = resource.getName();
		String module = resource.getBundle();
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("tableName", tableName);
		map.put("module", SysValueUtil.getBasePath(module));
		map.put("type", idDataType);
		map.put("entity", entityName);
		map.put("detailServices", detailResources);
		map.put("searchUrl", resource.getSearchUrl());
		map.put("multiSaveUrl", resource.getMultiSaveUrl());

		if (detailResources.size() > 0) {
			map.put("isDetailResources", true);
		}

		BasicOutput result = new BasicOutput();
		try {
			String content = this.processTemplate("templates.code", "controller.vm", map);

			// File 경로 추출
			StringJoiner contextPath = new StringJoiner(File.separator);
			List<String> modulePath = getModulePath(module);
			for (String path : modulePath) {
				contextPath.add(path);
			}

			contextPath.add("src").add("main").add("java");

			String detailPath = SysValueUtil.getBasePath(module);
			String[] values = StringUtils.tokenizeToStringArray(detailPath, ".");
			for (String value : values) {
				contextPath.add(value);
			}

			contextPath.add("rest");
			String dirPath = getDirPath(module, contextPath.toString(), false);
			// File 생성
			FileUtil.createFile(dirPath, entityName + "Controller.java", content);
			// Result
			result.setMsg(content);
		} catch (Exception e) {
			throw new ElidomServiceException(SysMessageConstants.GENERATE_FAIL, "Failed to generate service!", e);
		}

		return result;
	}

	@DeleteMapping("/code/controller/{id}")
	@ApiDesc(description = "Delete Service Controller.")
	public boolean deleteController(@PathVariable("id") String id) throws Exception {
		// Validation Check
		Resource resource = this.validationCheck(id);

		String entityName = resource.getName();
		AssertUtil.assertNotEmpty("terms.label.entity", entityName);

		// 삭제 할 파일의 Module Path 가져오기
		StringJoiner path = new StringJoiner(File.separator);
		List<String> modulePath = getModulePath(resource.getBundle());
		for (String detail : modulePath) {
			path.add(detail);
		}

		path.add("src").add("main").add("java");

		String detailPath = SysValueUtil.getBasePath(resource.getBundle());
		String[] values = StringUtils.tokenizeToStringArray(detailPath, ".");
		for (String str : values) {
			path.add(str);
		}

		path.add("rest");

		String fullPath = new StringBuilder().append(path).append(File.separator).append(entityName).append("Controller.java").toString();

		File file = new File(fullPath);
		if (file.isFile()) {
			file.delete();
		}

		return true;
	}

	/**
	 * 문자의 첫 글자를 대문자 또는 소문자로 변경.
	 * 
	 * @param value
	 * @param isUpperCase
	 * @return
	 */
	private String parseHeadCase(String value, boolean isUpperCase) {
		char charArr[] = value.toCharArray();
		if (isUpperCase) {
			charArr[0] = String.valueOf(charArr[0]).toUpperCase().toCharArray()[0];
		} else {
			charArr[0] = String.valueOf(charArr[0]).toLowerCase().toCharArray()[0];
		}
		return String.valueOf(charArr);
	}

	/**
	 * (시작 구문 Tab, 종료 구문 Enter 추가)
	 * 
	 * @param sb
	 * @param datas
	 * @return
	 */
	private String appendWithTabAndEnter(StringBuilder sb, String... datas) {
		sb.append(TAB);

		for (String data : datas) {
			sb.append(data);
		}

		sb.append(ENTER);
		return sb.toString();
	}

	/**
	 * 컬럼 정보에 대한 Annotation 정보를 추가
	 * 
	 * @param columnName
	 * @param nullable
	 * @param length
	 * @return
	 */
	private String appendColumnAnnotation(String columnName, boolean nullable, int length) {
		StringJoiner joiner = new StringJoiner(", ");

		if (!SysValueUtil.isEmpty(columnName)) {
			joiner.add(new StringBuilder().append("name = \"").append(columnName).append("\""));
		}

		if (!nullable) {
			joiner.add(new StringBuilder().append("nullable = ").append(nullable));
		}

		if (length > 0 && length != 255) {
			joiner.add(new StringBuilder().append("length = ").append(length));
		}

		StringBuilder builder = new StringBuilder();
		builder.append("@Column (").append(joiner.toString()).append(")");
		return builder.toString();
	}

	/**
	 * 컬럼 정보에 대한 Annotation 정보를 추가
	 * 
	 * @param columnName
	 * @param columnType
	 * @param nullable
	 * @param length
	 * @return
	 */
	private String appendColumnAnnotation(String columnName, String columnType, boolean nullable, int length) {
		StringJoiner joiner = new StringJoiner(", ");

		if (!SysValueUtil.isEmpty(columnName)) {
			joiner.add(new StringBuilder().append("name = \"").append(columnName).append("\""));
		}

		if (!nullable) {
			joiner.add(new StringBuilder().append("nullable = ").append(nullable));
		}

		if (length > 0 && length != 255) {
			joiner.add(new StringBuilder().append("length = ").append(length));
		}

		if (SysValueUtil.isEqualIgnoreCase(columnType, "datetime")) {
			joiner.add(new StringBuilder().append("type = ").append("xyz.elidom.dbist.annotation.ColumnType.DATETIME"));
		}

		StringBuilder builder = new StringBuilder();
		builder.append("@Column (").append(joiner.toString()).append(")");
		return builder.toString();
	}

	/**
	 * Parent 객체 정보
	 * 
	 * @param isDetail
	 * @return
	 */
	private Map<String, Object> parentsInfo(List<ResourceColumn> columns) {
		List<String> columnNameList = new ArrayList<String>();
		for (ResourceColumn column : columns)
			columnNameList.add(SysValueUtil.toCamelCase(column.getName(), '_'));

		String basePackage = "xyz.elidom.core.entity.basic";
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*\\.*" + "Stamp")));

		int parentFieldCnt = 0;
		Map<String, Object> map = new HashMap<String, Object>();

		// scan from Stamp package
		for (BeanDefinition bd : scanner.findCandidateComponents(basePackage)) {
			try {
				boolean isSuspend = false;
				List<String> parentsFieldList = new ArrayList<String>();

				Class<?> stampClass = Class.forName(bd.getBeanClassName());
				Field[] fields = stampClass.getDeclaredFields();

				for (Field field : fields) {
					Object ann = field.getAnnotation(xyz.elidom.dbist.annotation.Relation.class);

					if (ann != null) {
						continue;
					}

					String fieldName = field.getName();
					if (SysValueUtil.isEqual(fieldName, "serialVersionUID")) {
						continue;
					}

					if (!columnNameList.contains(fieldName)) {
						isSuspend = true;
						break;
					}

					parentsFieldList.add(fieldName);
				}

				if (!isSuspend && parentsFieldList.size() > parentFieldCnt) {
					parentFieldCnt = parentsFieldList.size();
					map.put("parentsName", stampClass.getName() + "Hook");
					map.put("fields", parentsFieldList);
				}
			} catch (Exception e) {
				continue;
			}
		}

		if (map.isEmpty()) {
			List<String> parentsFieldList = new ArrayList<String>();
			for (Field field : ElidomStamp.class.getDeclaredFields()) {
				String fieldName = field.getName();

				if (SysValueUtil.isEqual(fieldName, "serialVersionUID")) {
					continue;
				}

				parentsFieldList.add(field.getName());
			}

			map.put("parentsName", ElidomStampHook.class.getName());
			map.put("fields", parentsFieldList);
		}

		return map;
	}

	/**
	 * Directory 경로 가져오기 실행.
	 * 
	 * @param module
	 * @param packageName
	 * @return
	 */
	private String getDirPath(String module, String contextPath, boolean changeSeparator) {
		StringBuilder path = new StringBuilder();
		path.append(contextPath);
		if (!contextPath.endsWith(File.separator)) {
			path.append(File.separator);
		}

		return changeSeparator ? path.toString().replaceAll("/", "\\\\") : path.toString();
	}

	/**
	 * Resources Column Type을 JAVA Type으로 변경.
	 * 
	 * @param dbType
	 * @param dataType
	 * @return
	 */
	private String parseResourceType(String dataType) {
		if (dataType.startsWith("integer"))
			return "Integer";
		else if (dataType.startsWith("string") || dataType.equals("text"))
			return "String";
		else if (dataType.startsWith("float"))
			return "Float";
		else if (dataType.startsWith("double"))
			return "Long";
		else if (dataType.startsWith("boolean"))
			return "Boolean";
		else if (dataType.startsWith("datetime") || dataType.startsWith("date") || dataType.startsWith("timestamp"))
			return "Date";
		else
			throw new ElidomServiceException(SysMessageConstants.UNKNOWN_TYPE, "Unknown Type : {0}", MessageUtil.params(dataType));
	}

	/**
	 * Resource Column의 Type을 Postgres DB Type으로 변경.
	 * 
	 * @param column
	 * @return
	 */
	private String toDatabaseType(ResourceColumn column) {
		Map<String, Object> map = SysValueUtil.newMap("type,length", column.getColType(), column.getColSize());
		return this.ddl.getDdlMapper().toDatabaseType(map);
	}

	/**
	 * Validation Check
	 * 
	 * @param id
	 * @return
	 * @throws Exception
	 */
	private Resource validationCheck(String id) throws Exception {
		Resource resource = queryManager.select(true, Resource.class, id);
		String notModifiableList = SettingUtil.getValue(SysConfigConstants.MODULE_NOT_MODIFIABLE_LIST, "base");
		List<String> moduleList = Arrays.asList(StringUtils.tokenizeToStringArray(notModifiableList, ","));

		if (moduleList.contains(resource.getBundle())) {
			// throw new ElidomServiceException("Module [" + resource.getBundle() + "] can't be modified!");
		}

		return resource;
	}

	/**
	 * Resourc 내의 URL 값에 대한 Validation Check.
	 * 
	 * @param resource
	 * @param isDetail
	 * @return
	 */
	private Resource setUrl(Resource resource, boolean isDetail) {
		String searchUrl = resource.getSearchUrl();
		String multiSaveUrl = resource.getMultiSaveUrl();

		if (isDetail) {
			// Set Search URL
			if (SysValueUtil.isEmpty(searchUrl)) {
				// "/{id}/table_name"
				searchUrl = new StringJoiner("/", "/", "").add("{id}").add(resource.getTableName()).toString();
			} else {
				searchUrl = this.parseUrl(resource.getSearchUrl());
			}

			// Set Multi Save URL
			if (SysValueUtil.isEmpty(multiSaveUrl)) {
				// "/{id}/table_name/update_multiple"
				StringJoiner joiner = new StringJoiner("/", "/", "");
				multiSaveUrl = joiner.add("{id}").add(resource.getTableName()).add("update_multiple").toString();
			} else {
				multiSaveUrl = this.parseUrl(resource.getMultiSaveUrl());
			}
		} else {
			if (SysValueUtil.isEmpty(searchUrl)) {
				searchUrl = new StringBuilder("/").append(resource.getTableName()).toString();
			} else {
				searchUrl = this.parseUrl(resource.getSearchUrl());
			}

			if (SysValueUtil.isEmpty(multiSaveUrl)) {
				multiSaveUrl = "/update_multiple";
			} else {
				multiSaveUrl = this.parseUrl(resource.getMultiSaveUrl());
			}
		}

		resource.setSearchUrl(searchUrl);
		resource.setMultiSaveUrl(multiSaveUrl);
		return resource;
	}

	/**
	 * URL Param 형식 변경
	 * 
	 * @param url
	 * @return
	 */
	private String parseUrl(String url) {
		url = url.substring(url.indexOf("/") + 1, url.length());
		url = url.replaceAll(":id", "{id}");

		if (!url.startsWith("/")) {
			url = "/" + url;
		}

		return url;
	}

	private List<String> getModulePath(String module) {
		String bundleSql = "select id from common_codes where name = 'BUNDLE'";
		String bundleId = queryManager.selectBySql(bundleSql, null, String.class);

		String moduleSql = "select data_1 from common_code_details where parent_id = :bundleId and name = :module";
		Map<String, Object> param = ValueUtil.newMap("bundleId,module", bundleId, module);
		String modulePath = queryManager.selectBySql(moduleSql, param, String.class);

		if (ValueUtil.isEmpty(modulePath)) {
			return new ArrayList<>();
		}

		return Arrays.asList(modulePath.split("/"));
	}
}