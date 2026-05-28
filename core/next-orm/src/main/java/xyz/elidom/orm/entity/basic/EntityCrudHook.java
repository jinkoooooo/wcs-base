/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.orm.entity.basic;

import java.lang.reflect.Field;

import org.apache.commons.lang.reflect.FieldUtils;
import org.springframework.core.annotation.AnnotationUtils;

import xyz.elidom.dbist.annotation.Relation;
import xyz.elidom.orm.OrmConstants;
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
	 * Entity 삭제에 대한 후처리 작업 수행
	 * 
	 * @param t
	 * @return
	 */
	protected <T> T afterSearchEntities(T t) {
		return t;
	}
	
	protected <T> T afterSearchEntities(T t, Object query) {
		return t;
	}


	/**
	 * Reference Object에서 값을 추출하여, Reference Id Field에 값 bind.  
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
				if(refObject == null) {
					continue;
				}
				
				// Get Reference Object				
				Object value = ClassUtil.getFieldValue(refObject, OrmConstants.ENTITY_FIELD_ID);
				
				if(value == null) {
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
}