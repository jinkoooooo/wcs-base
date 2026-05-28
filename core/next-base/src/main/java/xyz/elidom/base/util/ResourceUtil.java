/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.base.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import jxl.common.Logger;
import net.sf.common.util.ReflectionUtils;
import xyz.elidom.base.BaseConstants;
import xyz.elidom.base.entity.Menu;
import xyz.elidom.base.entity.MenuColumn;
import xyz.elidom.base.entity.Resource;
import xyz.elidom.base.entity.ResourceColumn;
import xyz.elidom.base.rest.MenuController;
import xyz.elidom.base.rest.ResourceController;
import xyz.elidom.core.CoreConstants;
import xyz.elidom.core.entity.CodeDetail;
import xyz.elidom.core.util.BundleUtil;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Relation;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.dbist.ddl.Ddl;
import xyz.elidom.dbist.ddl.mapper.DdlMapper;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.util.DdlUtil;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.config.ModuleConfigSet;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.system.config.module.IModuleProperties;
import xyz.elidom.sys.util.MessageUtil;
import xyz.elidom.sys.util.SysValueUtil;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ClassUtil;
import xyz.elidom.util.FormatUtil;

/**
 * Entity 관련 유틸리티 
 * 
 * @author shortstop
 */
public class ResourceUtil {
	/**
	 * Logger
	 */
	private static Logger logger = Logger.getLogger(ResourceUtil.class);
	
	/**
	 * Entity Object로 부터 Entity Class Full Name을 리턴한다.
	 * 
	 * @param resource
	 * @return
	 */
	public static String getEntityClassName(Resource resource) {
		return BundleUtil.getEntityClassName(resource.getBundle(), resource.getName());
	}
	
	/**
	 * entityClass의 bundle 명을 리턴한다. 
	 * 
	 * @param entityClass
	 * @return
	 */
	public static String getBundleOfEntity(Class<?> entityClass) {
		ModuleConfigSet configSet = BeanUtil.get(ModuleConfigSet.class);
		String className = entityClass.getName();
		String entityPackage = className.substring(0, className.lastIndexOf(OrmConstants.DOT));
		
		Map<String, IModuleProperties> moduleMap = configSet.getAll();
		Iterator<String> keyIter = configSet.getAll().keySet().iterator();
		
		while(keyIter.hasNext()) {
			IModuleProperties module = moduleMap.get(keyIter.next());
			if(SysValueUtil.isEqual(module.getScanEntityPackage(), entityPackage)) {
				return module.getName();
			}
		}
		
		return null;
	}
	
	/**
	 * entityClass의 bundle 명을 리턴한다. 
	 * 
	 * @param bundle
	 * @return
	 */
	public static List<Class<?>> getEntityClassesByBundle(String bundle) {
		ModuleConfigSet configSet = BeanUtil.get(ModuleConfigSet.class);
		IModuleProperties module = configSet.getConfig(bundle);
		List<Class<?>> entityClassList = new ArrayList<Class<?>>();
		
		if(module != null) {
			ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
			scanner.addIncludeFilter(new AnnotationTypeFilter(Table.class));			
			
			for (BeanDefinition bd : scanner.findCandidateComponents(module.getScanEntityPackage())) {
				String beanName = bd.getBeanClassName();
				Class<?> entityClass = ClassUtil.forName(beanName);
				entityClassList.add(entityClass);
			}
		}
		
		return entityClassList;
	}
	
	/**
	 * entity 테이블에서 selectFields로 모든 레코드를 조회해서 CodeDetail 형식의 리스트로 리턴한다.
	 * resource-code 컴포넌트 지원을 위한 기능
	 * 
	 * @param entity
	 * @param domainBased
	 * @return
	 */
	public static List<CodeDetail> searchRecordsAsCode(Resource entity, boolean domainBased) {
		if(SysValueUtil.isEmpty(entity.getTitleField())) {
			throw new ElidomRuntimeException("Title Field of Entity [" + entity.getName() + "] must not be empty!");
		}
		
		if(SysValueUtil.isEmpty(entity.getDescField())) {
			throw new ElidomRuntimeException("Description Field of Entity [" + entity.getName() + "] must not be empty!");
		}
		
		StringBuffer sql = new StringBuffer("select ");
		sql.append(entity.getTitleField()).append(" as name,")
		   .append(entity.getDescField()).append(" as description")
		   .append(" from ").append(entity.getTableName()).append(domainBased ? " where domain_id = :domainId" : "");
		
		return BeanUtil.get(IQueryManager.class).selectListBySql(sql.toString(), SysValueUtil.newMap("domainId", entity.getDomainId()), CodeDetail.class, 0, 0);
	}
	
	/**
	 * ResourceColumn의 기본값을 설정한다.
	 * 
	 * @param resource
	 * @param column
	 */
	public static void setDefaultColumnInfo(Resource resource, ResourceColumn column) {
		if(SysValueUtil.isEqual(column.getCudFlag_(), CoreConstants.CUD_FLAG_DELETE)) return;

		// 용어 정보 설정
		if(SysValueUtil.isEmpty(column.getTerm())) {
			column.setTerm(SysConstants.LABEL_KEY + SysConstants.DOT + column.getName());
		}
		
		// 설명 정보 설정
		if(SysValueUtil.isEmpty(column.getDescription()) && SysValueUtil.isNotEmpty(column.getTerm())) {
			String term = MessageUtil.getTerm(column.getTerm());
			if(!term.startsWith(SysConstants.LABEL_KEY)) {
				column.setDescription(term);
			}
		}
		
		String columnName = SysValueUtil.toCamelCase(column.getName(), SysConstants.CHAR_UNDER_SCORE);
		if(!CoreConstants.ENTITY_FIELD_RESERVED_LIST.contains(columnName)) return;
		
		// 1. ID
		if(SysValueUtil.isEqual(CoreConstants.ENTITY_FIELD_ID, columnName)) {
			column.setTerm(OrmConstants.LABEL_KEY + OrmConstants.DOT + column.getName());
			column.setGridEditor("hidden");
			
			if(SysValueUtil.isEqual(resource.getIdType(), GenerationRule.UUID)) {
				column.setColType(OrmConstants.DATA_TYPE_STRING);
				column.setColSize(OrmConstants.FIELD_SIZE_UUID);

			} else if(SysValueUtil.isEqual(resource.getIdType(), GenerationRule.AUTO_INCREMENT)) {
				column.setColType(OrmConstants.DATA_TYPE_INTEGER);
				column.setColSize(0);
				
			} else if(SysValueUtil.isEqual(resource.getIdType(), GenerationRule.MEANINGFUL)) {
				column.setColType(OrmConstants.DATA_TYPE_STRING);
				column.setColSize(OrmConstants.FIELD_SIZE_MEANINGFUL_ID);
			}
		
		// 2. Domain ID
		} else if(SysValueUtil.isEqual(CoreConstants.ENTITY_FIELD_DOMAIN_ID, columnName)) {
			column.setTerm(OrmConstants.LABEL_KEY + OrmConstants.DOT + columnName.replace("Id", SysConstants.EMPTY_STRING));
			column.setColType(OrmConstants.DATA_TYPE_INTEGER);
			
		// 3. Creator / Updater
		} else if(SysValueUtil.isEqual(CoreConstants.ENTITY_FIELD_CREATOR_ID, columnName) || SysValueUtil.isEqual(CoreConstants.ENTITY_FIELD_UPDATER_ID, columnName)) {
			column.setColType(OrmConstants.DATA_TYPE_STRING);
			column.setRefType(BaseConstants.REF_TYPE_ENTITY);
			column.setRefName("User");
			column.setColSize(OrmConstants.FIELD_SIZE_USER_ID);
			column.setTerm(OrmConstants.LABEL_KEY + OrmConstants.DOT + columnName.replace("Id", SysConstants.EMPTY_STRING));
			column.setGridWidth(90);
			column.setNullable(true);
			
		// 4. Created At / Updated At
		} else if(SysValueUtil.isEqual(CoreConstants.ENTITY_FIELD_CREATED_AT, columnName) || SysValueUtil.isEqual(CoreConstants.ENTITY_FIELD_UPDATED_AT, columnName)) {
			column.setColType(OrmConstants.DATA_TYPE_DATETIME);
			column.setTerm(OrmConstants.LABEL_KEY + OrmConstants.DOT + column.getName());
			column.setGridEditor("readonly");
			column.setGridWidth(140);
			column.setNullable(true);
		}
	}
	
	/**
	 * entityClass를 Base로 해서 EntityColumn 정보를 동기화한다.
	 *  
	 * @param entityClass
	 * @param resource
	 * @return 변경된 EntityColumn의 갯수 
	 */
	public static int syncEntityColumnsWithEntity(Class<?> entityClass, Resource resource) {
		List<ResourceColumn> entityColumns = resource.resourceColumns();
		List<ResourceColumn> changeEntityColumns = new ArrayList<ResourceColumn>();
		
		// 1. 필요한 메타 정보 추출  
		Ddl ddl = BeanUtil.get(Ddl.class);
		DdlMapper ddlMapper = ddl.getDdlMapper();
		Annotation tableAnn = AnnotationUtils.findAnnotation(entityClass, xyz.elidom.dbist.annotation.Table.class);
		String uniqueFields = (String)AnnotationUtils.getAnnotationAttributes(tableAnn).get(OrmConstants.TABLE_ANN_UNIQUE_FIELDS);
		List<String> uniqueFieldList = SysValueUtil.isEmpty(uniqueFields) ? new ArrayList<String>(0) : Arrays.asList(uniqueFields.split(OrmConstants.COMMA));		
		List<Field> relationFields = new ArrayList<Field>();
		int maxRank = 0;
		
		// 2. maxRank 추출 
		for(ResourceColumn eCol : entityColumns) {
			if(eCol.getRank() > maxRank) {
				maxRank = eCol.getRank();
			}
		}
		
		// 3. 일반 엔티티 컬럼 처리
		List<Field> fieldList = ReflectionUtils.getFieldList(entityClass, false);
		List<String> skipFields = OrmConstants.ENTITY_FIELD_DEFAULT_IGNORED_LIST;
		
		for(Field field : fieldList) {
			if(skipFields.contains(field.getName())) {
				continue;
			}
			
			xyz.elidom.dbist.annotation.Column colAnn = field.getAnnotation(xyz.elidom.dbist.annotation.Column.class);
			
			if(field.getAnnotation(xyz.elidom.dbist.annotation.Relation.class) != null) {
				relationFields.add(field);
				
			} else if(colAnn != null) {
				ResourceColumn currentColumn = ResourceUtil.findOrBuildEntityColumn(resource.getId(), field, colAnn, entityColumns, maxRank);
				if(SysValueUtil.isEqual(currentColumn.getCudFlag_(), SysConstants.CUD_FLAG_CREATE)) {
					maxRank = currentColumn.getRank();
				}
				
				ResourceUtil.buildEntityColumn(ddlMapper, field, colAnn, currentColumn, uniqueFieldList);
				changeEntityColumns.add(currentColumn);
			}
		}
		
		// 4. 관계 (Relation) 컬럼 처리  
		for(Field relationField : relationFields) {
			ResourceUtil.buildRelationEntityColumn(resource.getName(), entityClass, relationField, changeEntityColumns);
		}
		
		// 5. 변경된 데이터 생성 혹은 업데이트
		int changeCount = ResourceUtil.saveChanges(changeEntityColumns);
		
		if(changeCount > 0) {
			logger.info("Total [" + changeCount + "] entity columns changed at Entity [" + resource.getName() + "] ");
		}
		
		return changeCount;
	}
	
	/**
	 * entityClass를 Base로 해서 MenuColumn 정보를 동기화한다.
	 *  
	 * @param menuId
	 * @return 변경된 MenuColumn의 갯수 
	 */
	public static int syncMenuColumnsWithEntity(String menuId) {
		MenuController menuCtrl = BeanUtil.get(MenuController.class);
		Menu menu = menuCtrl.findOne(menuId, null);
		
		if(SysValueUtil.isNotEqual(BaseConstants.RESOURCE_TYPE_ENTITY, menu.getResourceType()) || SysValueUtil.isEmpty(menu.getResourceName())) {
			return 0;
		}
		
		List<MenuColumn> menuColumns = menuCtrl.findMenuColumns(menuId);
		ResourceController resourceCtrl = BeanUtil.get(ResourceController.class);
		Resource entity = resourceCtrl.findOne(SysConstants.SHOW_BY_NAME_METHOD, menu.getResourceName());
		List<ResourceColumn> entityColumns = entity.resourceColumns();
		return syncMenuColumnsWithEntity(menuId, entityColumns, menuColumns);
	}
	
	/**
	 * menuColumns를 기본으로 menuColumns와 menuColumns 정보를 동기화한다.
	 * 
	 * @param menuId
	 * @param entityColumns
	 * @param menuColumns
	 * @return 변경된 MenuColumn의 갯수 
	 */
	public static int syncMenuColumnsWithEntity(String menuId, List<ResourceColumn> entityColumns, List<MenuColumn> menuColumns) {
		List<MenuColumn> changeMenuColumns = new ArrayList<MenuColumn>();
		
		// 1. 엔티티 컬럼을 순회하면서  
		List<String> skipFields = OrmConstants.TABLE_FIELD_DEFAULT_IGNORED_LIST;
		for(ResourceColumn entityColumn : entityColumns) {
			if(!skipFields.contains(entityColumn.getName())) {
				MenuColumn menuColumn = ResourceUtil.findOrBuildMenuColumn(menuId, entityColumn, menuColumns);
				ResourceUtil.buildMenuColumn(entityColumn, menuColumn);
				if(SysValueUtil.isNotEmpty(menuColumn.getCudFlag_())) {
					changeMenuColumns.add(menuColumn);
				}
			}
		}
		
		// 2. 변경된 데이터 생성 혹은 업데이트 
		int changeCount = ResourceUtil.saveChanges(changeMenuColumns);
		
		if(changeCount > 0) {
			logger.info("Total [" + changeCount + "] menu columns changed at Menu [" + menuId + "] ");
		}
		
		return changeCount;
	}
	
	/**
	 * 이름을 이용하여, Custom Resource 객체 가져오기 실
	 * 
	 * @param name
	 * @return
	 */
	public static Resource findExtResource(String name) {
		return BeanUtil.get(ResourceController.class).findExtResource(SysValueUtil.isEqual(name, "Resource") ? "Entity" : name);
	}

	/**
	 * Entity Name을 이용하여 Entity의 Column name 정보를 추출
	 * 
	 * @param name
	 * @return
	 */
	public static List<String> resourceColumnNames(String name) {
		List<String> columnList = new ArrayList<String>();
		ResourceController resourceController = BeanUtil.get(ResourceController.class);

		Resource resource = resourceController.findOne(SysConstants.SHOW_BY_NAME_METHOD, name);
		resource = resourceController.resourceColumns(resource.getId());

		List<ResourceColumn> columns = resource.getItems();
		if (SysValueUtil.isNotEmpty(columns))
			columns.forEach(column -> {
				String columnName = column.getName();
				columnList.add(columnName);

				if (columnName.endsWith("_id") && SysValueUtil.isEqual(column.getRefType(), BaseConstants.REF_TYPE_ENTITY)) {
					columnList.add(columnName.replace("_id", ""));
				}
			});

		return columnList;
	}
	
	/**
	 * 변경된 데이터를 저장 
	 * 
	 * @param list
	 * @return
	 */
	private static int saveChanges(List<?> list) {
		IQueryManager queryManager = BeanUtil.get(IQueryManager.class);
		// 변경된 데이터 생성 혹은 업데이트 
		int changeCount = 0;
		
		for(Object object : list) {
			Object cudFlag = ClassUtil.getFieldValue(object, "cudFlag_");
			
			if(SysValueUtil.isEqual(SysConstants.CUD_FLAG_CREATE, cudFlag)) {
				queryManager.insert(object);
				changeCount++;
			} else if(SysValueUtil.isEqual(SysConstants.CUD_FLAG_UPDATE, cudFlag)) {
				queryManager.update(object);
				changeCount++;
			}
		}
		
		return changeCount;		
	}
	
	/**
	 * menuColumns 리스트 중에 entityColumn 정보와 일치하는 컬럼이 있다면 찾고 없다면 새로 생성하여 리턴 
	 * 
	 * @param menuId
	 * @param entityColumn
	 * @param menuColumns
	 * @return
	 */
	private static MenuColumn findOrBuildMenuColumn(String menuId, ResourceColumn entityColumn, List<MenuColumn> menuColumns) {
		String colName = entityColumn.getName();
		MenuColumn currentColumn = null;
		
		for(MenuColumn menuColumn : menuColumns) {
			if(SysValueUtil.isEqual(colName, menuColumn.getName())) {
				currentColumn = menuColumn;
				break;
			}
		}
		
		if(currentColumn == null) {
			currentColumn = SysValueUtil.populate(entityColumn, new MenuColumn());
			currentColumn.setId(null);
			currentColumn.setMenuId(menuId);
			currentColumn.setCudFlag_(SysConstants.CUD_FLAG_CREATE);
		}
		
		return currentColumn;
	}
	
	/**
	 * entityColumns 리스트 중에 tableColumn 정보와 일치하는 컬럼이 있다면 찾고 없다면 새로 생성하여 리턴 
	 * 
	 * @param entityId
	 * @param tableColumn
	 * @param entityColumns
	 * @param maxRank
	 * @return
	 */
	private static ResourceColumn findOrBuildEntityColumn(String entityId, Field field, xyz.elidom.dbist.annotation.Column fieldAnn, List<ResourceColumn> entityColumns, int maxRank) {
		String colName = fieldAnn.name();
		ResourceColumn currentColumn = null;
		
		for(ResourceColumn entityColumn : entityColumns) {
			if(SysValueUtil.isEqual(colName, entityColumn.getName())) {
				currentColumn = entityColumn;
				break;
			}
		}
		
		if(currentColumn == null) {
			currentColumn = new ResourceColumn(entityId, colName);
			currentColumn.setTerm(SysConstants.TERM_LABELS + colName);
			currentColumn.setCudFlag_(SysConstants.CUD_FLAG_CREATE);
			maxRank = maxRank + 10;
			currentColumn.setRank(maxRank);
		}
		
		return currentColumn;
	}	
	
	/**
	 * EntityColumn 정보 설정 
	 * 
	 * @param ddlMapper
	 * @param field
	 * @param fieldAnn
	 * @param entityColumn
	 * @param uniqueFieldList
	 */
	private static void buildEntityColumn(DdlMapper ddlMapper, Field field, xyz.elidom.dbist.annotation.Column fieldAnn, ResourceColumn entityColumn, List<String> uniqueFieldList) {
		String cudFlag = entityColumn.getCudFlag_();
		String newCudFlag = null;
		
		// 1. set nullable
		Boolean nullable = fieldAnn.nullable();
		if(SysValueUtil.isNotEqual(nullable, entityColumn.getNullable())) {
			entityColumn.setNullable(nullable);
			
			if(SysValueUtil.isEmpty(cudFlag) && SysValueUtil.isEmpty(newCudFlag)) {
				newCudFlag = SysConstants.CUD_FLAG_UPDATE;
			}
		}
		
		// 2. set column type
		String entityColType = DdlUtil.javaTypeToEntityColType(field, fieldAnn);		
		if(SysValueUtil.isNotEqual(entityColType, entityColumn.getColType())) {
			entityColumn.setColType(entityColType);
			
			if(SysValueUtil.isEmpty(cudFlag) && SysValueUtil.isEmpty(newCudFlag)) {
				newCudFlag = SysConstants.CUD_FLAG_UPDATE;
			}
		}
		
		// 3. set column size
		int prevColSize = entityColumn.getColSize() == null ? 0 : entityColumn.getColSize();
		int newColSize = fieldAnn.length(); //ValueUtil.isEqualIgnoreCase(entityColumn.getColType(), OrmConstants.DATA_TYPE_STRING) ? fieldAnn.length() : 0;
				
		if(SysValueUtil.isNotEqual(prevColSize, newColSize)) {
			entityColumn.setColSize(newColSize);
			
			if(SysValueUtil.isEmpty(cudFlag) && SysValueUtil.isEmpty(newCudFlag)) {
				newCudFlag = SysConstants.CUD_FLAG_UPDATE;
			}
		}
		
		// 4. set unique rank
		int prevUniqueRank = entityColumn.getUniqRank() == null ? 0 : entityColumn.getUniqRank();
		
		for(int i = 0 ; i < uniqueFieldList.size() ; i++) {
			if(SysValueUtil.isEqual(field.getName(), FormatUtil.toUnderScore(uniqueFieldList.get(i)))) {
				int newUniqueRank = (i + 1) * 10;
				
				if(SysValueUtil.isNotEqual(prevUniqueRank, newUniqueRank)) {
					entityColumn.setUniqRank(newUniqueRank);
					
					if(SysValueUtil.isEmpty(cudFlag) && SysValueUtil.isEmpty(newCudFlag)) {
						newCudFlag = SysConstants.CUD_FLAG_UPDATE;
					}
				}
			}
		}
		
		// 5. set cud_flag
		if(SysValueUtil.isNotEmpty(newCudFlag)) {
			entityColumn.setCudFlag_(newCudFlag);
		}
	}
	
	/**
	 * menuColumn을 entityColumn과 비교하여 다른 점이 있다면 entityColumn으로 수정하여 리턴  
	 * 
	 * @param entityColumn
	 * @param menuColumn
	 */
	private static void buildMenuColumn(ResourceColumn entityColumn, MenuColumn menuColumn) {
		String cudFlag = menuColumn.getCudFlag_();
		String newCudFlag = null;
		
		// 1. colType 비교 
		if(SysValueUtil.isNotEqual(entityColumn.getColType(), menuColumn.getColType())) {
			menuColumn.setColType(entityColumn.getColType());
			
			if(SysValueUtil.isEmpty(cudFlag) && SysValueUtil.isEmpty(newCudFlag)) {
				newCudFlag = SysConstants.CUD_FLAG_UPDATE;
			}
			
		// 2. colSize 비교 
		} else if(SysValueUtil.isNotEqual(entityColumn.getColSize(), menuColumn.getColSize())) {
			menuColumn.setColSize(entityColumn.getColSize());
			
			if(SysValueUtil.isEmpty(cudFlag) && SysValueUtil.isEmpty(newCudFlag)) {
				newCudFlag = SysConstants.CUD_FLAG_UPDATE;
			}
			
		// 3. nullable 비교 
		} else if(SysValueUtil.isNotEqual(entityColumn.getNullable(), menuColumn.getNullable())) {
			menuColumn.setNullable(entityColumn.getNullable());
			
			if(SysValueUtil.isEmpty(cudFlag) && SysValueUtil.isEmpty(newCudFlag)) {
				newCudFlag = SysConstants.CUD_FLAG_UPDATE;
			}
		}
		
		if(SysValueUtil.isNotEmpty(newCudFlag)) {
			menuColumn.setCudFlag_(newCudFlag);
		}
	}
	
	/**
	 * 참조 entity column 정보 설정 
	 * 
	 * @param resourceName
	 * @param entityClass
	 * @param field
	 * @param entityColumns
	 */
	private static void buildRelationEntityColumn(String resourceName, Class<?> entityClass, Field field, List<ResourceColumn> entityColumns) {
		Relation relation = field.getAnnotation(xyz.elidom.dbist.annotation.Relation.class);
		String relFieldName = relation.field()[0];
		ResourceColumn relatedColumn = null;
		
		// 1. 참조 컬럼 추출 
		for(ResourceColumn eCol : entityColumns) {
			if(SysValueUtil.isEqual(FormatUtil.toCamelCase(relFieldName), FormatUtil.toCamelCase(eCol.getName()))) {
				relatedColumn = eCol;
				break;
			}
		}
		
		if(relatedColumn != null) {
			String cudFlag = relatedColumn.getCudFlag_();
			String newCudFlag = null;
			relatedColumn.setRefType(BaseConstants.REF_TYPE_ENTITY);
			
			// 2-1. Self 참조인 경우 
			if(relation.selfReference()) {
				if(SysValueUtil.isNotEqual(resourceName, relatedColumn.getRefName())) {
					relatedColumn.setRefName(resourceName);
					
					if(SysValueUtil.isEmpty(cudFlag) && SysValueUtil.isEmpty(newCudFlag)) {
						newCudFlag = SysConstants.CUD_FLAG_UPDATE;
					}
				}
			
			// 2-2. Self 참조가 아닌 경우 
			} else {
				Field refField = ClassUtil.getField(entityClass, relFieldName);
				if(refField != null) {
					// 뒤에 붙은 Ref 문자를 제거 예) MenuRef -> Menu : TODO 더 좋은 방법 찾아야 함 (Table 애노테이션에 mainTable 같은 필드 추가 필요)
					String refName = refField.getType().getSimpleName();
					refName = refName.substring(0, refName.length() - 3);
					
					if(SysValueUtil.isNotEqual(refName, relatedColumn.getRefName())) {
						relatedColumn.setRefName(refName);
						
						if(SysValueUtil.isEmpty(cudFlag) && SysValueUtil.isEmpty(newCudFlag)) {
							newCudFlag = SysConstants.CUD_FLAG_UPDATE;
						}						
					}
				}
			}
			
			// 2-3. set cud_flag
			if(SysValueUtil.isNotEmpty(newCudFlag)) {
				relatedColumn.setCudFlag_(newCudFlag);
			}
		}
	}
	
	/**
	 * Excel Export Menu List  
	 * 
	 * @param menuList
	 * @param menuHeaderColumns
	 * @param columnHeaderColumns
	 * @return
	 */
	public static Workbook exportMenusToExcel(List<Menu> menuList, List<MenuColumn> menuHeaderColumns, List<ResourceColumn> columnHeaderColumns) {
		Workbook workbook = newWorkbook();
		String locale = User.currentUser().getLocale();
		MenuController menuCtrl = BeanUtil.get(MenuController.class);
		
		for(Menu menu : menuList) {
			List<MenuColumn> columns = menuCtrl.findMenuColumns(menu.getId());
			menu.setTitle(MessageUtil.getLocaleTerm(locale, "terms.menu." + menu.getName()));
			Sheet sheet = workbook.createSheet(menu.getTitle());
			addMenuInfoToSheet(sheet, menuHeaderColumns, columnHeaderColumns, menu, columns, locale);
		}
		
		return workbook;
	}
	
	/**
	 * create new workbook
	 * 
	 * @return
	 */
	private static Workbook newWorkbook() {
		Workbook workbook = new HSSFWorkbook();
		CellStyle style = workbook.createCellStyle();
		style.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.index);
		return workbook;
	}
	
	/**
	 * 메뉴 정보를 Sheet에 추가 
	 * 
	 * @param sheet
	 * @param menuHeaderColumns
	 * @param columnHeaderColumns
	 * @param menu
	 * @param menuColumns
	 * @param locale
	 */
	private static void addMenuInfoToSheet(Sheet sheet, List<MenuColumn> menuHeaderColumns, List<ResourceColumn> columnHeaderColumns, Menu menu, List<MenuColumn> menuColumns, String locale) {
		int sheetRows = 1;
		
		// 1. 첫번째 Row에 메뉴 정보 타이틀 추가 
		Row menuTitleRow = sheet.createRow(sheetRows);
		Cell menuTitleCell = menuTitleRow.createCell(0);
		menuTitleCell.setCellValue("Menu Information");
		
		// 2. 메뉴 헤더를 위한 Row 추가 
		sheetRows++;
		Row menuHeaderRow = sheet.createRow(sheetRows);
		
		// 3. 메뉴 헤더 Row에 컬럼 추가 
		for(int i = 0 ; i < menuHeaderColumns.size() ; i++) {
			MenuColumn header = menuHeaderColumns.get(i);
			Cell cell = menuHeaderRow.createCell(i);
			String headerName = SysValueUtil.isEmpty(header.getTerm()) ? header.getName() : MessageUtil.getLocaleTerm(locale, header.getTerm());
			cell.setCellValue(headerName);
		}
		
		// 4. 메뉴에 대한 데이터 Row 생성 
		sheetRows++;
		Row menuDataRow = sheet.createRow(sheetRows);
		
		for(int i = 0 ; i < menuHeaderColumns.size() ; i++) {
			MenuColumn header = menuHeaderColumns.get(i);
			Cell valueCell = menuDataRow.createCell(i);
			Object value = ClassUtil.getFieldValue(menu, FormatUtil.toCamelCase(header.getName()));
			valueCell.setCellValue(value == null ? null : value.toString());
		}
		
		// 5. 메뉴 컬럼 정보 헤더 타이틀 추가 
		sheetRows = sheetRows + 2;
		Row menuColumnTitleRow = sheet.createRow(sheetRows);
		Cell menuColumnTitleCell = menuColumnTitleRow.createCell(0);
		menuColumnTitleCell.setCellValue("Menu Columns");
		
		// 6. 메뉴 컬럼 헤더를 위한 Row 추가 
		sheetRows++;
		Row menuColumnHeaderRow = sheet.createRow(sheetRows);
		
		// 7. 메뉴 컬럼 헤더 Row에 컬럼 추가 
		for(int i = 0 ; i < columnHeaderColumns.size() ; i++) {
			ResourceColumn header = columnHeaderColumns.get(i);
			Cell cell = menuColumnHeaderRow.createCell(i);
			String headerName = SysValueUtil.isEmpty(header.getTerm()) ? header.getName() : MessageUtil.getLocaleTerm(locale, header.getTerm());
			cell.setCellValue(headerName);
		}
		
		// 8. 메뉴 컬럼 데이터를 돌면서 하나의 Row를 생성
		for(MenuColumn column : menuColumns) {
			sheetRows++;
			Row menuColumnDataRow = sheet.createRow(sheetRows);
			
			for(int i = 0 ; i < columnHeaderColumns.size() ; i++) {
				ResourceColumn header = columnHeaderColumns.get(i);
				Cell valueCell = menuColumnDataRow.createCell(i);
				Object value = ClassUtil.getFieldValue(column, FormatUtil.toCamelCase(header.getName()));
				valueCell.setCellValue(value == null ? null : value.toString());
			}
		}
	}
	
	/**
	 * Excel Export Entity List  
	 * 
	 * @param entityList
	 * @param entityHeaderColumns
	 * @param columnHeaderColumns
	 * @return
	 */
	public static Workbook exportEntitiesToExcel(List<Resource> entityList, List<ResourceColumn> entityHeaderColumns, List<ResourceColumn> columnHeaderColumns) {
		Workbook workbook = newWorkbook();
		String locale = User.currentUser().getLocale();
		
		for(Resource entity : entityList) {
			List<ResourceColumn> entityColumns = entity.resourceColumns();
			Sheet sheet = workbook.createSheet(entity.getName());
			addEntityInfoToSheet(sheet, entityHeaderColumns, columnHeaderColumns, entity, entityColumns, locale);
		}
		
		return workbook;
	}
	
	/**
	 * 엔티티 정보를 Sheet에 추가 
	 * 
	 * @param sheet
	 * @param entityHeaderColumns
	 * @param columnHeaderColumns
	 * @param entity
	 * @param entityColumns
	 * @param locale
	 */
	private static void addEntityInfoToSheet(Sheet sheet, List<ResourceColumn> entityHeaderColumns, List<ResourceColumn> columnHeaderColumns, Resource entity, List<ResourceColumn> entityColumns, String locale) {
		int sheetRows = 1;
		
		// 1. 첫번째 Row에 엔티티 정보 타이틀 추가 
		Row entityTitleRow = sheet.createRow(sheetRows);
		Cell entityTitleCell = entityTitleRow.createCell(0);
		entityTitleCell.setCellValue("Entity Information");
		
		// 2. 엔티티 헤더를 위한 Row 추가 
		sheetRows++;
		Row entityHeaderRow = sheet.createRow(sheetRows);
		
		// 3. 엔티티 헤더 Row에 컬럼 추가 
		for(int i = 0 ; i < entityHeaderColumns.size() ; i++) {
			ResourceColumn header = entityHeaderColumns.get(i);
			Cell cell = entityHeaderRow.createCell(i);
			String headerName = SysValueUtil.isEmpty(header.getTerm()) ? header.getName() : MessageUtil.getLocaleTerm(locale, header.getTerm());
			cell.setCellValue(headerName);
		}
		
		// 4. 엔티티에 대한 데이터 Row 생성 
		sheetRows++;
		Row entityDataRow = sheet.createRow(sheetRows);
		
		for(int i = 0 ; i < entityHeaderColumns.size() ; i++) {
			ResourceColumn header = entityHeaderColumns.get(i);
			Cell valueCell = entityDataRow.createCell(i);
			Object value = ClassUtil.getFieldValue(entity, FormatUtil.toCamelCase(header.getName()));
			valueCell.setCellValue(value == null ? null : value.toString());
		}
		
		// 5. 엔티티 컬럼 정보 헤더 타이틀 추가 
		sheetRows = sheetRows + 2;
		Row entityColumnTitleRow = sheet.createRow(sheetRows);
		Cell menuColumnTitleCell = entityColumnTitleRow.createCell(0);
		menuColumnTitleCell.setCellValue("Entity Columns");
		
		// 6. 엔티티 컬럼 헤더를 위한 Row 추가 
		sheetRows++;
		Row entityColumnHeaderRow = sheet.createRow(sheetRows);
		
		// 7. 엔티티 컬럼 헤더 Row에 컬럼 추가 
		for(int i = 0 ; i < columnHeaderColumns.size() ; i++) {
			ResourceColumn header = columnHeaderColumns.get(i);
			Cell cell = entityColumnHeaderRow.createCell(i);
			String headerName = SysValueUtil.isEmpty(header.getTerm()) ? header.getName() : MessageUtil.getLocaleTerm(locale, header.getTerm());
			cell.setCellValue(headerName);
		}
		
		// 8. 엔티티 컬럼 데이터를 돌면서 하나의 Row를 생성
		for(ResourceColumn column : entityColumns) {
			sheetRows++;
			Row entityColumnDataRow = sheet.createRow(sheetRows);
			
			for(int i = 0 ; i < columnHeaderColumns.size() ; i++) {
				ResourceColumn header = columnHeaderColumns.get(i);
				Cell valueCell = entityColumnDataRow.createCell(i);
				Object value = ClassUtil.getFieldValue(column, FormatUtil.toCamelCase(header.getName()));
				valueCell.setCellValue(value == null ? null : value.toString());
			}			
		}
	}
	
}
