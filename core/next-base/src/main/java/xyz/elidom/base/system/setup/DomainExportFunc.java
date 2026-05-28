package xyz.elidom.base.system.setup;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.elidom.base.model.SeedEntity;
import xyz.elidom.dbist.annotation.ChildEntity;
import xyz.elidom.dbist.annotation.Relation;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.dbist.dml.Filter;
import xyz.elidom.dbist.dml.Order;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.entity.relation.IdFindable;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.util.SysValueUtil;
import xyz.elidom.util.ClassUtil;
import xyz.elidom.util.FormatUtil;

/**
 * Domain Export를 위한 유틸리티 클래스 
 * 
 * @author shortstop
 */
public class DomainExportFunc {

	/**
	 * Logger
	 */
	private static Logger logger = LoggerFactory.getLogger(DomainExportFunc.class);
	
	/**
	 * Default Field List
	 */
	private static final List<String> DEFAULT_FIELDS = OrmConstants.ENTITY_FIELD_DEFAULT_IGNORED_LIST;
	
	/**
	 * Query Manager
	 */
	private IQueryManager queryManager;
	/**
	 * Seed data를 Seed Server로 export 하는 모드인지 여부 
	 */
	private boolean isSeedLocalMode;
	
	public DomainExportFunc(IQueryManager queryManager) {
		this.queryManager = queryManager;
		this.isSeedLocalMode = SetupUtil.isSeedLocalMode();
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////
	// 								Seed Export
	/////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Export Domain Data 
	 * 
	 * @return
	 */
	public boolean exportDomainData() {
		// 1. Seed Meta 파일로 부터 Export할 Entity 리스트를 추출한다.
		List<SeedEntity> seedEntities = SetupUtil.parseSeedMetaFile();
		
		// 2. Upload 할 SeedRepository ID를 설정 
		Long domainId = Domain.currentDomainId();
		
		// 3. Export Domain Data
		this.exportDomainDataBySeedMeta(domainId, seedEntities);
		
		return true;
	}
	
	/**
	 * Seed Data Export To Seed Server
	 * 
	 * @param domainId
	 * @param seedEntities
	 */
	private void exportDomainDataBySeedMeta(Long domainId, List<SeedEntity> seedEntities) {
		for(SeedEntity seedEntity : seedEntities) {
			String entityClassName = seedEntity.getEntity();
			Class<?> entityClass = ClassUtil.forName(entityClassName);
			String bundle = seedEntity.getBundle();
			Table table = entityClass.getAnnotation(xyz.elidom.dbist.annotation.Table.class);
			
			if (table != null) {
				// 엔티티에 대한 데이터 조회 
				String subentities = seedEntity.getSubEntities();
				List<?> dataList = this.searchData(entityClass, new String[] { OrmConstants.ENTITY_FIELD_DOMAIN_ID }, domainId);
				dataList = this.minifyDataList(entityClass, dataList);
				
				// Master Only 데이터 업로드 
				if(SysValueUtil.isEmpty(subentities)) {
					this.exportMasterOnlySeed(domainId, bundle, entityClass, table, dataList);
					
				// Master Detail 데이터 업로드 
				} else {
					this.exportMasterDetailSeed(domainId, bundle, entityClass, table, subentities, dataList);
				}
				
				logger.info("Entity [" + entityClassName + "] - [" + dataList.size() + "] count data exported successfully!");
			}
		}
	}

	/**
	 * 필요없는 값들은 null로 설정하여 JSON Content에서 제거한다. 
	 * 
	 * @param entityClass
	 * @param dataList
	 * @return
	 */
	private List<?> minifyDataList(Class<?> entityClass, List<?> dataList) {
		boolean hasDomainId = ClassUtil.hasField(entityClass, OrmConstants.ENTITY_FIELD_DOMAIN_ID);
		boolean hasCreatorId = ClassUtil.hasField(entityClass, OrmConstants.ENTITY_FIELD_CREATOR_ID);
		boolean hasUpdaterId = ClassUtil.hasField(entityClass, OrmConstants.ENTITY_FIELD_UPDATER_ID);
		boolean hasCreatedAt = ClassUtil.hasField(entityClass, OrmConstants.ENTITY_FIELD_CREATED_AT);
		boolean hasUpdatedAt = ClassUtil.hasField(entityClass, OrmConstants.ENTITY_FIELD_UPDATED_AT);
		List<Field> relationFields = findRelationFields(entityClass);
		
		for(Object data : dataList) {
			if(hasDomainId) {
				ClassUtil.setFieldValue(data, OrmConstants.ENTITY_FIELD_DOMAIN_ID, null);
			}
			
			if(hasCreatorId) {
				ClassUtil.setFieldValue(data, OrmConstants.ENTITY_FIELD_CREATOR_ID, null);
				ClassUtil.setFieldValue(data, OrmConstants.ENTITY_FIELD_CREATOR, null);
			}
			
			if(hasUpdaterId) {
				ClassUtil.setFieldValue(data, OrmConstants.ENTITY_FIELD_UPDATER_ID, null);
				ClassUtil.setFieldValue(data, OrmConstants.ENTITY_FIELD_UPDATER, null);
			}
			
			if(hasCreatedAt) {
				ClassUtil.setFieldValue(data, OrmConstants.ENTITY_FIELD_CREATED_AT, null);
			}
			
			if(hasUpdatedAt) {
				ClassUtil.setFieldValue(data, OrmConstants.ENTITY_FIELD_UPDATED_AT, null);
			}
			
			try {
				minifyRelationFields(data, relationFields);
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		
		return dataList;
	}
	
	/**
	 * entityClass에서 참조 필드를 찾아 리턴 
	 * 
	 * @param entityClass
	 * @return
	 */
	private List<Field> findRelationFields(Class<?> entityClass) {
		List<Field> fieldList = ClassUtil.getAllFields(new ArrayList<Field>(), entityClass);
		List<Field> relationFieldList = new ArrayList<Field>();
		
		for(Field field : fieldList) {
			// 필드가 IdFindable 인 경우 즉 Relation 필드인 경우 
			if(!DEFAULT_FIELDS.contains(field.getName()) && IdFindable.class.isAssignableFrom(field.getType()) && field.isAnnotationPresent(Relation.class)) {
				field.setAccessible(true);
				relationFieldList.add(field);				
			}			
		}
		
		return relationFieldList;
	}
	
	/**
	 * Relation 필드에 대한 Minify
	 * 
	 * @param data
	 * @param relationFields
	 */
	private void minifyRelationFields(Object data, List<Field> relationFields) {
		for(Field field : relationFields) {
			Object refObject = ClassUtil.getFieldValue(data, field);
			if(refObject != null) {
				Object refId = ClassUtil.getFieldValue(refObject, OrmConstants.ENTITY_FIELD_ID);
				if(SysValueUtil.isEmpty(refId)) {
					ClassUtil.setFieldValue(data, field, null);
					Relation relation = field.getAnnotation(Relation.class);
					String refFieldName = relation.field()[0];
					ClassUtil.setFieldValue(data, refFieldName, null);
				}
			}
		}		
	}	
	
	/**
	 * 마스터 데이터를 Seed 데이터로 업로드한다. 
	 * 
	 * @param domainId
	 * @param bundle
	 * @param entityClass
	 * @param table
	 * @param dataList
	 */
	private void exportMasterOnlySeed(Long domainId, String bundle, Class<?> entityClass, Table table, List<?> dataList) {
		//if(ValueUtil.isEqual(table.idStrategy(), GenerationRule.UUID)) {
			String content = FormatUtil.toJsonString(dataList);
			SetupUtil.exportSeed(this.isSeedLocalMode, bundle, entityClass, dataList.size(), content);
		//}
	}
	
	/**
	 * 마스터 디테일 데이터를 Seed 데이터로 업로드한다. 
	 * 
	 * @param domainId
	 * @param bundle
	 * @param entityClass
	 * @param table
	 * @param subEntities
	 * @param dataList
	 */
	private void exportMasterDetailSeed(Long domainId, String bundle, Class<?> entityClass, Table table, String subEntities, List<?> dataList) {
		//if(ValueUtil.isEqual(table.idStrategy(), GenerationRule.UUID)) {
			// 1. Sub Entity 정보를 수집 
			ChildEntity[] childEntities = table.childEntities();
			List<String> subEntityList = Arrays.asList(subEntities.split(OrmConstants.COMMA));
			Map<String, List<?>> allDataSet = new HashMap<String, List<?>>();
			
			// 2. Sub Entity 별로 데이터를 조회하여 Keeping
			for(ChildEntity childEntity : childEntities) {
				Class<?> subEntityClass = childEntity.entityClass();
				
				// Self Reference는 Skip
				if(SysValueUtil.isEqual(entityClass.getName(), subEntityClass.getName())) {
					continue;
				}
				
				if(subEntityList.contains(subEntityClass.getName())) {
					List<?> subDataList = searchDetailDataList(domainId, entityClass, subEntityList, subEntityClass, childEntity);
					allDataSet.put(subEntityClass.getName(), subDataList);
				}
			}
			
			// 3. 업로드 할 DataSet을 셋업한다. 
			List<Map<String, Object>> resultList = setupMasterDetailDataSet(childEntities, allDataSet, dataList);
			
			// 4. DataSet 업로드 
			String content = FormatUtil.toJsonString(resultList);
			SetupUtil.exportSeed(this.isSeedLocalMode, bundle, entityClass, dataList.size(), content);
		//}
	}
	
	/**
	 * master - detail data set을 셋업한다. 
	 * 
	 * @param childEntities
	 * @param allDataSet
	 * @param dataList
	 * @return
	 */
	private List<Map<String, Object>> setupMasterDetailDataSet(ChildEntity[] childEntities, Map<String, List<?>> allDataSet, List<?> dataList) {
		List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();
		
		for(Object data : dataList) {
			Map<String, Object> item = SysValueUtil.newMap("master", data);
			Object masterId = ClassUtil.getFieldValue(data, OrmConstants.ENTITY_FIELD_ID);
			
			for(ChildEntity childEntity : childEntities) {
				Class<?> subEntityClass = childEntity.entityClass();
				String refIdField = realReferenceField(childEntity.refFields());
				
				if(allDataSet.containsKey(subEntityClass.getName())) {
					List<?> subDataList = allDataSet.get(subEntityClass.getName());
					if(SysValueUtil.isNotEmpty(subDataList)) {
						List<Object> subListOfMaster = new ArrayList<Object>();
						
						for(Object subData : subDataList) {
							Object refVal = ClassUtil.getFieldValue(subData, refIdField);
							if(SysValueUtil.isEqual(masterId, refVal)) {
								subListOfMaster.add(subData);
							}
						}
						
						item.put(childEntity.dataProperty(), subListOfMaster);
					}
				}
			}
			
			resultList.add(item);
		}
		
		return resultList;
	}	
	
	/**
	 * master - detail data 조회 
	 * 
	 * @param domainId
	 * @param masterEntityClass
	 * @param subEntityList
	 * @param subEntityClass
	 * @param childEntity
	 * @return
	 */
	private List<?> searchDetailDataList(Long domainId, Class<?> masterEntityClass, List<String> subEntityList, Class<?> subEntityClass, ChildEntity childEntity) {
		List<String> refFieldList = Arrays.asList(childEntity.refFields().split(OrmConstants.COMMA));
		List<?> subDataList = null;
		
		if(refFieldList.contains(OrmConstants.ENTITY_FIELD_ON_TYPE)) {
			subDataList = searchData(subEntityClass, new String[] { OrmConstants.ENTITY_FIELD_DOMAIN_ID, OrmConstants.ENTITY_FIELD_ON_TYPE }, domainId, masterEntityClass.getSimpleName());

		} else if(refFieldList.contains(OrmConstants.ENTITY_FIELD_RESOURCE_TYPE)) {
			subDataList = searchData(subEntityClass, new String[] { OrmConstants.ENTITY_FIELD_DOMAIN_ID, OrmConstants.ENTITY_FIELD_RESOURCE_TYPE }, domainId, masterEntityClass.getSimpleName());
				
		} else {
			subDataList = searchData(subEntityClass, new String[] { OrmConstants.ENTITY_FIELD_DOMAIN_ID }, domainId);
		}
		
		return this.minifyDataList(subEntityClass, subDataList);
	}
	
	/**
	 * onType, resourceType을 제외한 master 참조 아이디를 추출한다. 
	 * 
	 * @param refFieldList
	 * @return
	 */
	private String realReferenceField(String refFields) {
		List<String> refFieldList = Arrays.asList(refFields.split(OrmConstants.COMMA));
		for(String refField : refFieldList) {
			if(SysValueUtil.isNotEqual(refField, OrmConstants.ENTITY_FIELD_ON_TYPE) && SysValueUtil.isNotEqual(refField, OrmConstants.ENTITY_FIELD_RESOURCE_TYPE)) {
				return refField.trim();
			}			
		}
		
		return null;
	}
	
	/**
	 * entityClass와 검색 조건으로 조회하여 결과 리턴
	 * 
	 * @param entityClass
	 * @param addFilterFieldNames
	 * @param filterValues
	 * @return
	 */
	private List<?> searchData(Class<?> entityClass, String[] addFilterFieldNames, Object... filterValues) {
		Object condition = createSearchCondition(entityClass, addFilterFieldNames, filterValues);
		
		if(SysValueUtil.isEqual(entityClass.getSimpleName(), "Menu")) {
			Object domainId = filterValues[0];
			Query query = new Query();
			query.addFilter(new Filter(OrmConstants.ENTITY_FIELD_DOMAIN_ID, domainId));
			query.addOrder(new Order("parentId", false));
			return this.queryManager.selectList(entityClass, query);
			
		} else if(SysValueUtil.isEqual(entityClass.getSimpleName(), "Resource")) {
			Object domainId = filterValues[0];
			Query query = new Query();
			query.addFilter(new Filter(OrmConstants.ENTITY_FIELD_DOMAIN_ID, domainId));
			query.addOrder(new Order("masterId", false));
			return this.queryManager.selectList(entityClass, query);
			
		} else {
			return condition == null ? null : this.queryManager.selectList(entityClass, condition);
		}
	}
	
	/**
	 * 조회 조건을 생성하여 리턴 
	 * 
	 * @param entityClass
	 * @param addFilterFieldNames
	 * @param filterValues
	 * @return
	 */
	private Object createSearchCondition(Class<?> entityClass, String[] addFilterFieldNames, Object... filterValues) {
		try {
			Object condition = ClassUtil.newInstance(entityClass);
			
			for(int i = 0 ; i < addFilterFieldNames.length ; i++) {
				if(ClassUtil.hasField(entityClass, addFilterFieldNames[i])) {
					ClassUtil.setFieldValue(condition, addFilterFieldNames[i], filterValues[i]);
				}
			}
			
			return condition;
			
		} catch(Exception e) {
			logger.error(e.getMessage());
			return null;
		}		
	}
	
}
