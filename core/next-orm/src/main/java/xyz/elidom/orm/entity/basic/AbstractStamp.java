/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.orm.entity.basic;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;

import org.apache.commons.lang.reflect.FieldUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

import xyz.elidom.dbist.annotation.ChildEntity;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.DetailRemovalStrategy;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Ignore;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Relation;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.dbist.dml.Dml;
import xyz.elidom.exception.client.ElidomRecordNotFoundException;
import xyz.elidom.exception.server.ElidomServiceException;
import xyz.elidom.exception.server.ElidomValidationException;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.OrmMessageConstants;
import xyz.elidom.orm.entity.relation.IdFindable;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ClassUtil;
import xyz.elidom.util.ValueUtil;

/**
 * Entity의 최상위 클래스 
 * 
 * 1) ID 생성 전략에 따른 ID 값 자동 생성 - 객체 생성시 Stamp 모드 - Auto Increment, UUID, Meaningful 등 
 * 2) 삭제 전략에 따른 삭제 수행 (디테일 정보가 있는 마스터 데이터의 경우)
 * 3) 중복 데이터 체크 (by UniqueFields) 
 * 4) Null not allowed 필드가 빈 값인 지 체크 (by NotnullFields) 
 * 5) Import를 위해서 선행 체크되어야 할 
 */
public abstract class AbstractStamp implements Serializable {

	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = -4845692483715521395L;

	/**
	 * 강제 삭제 플래그
	 */
	public static String FORCIBLY_DELETE_FLAG = "FD";

	/**
	 * Multiple Update시 Create(c), Update(u), Delete(d) Flag
	 */
	@Ignore
	protected String cudFlag_;

	@Ignore
	private Map<String, Object> extprops_;
	
	/**
	 * @return the cudFlag_
	 */
	public String getCudFlag_() {
		return cudFlag_;
	}

	/**
	 * @param cudFlag_ the cudFlag_ to set
	 */
	public void setCudFlag_(String cudFlag_) {
		this.cudFlag_ = cudFlag_;
	}

	/**
	 * @return 확장 필드 Map
	 */
	public Map<String, Object> getExtprops_() {
		return extprops_;
	}

	/**
	 * @param extprops_ 확장 필드 Map
	 */
	public void setExtprops_(Map<String, Object> extprops_) {
		this.extprops_ = extprops_;
	}

	/**
	 * 확장 필드 Map에 키-값 추가
	 * 
	 * @param propName
	 * @param propValue
	 */
	public void addExtprops_(String propName, Object propValue) {
		if (this.extprops_ == null) {
			this.extprops_ = new HashMap<String, Object>();
		}

		this.extprops_.put(propName, propValue);
	}
	
	/**
	 * id 생성 전략에 따른 id 생성
	 */
	protected <T> void _setId_() {
		Class<?> clazz = this.getClass();
		Table tableAnn = clazz.getAnnotation(Table.class);
		String idType = tableAnn.idStrategy();

		// 1. ID Type이 NONE이거나 COMPLEX-TYPE인 경우 스킵 
		if (ValueUtil.isEqual(idType, GenerationRule.NONE) || ValueUtil.isEqual(idType, "complex-key")) {
			return;
		}
		
		// 2. id field를 찾는다. 
		String idFieldName = ValueUtil.isEmpty(tableAnn.idField()) ? OrmConstants.ENTITY_FIELD_ID : tableAnn.idField();
		Field idField = FieldUtils.getDeclaredField(clazz, idFieldName, true);
		boolean isStringType = idField.getType().isAssignableFrom(String.class);

		// 3. ID 생성 전략 - auto-increment : 데이터베이스에 맡긴다.
		if (ValueUtil.isEmpty(idType) || ValueUtil.isEqual(idType, GenerationRule.AUTO_INCREMENT)) {
			/*if (isStringType) {
				throw new ElidomValidationException(OrmMessageConstants.INVALID_DATA_TYPE_OF_ENTITY_ID_TYPE, "Data type must be ({2}) of ID Type ({1}) of entity ({0})",
						ValueUtil.toList(clazz.getSimpleName(), idType, OrmConstants.DATA_TYPE_NUMBER));
			}*/
			
			return;
		}

		// 4. Meaningful & UUID인 경우 ID 필드 타입은 String이어야 한다. 
		if (!isStringType) {
			throw new ElidomValidationException(OrmMessageConstants.INVALID_DATA_TYPE_OF_ENTITY_ID_TYPE, "Data type must be ({2}) of ID Type ({1}) of entity ({0})",
					ValueUtil.toList(clazz.getSimpleName(), idType, OrmConstants.DATA_TYPE_STRING));
		}

		String idValue = null;

		// 5. ID 생성 전략 - UUID
		if (ValueUtil.isEqual(idType, GenerationRule.UUID)) {
			Object fieldValue = ClassUtil.getFieldValue(this, idField.getName());
			if (ValueUtil.isEmpty(fieldValue) || fieldValue.toString().length() <= 10) {
				idValue = UUID.randomUUID().toString();
			}
			
		// 6. ID 생성 전략 - MEANINGFUL-ID
		} else if (ValueUtil.isEqual(idType, GenerationRule.MEANINGFUL)) {
			String meaningfulFields = tableAnn.meaningfulFields();

			if (ValueUtil.isEmpty(meaningfulFields)) {
				throw new ElidomValidationException(OrmMessageConstants.NOT_FOUND_UNIQUE_FIELDS, "Not found uniqueFields in entity ({0})", ValueUtil.toList(clazz.getName()));
			}
			
			StringJoiner key = new StringJoiner(OrmConstants.DASH);
			String[] fieldArr = StringUtils.tokenizeToStringArray(meaningfulFields, OrmConstants.COMMA);

			for (String fieldName : fieldArr) {
				Object value = ClassUtil.getFieldValue(this, fieldName);
				key.add(new StringBuilder().append(value));
			}

			idValue = key.toString();
		}

		// 4. ID 값 설정
		if (idValue != null) {
			ClassUtil.setFieldValue(this, idField, idValue);
		}
	}

	/**
	 * Detail 클래스의 참조 필드 값
	 * 
	 * @param refFieldName
	 * @param idValue
	 * @return
	 */
	private Object detailRefFieldValue(String refFieldName, Object idValue) {
		// 필드명이 onType, resourceType인 경우 검색 조건에 마스터 Entity의 이름을 설정
		if (ValueUtil.isEqual(refFieldName, OrmConstants.ENTITY_FIELD_ON_TYPE) || ValueUtil.isEqual(refFieldName, OrmConstants.ENTITY_FIELD_RESOURCE_TYPE)) {
			return this.getClass().getSimpleName();

		// 필드명이 onId, resourceId인 마스터 Entity의 id를 무조건 String으로 변환하여 설정
		} else if (ValueUtil.isEqual(refFieldName, OrmConstants.ENTITY_FIELD_ON_ID) || ValueUtil.isEqual(refFieldName, OrmConstants.ENTITY_FIELD_RESOURCE_ID)) {
			return ValueUtil.toString(idValue);

		// 그 외의 경우 마스터 Entity의 id 정보를 설정
		} else {
			return idValue;
		}
	}

	/**
	 * Master 데이터 삭제 시, 옵션에 따른 Detail 데이터 처리.
	 * 
	 * @return
	 */
	protected Object deleteDetailResource() {
		Table tableAnn = this.getClass().getAnnotation(Table.class);
		ChildEntity[] detailEntities = tableAnn.childEntities();

		if (ValueUtil.isEmpty(detailEntities)) {
			return this;
		}

		boolean forciblyDelete = ValueUtil.isEqual(this.cudFlag_, FORCIBLY_DELETE_FLAG);
		Object idValue = ClassUtil.getFieldValue(this, OrmConstants.ENTITY_FIELD_ID);
		IQueryManager qm = BeanUtil.get(IQueryManager.class);

		// Detail Entity 순회하면서 삭제 전략 적용
		for (int i = 0; i < detailEntities.length; i++) {
			ChildEntity detailEntity = detailEntities[i];
			Class<?> detailClass = detailEntity.entityClass();
			String[] refFields = detailEntity.refFields().split(OrmConstants.COMMA);
			List<?> detailList = this.detailList(detailClass, refFields, idValue, qm);

			if (ValueUtil.isEmpty(detailList)) {
				continue;
			}

			String deleteStrategy = ValueUtil.checkValue(detailEntity.deleteStrategy(), DetailRemovalStrategy.EXCEPTION);

			// Case 1. 디테일 삭제 후 디테일 콜백(Hook 실행)
			if (forciblyDelete || ValueUtil.isEqual(deleteStrategy, DetailRemovalStrategy.DESTROY)) {
				try {
					qm.deleteBatch(detailList);
				} catch (Exception e) {
				}

			// Case 2. 디테일 삭제 후 디테일 콜백 없음(Hook 실행하지 않음)
			} else if (ValueUtil.isEqual(deleteStrategy, DetailRemovalStrategy.DELETE)) {
				try {
					BeanUtil.get(Dml.class).deleteBatch(detailList);
				} catch (Exception e) {
					throw new ElidomServiceException("Failed to delete detail entity [" + detailClass.getName() + "] - " + e.getMessage(), e);
				}

			// Case 3. 디테일의 마스터 참조 아이디를 Null로 업데이트
			} else if (ValueUtil.isEqual(deleteStrategy, DetailRemovalStrategy.NULLIFY)) {
				for (Object detailObj : detailList) {
					for (int j = 0; j < refFields.length; j++) {
						String refFieldName = refFields[j];
						// 필드명이 onType, resourceType인 경우 검색 조건에 마스터 Entity의 이름을 설정
						if (ValueUtil.isNotEqual(refFieldName, OrmConstants.ENTITY_FIELD_ON_TYPE) && ValueUtil.isNotEqual(refFieldName, OrmConstants.ENTITY_FIELD_RESOURCE_TYPE)) {
							ClassUtil.setFieldValue(detailObj, refFieldName, null);
						}
					}
				}

				qm.updateBatch(detailList);

			// Case 4. 디테일이 존재 할 경우 에러
			} else {
				throw new ElidomServiceException(OrmMessageConstants.HAS_DETAIL_DATA, "There are detail data in entity ({0}), Please re-try after deleting detail data.",
						ValueUtil.toList(detailClass.getSimpleName()));
			}
		}

		return this;
	}

	/**
	 * Search detail list
	 * 
	 * @param detailClass
	 * @param refFields
	 * @param idValue
	 * @param qm
	 * @return
	 */
	private List<?> detailList(Class<?> detailClass, String[] refFields, Object idValue, IQueryManager qm) {
		Object condition = ClassUtil.newInstance(detailClass);
		for (int j = 0; j < refFields.length; j++) {
			String refField = refFields[j];
			Object refValue = this.detailRefFieldValue(refField, idValue);
			ClassUtil.setFieldValue(condition, refField, refValue);
		}

		return qm.selectList(detailClass, condition);
	}

	/**
	 * Import를 위해서 데이터를 체크한다.
	 * 참조 데이터의 id가 없고 id를 정확히 찾아낼 수 있는 데이터만 있다면 인스턴스로 부터 id를 조회한 후 id를 설정한다.
	 */
	public <T> void _checkReferenceFields() {
		Field[] fields = this.getClass().getDeclaredFields();

		for (int i = 0; i < fields.length; i++) {
			Field field = fields[i];

			if (IdFindable.class.isAssignableFrom(field.getType())) {
				// 1. get reference object value
				Object referenceObj = ClassUtil.getFieldValue(this, field.getName());
				Object refId = null;

				// 2. find id by reference object data and set reference id
				if (referenceObj != null) {
					refId = ((IdFindable) referenceObj).findAndSetId();
				}

				// 3. Relation Annotation으로 부터 연관 필드를 찾아 id를 설정
				if (field.isAnnotationPresent(Relation.class)) {
					Relation relation = field.getAnnotation(Relation.class);
					String refField = relation.field()[0];
					ClassUtil.setFieldValue(this, refField, refId);
				}
			}
		}
	}

	/**
	 * Instance에 ID 값 Binding 및 Return.
	 * 
	 * @return
	 */
	public Object findAndSetId() {
		Class<?> clazz = this.getClass();
		Table table = clazz.getAnnotation(Table.class);
		Map<String, Object> map = AnnotationUtils.getAnnotationAttributes(table);
		Object value = map.get(OrmConstants.TABLE_ANN_UNIQUE_FIELDS);

		if (ValueUtil.isEmpty(value)) {
			return null;
		}

		String tableName = (String) map.get(OrmConstants.NAME_KEY);
		String[] uniqueFields = StringUtils.tokenizeToStringArray(String.valueOf(value), OrmConstants.COMMA);

		StringBuffer sql = new StringBuffer(OrmConstants.ENTITY_ID_QUERY_PREFIX);
		sql.append(tableName).append(OrmConstants.ENTITY_ID_QUERY_WHERE);

		Map<String, Object> params = new HashMap<String, Object>(4);
		for (String fieldName : uniqueFields) {
			// Column Name 추출
			Field field = FieldUtils.getField(clazz, fieldName, true);
			Column column = field.getAnnotation(Column.class);
			Object columnName = AnnotationUtils.getAnnotationAttributes(column).get(OrmConstants.NAME_KEY);

			// 조건 Query 생성
			sql.append(OrmConstants.AND_KEY).append(OrmConstants.SPACE).append(columnName != null ? String.valueOf(columnName) : fieldName).append(OrmConstants.QUERY_BINDING).append(fieldName).append(OrmConstants.SPACE);

			// Parameter Binding.
			params.put(fieldName, ClassUtil.getFieldValue(this, fieldName));
		}

		Field idField = FieldUtils.getField(clazz, OrmConstants.ENTITY_FIELD_ID, true);
		Object id = BeanUtil.get(IQueryManager.class).selectBySql(sql.toString(), params, idField.getType());
		ClassUtil.setFieldValue(this, OrmConstants.ENTITY_FIELD_ID, id);
		return id;
	}

	/**
	 * Validation Check For Create
	 * 
	 * @param crudType
	 */
	public void validationCheck(String crudType) {
		if (ValueUtil.isEqual(crudType, OrmConstants.CUD_FLAG_UPDATE)) {
			Object self = BeanUtil.get(IQueryManager.class).select(this);
			this.checkPopulate(this, self);
		}

		// Check Not-Null Field
		this.checkNotNullData();

		// Check Duplication Data
		this.checkDuplicateData(crudType);
	}

	/**
	 * Check Not-Null Field
	 */
	private <T> void checkNotNullData() {
		Class<?> clazz = this.getClass();
		xyz.elidom.dbist.annotation.Table tableAnn = clazz.getAnnotation(xyz.elidom.dbist.annotation.Table.class);

		String[] fieldNameArr = StringUtils.tokenizeToStringArray(tableAnn.notnullFields(), OrmConstants.COMMA);
		for (String fieldName : fieldNameArr) {
			if (ValueUtil.isEmpty(ClassUtil.getFieldValue(this, fieldName))) {
				throw new ElidomServiceException(OrmMessageConstants.EMPTY_PARAM, ValueUtil.toList(fieldName));
			}
		}
	}

	/**
	 * Check Duplication Data.
	 * 
	 * @param crudType
	 */
	public <T> void checkDuplicateData(String crudType) {
		Class<?> clazz = this.getClass();
		xyz.elidom.dbist.annotation.Table tableAnn = clazz.getAnnotation(xyz.elidom.dbist.annotation.Table.class);
		String uniqueFieldsStr = tableAnn.uniqueFields();
		if (ValueUtil.isEmpty(uniqueFieldsStr)) {
			return;
		}

		Object condition = ClassUtil.newInstance(clazz);
		String[] uniqueFields = StringUtils.tokenizeToStringArray(uniqueFieldsStr, OrmConstants.COMMA);

		for (String field : uniqueFields) {
			Object value = ClassUtil.getFieldValue(this, field);
			ClassUtil.setFieldValue(condition, field, value);
		}

		Object result = BeanUtil.get(IQueryManager.class).selectList(clazz, condition);
		if (ValueUtil.isEmpty(result)) {
			return;
		}

		StringJoiner fieldValue = new StringJoiner(OrmConstants.COMMA);
		List<String> fieldNameList = new ArrayList<String>();

		for (String field : uniqueFields) {
			fieldValue.add(ValueUtil.toString(ClassUtil.getFieldValue(this, field)));
			fieldNameList.add(this.getColumnName(clazz, field));
		}

		// Create
		if (ValueUtil.isEqual(crudType, OrmConstants.CUD_FLAG_CREATE)) {
			throw new ElidomValidationException(OrmMessageConstants.DATA_DUPLICATED, fieldNameList);

		// Update
		} else if (ValueUtil.isEqual(crudType, OrmConstants.CUD_FLAG_UPDATE)) {
			@SuppressWarnings("unchecked")
			List<T> resultList = (List<T>) result;

			if (resultList.size() == 1) {
				Object inputKeyValue = this.getPrimaryKeyValue(this);
				Object getKeyValue = this.getPrimaryKeyValue(resultList.get(0));
				if (ValueUtil.isEqual(inputKeyValue, getKeyValue)) {
					return;
				}
			}

			throw new ElidomValidationException(OrmMessageConstants.DATA_DUPLICATED, fieldNameList);
		}
	}

	/**
	 * Primary Field의 값 추출. (@Primary가 존재하지 않을 경우, id Field 값 Return)
	 * 
	 * @param obj
	 * @return
	 */
	private Object getPrimaryKeyValue(Object obj) {
		Field[] declaredFields = obj.getClass().getDeclaredFields();
		
		for (Field field : declaredFields) {
			PrimaryKey ann = field.getAnnotation(PrimaryKey.class);
			if (ann != null) {
				return ClassUtil.getFieldValue(obj, field);
			}
		}

		for (Field field : declaredFields) {
			if (ValueUtil.isEqual(field.getName(), OrmConstants.ENTITY_FIELD_ID)) {
				return ClassUtil.getFieldValue(obj, field);
			}
		}

		return null;
	}

	/**
	 * Class의 Column Annotation에서 fieldName으로 컬럼명 추출
	 * 
	 * @param clazz
	 * @param fieldName
	 * @return
	 */
	private String getColumnName(Class<?> clazz, String fieldName) {
		Field field = FieldUtils.getField(clazz, fieldName, true);
		if (field == null) {
			throw new ElidomRecordNotFoundException(OrmMessageConstants.NOT_FOUND, ValueUtil.toList(clazz.getSimpleName(), fieldName));
		}
		
		if (Modifier.isStatic(field.getModifiers())) {
			return null;
		}
		
		Column columnAnn = field.getAnnotation(Column.class);
		return (columnAnn != null) ? columnAnn.name() : fieldName;
	}

	/**
	 * from instance 값을 To instance로 복사 (빈값 제외)
	 * 
	 * @param from
	 * @param to
	 * @return
	 */
	private <F, T> T checkPopulate(F from, T to) {
		List<Field> fromFieldList = ClassUtil.getAllFields(from.getClass());

		for (Field field : fromFieldList) {
			if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			
			Object value = ClassUtil.getFieldValue(from, field);
			if (value != null) {
				ClassUtil.setFieldValue(to, field.getName(), value);
			}
		}

		return to;
	}
	
	protected void setPrimaryKeyByComplexKey() {
		
	}
	/**
	 * 단일 조회 건에 대한 Complex-Key 설정.
	 * 
	 * @param t
	 */
	protected <T> void setComplexKey() {
		
	}
}