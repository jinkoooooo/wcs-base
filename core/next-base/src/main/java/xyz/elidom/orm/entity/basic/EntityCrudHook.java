/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.orm.entity.basic;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.reflect.FieldUtils;
import org.springframework.core.annotation.AnnotationUtils;

import xyz.elidom.base.entity.Resource;
import xyz.elidom.base.entity.ResourceColumn;
import xyz.elidom.base.util.ResourceUtil;
import xyz.elidom.dbist.annotation.Relation;
import xyz.elidom.dbist.dml.Filter;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.dbist.util.ReflectionUtil;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.config.module.IModuleProperties;
import xyz.elidom.sys.util.SysValueUtil;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ClassUtil;

/**
 * Entity CRUD Hook
 * 
 * @author shortstop
 */
public abstract class EntityCrudHook {

	/**
	 * Id로 Entity 조회에 대한 전처리 작업 수행
	 * 
	 * @param t
	 * @return
	 */
	protected <T> T beforeFindEntity(T t) {
		return t;
	}

	/**
	 * Id로 Entity 조회에 대한 후처리 작업 수행
	 * 
	 * @param t
	 * @return
	 */
	protected <T> T afterFindEntity(T t) {
		// 확장컬럼 정보 추출.
		this.findExtension(t);
		return t;
	}

	/**
	 * Entity 생성에 대한 전처리 작업 수행
	 * 
	 * @param t
	 * @return
	 */
	protected <T> T beforeSaveEntity(T t) {
		setRefKey(t);
		return t;
	}

	/**
	 * Entity 생성에 대한 후처리 작업 수행
	 * 
	 * @param t
	 * @return
	 */
	protected <T> T afterSaveEntity(T t) {
		// 확장컬럼 정보 저장.
		this.saveExtension(t);
		return t;
	}

	/**
	 * Entity 삭제에 대한 전처리 작업 수행
	 * 
	 * @param t
	 * @return
	 */
	protected <T> T beforeDeleteEntity(T t) {
		return t;
	}

	/**
	 * Entity 삭제에 대한 후처리 작업 수행
	 * 
	 * @param t
	 * @return
	 */
	protected <T> T afterDeleteEntity(T t) {
		return t;
	}

	/**
	 * Entity 삭제에 대한 전처리 작업 수행
	 * 
	 * @param t
	 * @return
	 */
	protected <T> T beforeSearchEntities(T t) {
		return t;
	}

	/**
	 * Entity 조회에 대한 후처리 작업 수행
	 * 
	 * @param t
	 * @return
	 */
	protected <T> T afterSearchEntities(T t) {
		return t;
	}
	
	/**
	 * Entity 조회에 대한 후처리 작업 수행
	 * 
	 * @param t
	 * @return
	 */
	protected <T> T afterSearchEntities(T t, Query query) {
		this.searchExtension(t, query);
		return t;
	}

	/**
	 * Reference Object에서 값을 추출하여, Reference Id Field에 값 bind.
	 * 
	 * @param entity
	 */
	private <T> void setRefKey(T entity) {
		Class<?> clazz = entity.getClass();

		do {
			Field[] fields = clazz.getDeclaredFields();
			for (Field field : fields) {
				// Check Relation Annotation.
				Relation relation = field.getAnnotation(Relation.class);
				if (relation == null) {
					continue;
				}

				// Check Annotation Value.
				String[] refFieldNames = (String[]) AnnotationUtils.getValue(relation, OrmConstants.FIELD_KEY);
				if (refFieldNames == null) {
					continue;
				}

				Object refObject = ClassUtil.getFieldValue(entity, clazz, field.getName());
				if (refObject == null) {
					continue;
				}

				// Get Reference Object
				Object value = ClassUtil.getFieldValue(refObject, OrmConstants.ENTITY_FIELD_ID);

				if (value == null) {
					value = 0L;
				}

				// Set Data
				try {
					FieldUtils.writeField(entity, refFieldNames[0], value, true);
				} catch (Throwable th) {
					continue;
				}
			}

			clazz = clazz.getSuperclass();
		} while (clazz != null && clazz instanceof Object);
	}

	/************************************************************************************************
	 * *
	 * 확정 컬럼 조회 API *
	 * *
	 ************************************************************************************************/

	/**
	 * Find 실행에 대한 확장 컬럼 조회.
	 * 
	 * @param t
	 */
	@SuppressWarnings("rawtypes")
	private <T> void findExtension(T t) {
		// 1. Validation
		if (!(t instanceof AbstractStamp))
			return;

		if (t.getClass().isAssignableFrom(Resource.class))
			return;

		// 2. Extends Resources 정보가 존재 여부 확인.
		Class<?> entityClass = t.getClass();
		Resource extResource = ResourceUtil.findExtResource(entityClass.getSimpleName());
		if (SysValueUtil.isEmpty(extResource) || SysValueUtil.isEmpty(extResource.getId())) {
			return;
		}

		// 3. Master ID 추출
		List<Field> idFieldList = ReflectionUtil.getIdField(entityClass);
		if (idFieldList.size() != 1) {
			return;
		}

		Field idField = idFieldList.get(0);

		// 4. 확장 컬럼을 포함한 모든 필드의 Data 추출
		Query condition = new Query();
		condition.addFilter(new Filter(OrmConstants.ENTITY_FIELD_DOMAIN_ID, Domain.currentDomainId()));
		condition.addFilter(new Filter(idField.getName(), ClassUtil.getFieldValue(t, idField)));
		List<Map> orderExtList = BeanUtil.get(IQueryManager.class).selectList(extResource.getTableName(), condition, Map.class);

		// 5. Master Table 과 Extends Resource Data Merge
		AbstractStamp resource = (AbstractStamp) t;
		List<ResourceColumn> resourceColumnList = extResource.getItems();

		Object value = ClassUtil.getFieldValue(t, idField);
		for (Map orderExt : orderExtList) {
			if (SysValueUtil.isNotEqual(value, orderExt.get(idField.getName())))
				continue;

			for (ResourceColumn column : resourceColumnList)
				resource.addExtprops_(column.getName(), orderExt.get(SysValueUtil.toCamelCase(column.getName(), '_')));

			break;
		}
	}

	/**
	 * Save 실행에 대한 확장 컬럼 저장.
	 * 
	 * @param t
	 */
	private <T> void saveExtension(T t) {
		// 1. Validation
		if (!(t instanceof AbstractStamp))
			return;

		AbstractStamp resource = (AbstractStamp) t;
		Map<String, Object> extPropsMap = resource.getExtprops_();
		if (SysValueUtil.isEmpty(extPropsMap))
			return;

		Class<?> targetClass = resource.getClass();
		Resource extResourceInfo = ResourceUtil.findExtResource(targetClass.getSimpleName());
		if (SysValueUtil.isEmpty(extResourceInfo) || SysValueUtil.isEmpty(extResourceInfo.getId()))
			return;

		// 2. Update Extends Field Value
		IModuleProperties moduleProperties = SysValueUtil.getModuleProperties(extResourceInfo.getBundle());
		String entityPath = moduleProperties.getScanEntityPackage() + "." + extResourceInfo.getName();
		Object extResource = ClassUtil.newInstance(entityPath);
		SysValueUtil.populate(resource, extResource);

		extPropsMap.forEach((k, v) -> {
			ClassUtil.setFieldValue(extResource, SysValueUtil.toCamelCase(k, '_'), v);
		});

		IQueryManager queryManager = BeanUtil.get(IQueryManager.class);
		queryManager.update(extResource);
	}

	/**
	 * Search 실행에 대한 확장 컬럼 조회.
	 * 
	 * @param list
	 * @param query
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <T> void searchExtension(T t, Query query) {
		// 1. Validation
		if (!(t instanceof Page)) {
			return;
		}

		Page<AbstractStamp> page = (Page<AbstractStamp>) t;

		// 2. 조회 결과가 존재하지 않을 경우 Return
		List<AbstractStamp> list = page.getList();
		if (list == null || list.isEmpty())
			return;

		// 3. Extends Resources 정보가 존재하지 않을 경우 Return
		Class<?> entityClass = list.get(0).getClass();
		Resource extEntity = ResourceUtil.findExtResource(entityClass.getSimpleName());
		if (SysValueUtil.isEmpty(extEntity) || SysValueUtil.isEmpty(extEntity.getId()))
			return;

		// 4. Master ID 추출
		List<Field> idFieldList = ReflectionUtil.getIdField(entityClass);
		if (idFieldList.size() != 1) {
			return;
		}

		Field idField = idFieldList.get(0);
		List<String> idList = new ArrayList<String>();

		for (AbstractStamp obj : list) {
			Object value = ClassUtil.getFieldValue(obj, idField);
			if (SysValueUtil.isNotEmpty(value))
				idList.add(SysValueUtil.toString(value));
		}

		// 5. 확장 컬럼을 포함한 모든 필드의 Data 추출
		Query condition = new Query();
		condition.addFilter(new Filter(OrmConstants.ENTITY_FIELD_DOMAIN_ID, Domain.currentDomainId()));
		condition.addFilter(new Filter(idField.getName(), OrmConstants.IN, idList));
		List<Map> orderExtList = BeanUtil.get(IQueryManager.class).selectList(extEntity.getTableName(), condition, Map.class);

		// 6. Master Table 과 Extends Resource Data Merge
		List<String> extFields = query.getExtselect();
		List<ResourceColumn> resourceColumnList = extEntity.getItems();

		for (AbstractStamp obj : list) {
			Object value = ClassUtil.getFieldValue(obj, idField);
			for (Map orderExt : orderExtList) {
				if (SysValueUtil.isNotEqual(value, orderExt.get(idField.getName()))) {
					continue;
				}

				for (ResourceColumn column : resourceColumnList) {
					String columnName = column.getName();
					// 6-1. 컬럼 이름이 확장 컬럼 정보에 포함되어 있을 경우, Extprop에 데이터 저장
					if (extFields == null || extFields.contains(columnName))
						obj.addExtprops_(columnName, orderExt.get(SysValueUtil.toCamelCase(columnName, '_')));
				}

				break;
			}
		}
	}
}