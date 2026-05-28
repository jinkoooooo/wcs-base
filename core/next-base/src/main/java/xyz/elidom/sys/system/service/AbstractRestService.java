/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import xyz.elidom.base.entity.Resource;
import xyz.elidom.base.util.ResourceUtil;
import xyz.elidom.dbist.annotation.ChildEntity;
import xyz.elidom.dbist.annotation.MasterDetailType;
import xyz.elidom.dbist.dml.Filter;
import xyz.elidom.dbist.dml.Order;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.dbist.metadata.Table;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.entity.basic.AbstractStamp;
import xyz.elidom.orm.entity.basic.EntityCrudHook;
import xyz.elidom.sys.SysConfigConstants;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.controller.handler.file.IWorkbookDownloadHandler;
import xyz.elidom.sys.system.converter.excel.IExcelSerializer;
import xyz.elidom.sys.util.AssertUtil;
import xyz.elidom.sys.util.EntityUtil;
import xyz.elidom.sys.util.SettingUtil;
import xyz.elidom.sys.util.ThrowUtil;
import xyz.elidom.util.ClassUtil;
import xyz.elidom.util.ValueUtil;
import xyz.elidom.util.converter.msg.IJsonParser;

/**
 * RESTful 추상화 서비스
 * 
 * @author shortstop
 */
public abstract class AbstractRestService extends EntityCrudHook {
	
	@Autowired
	protected IQueryManager queryManager;

	@Autowired
	@Qualifier("under_to_camel")
	protected IJsonParser jsonParser;
	
	@Autowired
	@Qualifier("export")
	protected IExcelSerializer excelExporter;

	@Autowired
	protected IWorkbookDownloadHandler excelDownloader;

	/**
	 * Entity Class 리턴 - 구현 서비스에서 정의 필요
	 * 
	 * @return
	 */
	protected abstract Class<?> entityClass();

	/**
	 * JSON Parser
	 * 
	 * @return
	 */
	public IJsonParser getJsonParser() {
		return this.jsonParser;
	}

	/**
	 * 키 중에 빈 값이 있는지 체크
	 * 
	 * @param keys
	 */
	protected void checkEmptyKey(Object... keys) {
		for (Object key : keys) {
			if (ValueUtil.isEmpty(key)) {
				throw ThrowUtil.newNotFoundKey();
			}
		}
	}

	/**
	 * entity object로 부터 Primary Key값을 추출한다.
	 * 
	 * @param entity
	 * @return
	 */
	protected Object[] getPkValues(Object entity) {
		Table table = this.queryManager.getTable(entity);
		String[] pkFields = table.getPkFieldNames();
		Object[] pkValues = new Object[pkFields.length];

		for (int i = 0; i < pkFields.length; i++) {
			try {
				pkValues[i] = ClassUtil.getFieldValue(entity, pkFields[i]);
			} catch (Exception e) {
				throw ThrowUtil.newInvalidKey(pkValues);
			}
		}

		return pkValues;
	}

	/**
	 * DB에 동일한 Key의 Data가 존재하는지 확인
	 * 
	 * @param input
	 * @return
	 */
	protected <T> boolean isExistOne(T input) {
		return ValueUtil.isNotEmpty(this.queryManager.select(input));
	}

	/**
	 * DB에 동일한 Key의 Data가 존재하는지 확인
	 * 
	 * @param input
	 * @return
	 */
	protected boolean isExistOne(Class<?> entityClass, Object... keys) {
		return ValueUtil.isNotEmpty(this.queryManager.select(entityClass, keys));
	}

	/**
	 * Entity 조회 keys로 찾을 수 없는 경우 null을 리턴
	 * 
	 * @param entityClass
	 * @param keys
	 * @return
	 */
	protected <T> T getOne(Class<?> entityClass, Object... keys) {
		return getOne(false, entityClass, keys);
	}
	
	/**
	 * Entity 조회 keys로 찾을 수 없는 경우 null을 리턴
	 * 
	 * @param entityClass
	 * @param keys default
	 * @return
	 */	
	protected <T> T getOne(Class<?> entityClass, boolean includeDefaultFields, Object... keys) {
		// 무한루프 수정
		return this.getOne(false, includeDefaultFields, entityClass, keys);
	}
	
	/**
	 * Entity 조회 시 keys로 찾을 수 없는 경우 예외 발생
	 * 
	 * @param withException
	 * @param entityClass
	 * @param keys
	 * @return
	 */
	protected <T> T getOne(boolean withException, Class<?> entityClass, Object... keys) {
		return this.getOne(withException, true, entityClass, keys);
	}
	
	/**
	 * Entity 조회 시 keys로 찾을 수 없는 경우 예외 발생
	 * 
	 * @param withException
	 * @param includeDefaultFields
	 * @param entityClass
	 * @param keys
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected <T> T getOne(boolean withException, boolean includeDefaultFields, Class<?> entityClass, Object... keys) {
		// 1. Check Key
		this.checkEmptyKey(keys);

		// 2. 전처리 작업
		beforeFindEntity(keys);

		// 3. 조회
		T entity = includeDefaultFields ? (T) this.queryManager.select(withException, entityClass, keys) : (T) this.queryManager.simpleSelect(withException, entityClass, keys);
		
		// 4. 후처리 작업
		this.afterFindEntity(entity);
		return entity;
	}

	/**
	 * Entity 생성
	 * 
	 * @param input
	 * @return
	 */
	protected <T> T createOne(T input) {
		// 1. Check Empty
		AssertUtil.assertNotEmpty("terms.label.parameter", input);

		// 2. 전처리 작업
		beforeSaveEntity(input);

		// 3. 저장
		@SuppressWarnings("unchecked")
		T entity = (T) this.queryManager.insert(input.getClass(), input);

		// 4. 후처리 작업
		afterSaveEntity(entity);
		return entity;
	}

	/**
	 * Entity 수정
	 * 
	 * @param input
	 * @return
	 */
	protected <T> T updateOne(T input) {
		// 1. Check Empty
		AssertUtil.assertNotEmpty("terms.label.parameter", input);

		// 2. 전처리 작업
		beforeSaveEntity(input);

		// 3. 키 추출
		Object[] keys = this.getPkValues(input);

		// 4. 조회
		@SuppressWarnings("unchecked")
		T oneToUpdate = (T) this.queryManager.select(input.getClass(), keys);

		// 5. 값 복사
		ValueUtil.cloneObject(input, oneToUpdate, SysConstants.ENTITY_FIELD_ID, SysConstants.ENTITY_FIELD_DOMAIN_ID, SysConstants.ENTITY_FIELD_CREATED_AT, SysConstants.ENTITY_FIELD_UPDATED_AT, SysConstants.ENTITY_FIELD_CREATOR_ID, SysConstants.ENTITY_FIELD_UPDATER_ID, SysConstants.ENTITY_FIELD_CREATOR, SysConstants.ENTITY_FIELD_UPDATER);

		// 6. 업데이트
		this.queryManager.upsert(oneToUpdate);

		// 7. 후처리 작업
		ValueUtil.cloneObject(oneToUpdate, input, SysConstants.ENTITY_FIELD_ID, SysConstants.ENTITY_FIELD_DOMAIN_ID, SysConstants.ENTITY_FIELD_CREATED_AT, SysConstants.ENTITY_FIELD_UPDATED_AT, SysConstants.ENTITY_FIELD_CREATOR_ID, SysConstants.ENTITY_FIELD_UPDATER_ID, SysConstants.ENTITY_FIELD_CREATOR, SysConstants.ENTITY_FIELD_UPDATER);
		afterSaveEntity(input);
		return input;
	}

	/**
	 * Entity 삭제
	 * 
	 * @param entityClass
	 * @param keys
	 */
	protected <T> void deleteOne(Class<?> entityClass, Object... keys) {
		// 1. Check Key
		this.checkEmptyKey(keys);

		// 2. 전처리 작업
		beforeDeleteEntity(keys);

		// 3. pk로 entity 조회
		T entity = this.getOne(entityClass, keys);

		// 4. 삭제
		this.queryManager.delete(entity);

		// 5. 후처리 작업
		afterDeleteEntity(keys);
	}

	/**
	 * 조건을 통한 데이터 검색(단건), 조회가 안 될 경우 null로 리턴
	 * 
	 * @param entityClass
	 * @param condition
	 * @return
	 */
	protected <T> T selectByCondition(Class<T> entityClass, Object condition) {
		return selectByCondition(false, entityClass, condition);
	}
	
	/**
	 * 조건을 통한 데이터 검색(단건), 조회가 안 될 경우 예외 발생 여부 설정
	 * 
	 * @param withException 조회가 안 될 경우 예외 발생 여부
	 * @param entityClass
	 * @param condition
	 * @return
	 */
	protected <T> T selectByCondition(boolean withException, Class<T> entityClass, Object condition) {
		// 1. 전처리 작업
		beforeFindEntity(condition);

		// 2. Condition으로 entity 조회
		T entity = (T) this.queryManager.selectByCondition(withException, entityClass, condition);

		// 3. 후처리 작업
		afterFindEntity(entity);
		return entity;
	}
	
	/**
	 * 검색을 위한 파라미터 파싱
	 * 
	 * @param page
	 * @param limit
	 * @param select
	 * @param sort
	 * @param query
	 * @return
	 */	
	protected Query parseQuery(Class<?> entityClass, Integer page, Integer limit, String select, String sort, String query) {
		Query queryObj = new Query();
		queryObj.setPageIndex(page == null ? 1 : page.intValue());
		limit = (limit == null) ? ValueUtil.toInteger(SettingUtil.getValue(SysConfigConstants.SCREEN_PAGE_LIMIT, "50")) : limit.intValue();
		queryObj.setPageSize(limit);

		if (ValueUtil.isNotEmpty(select)) {
			List<String> selectList = new ArrayList<String>(Arrays.asList(select.split(SysConstants.COMMA)));
			// 수정1. 멀티 도메인 환경에서는 엔티티가 domainId 필드를 가지고 있다면 무조건 추가되어야 함
			if(!selectList.contains(OrmConstants.TABLE_FIELD_DOMAIN_ID) && this.isDomainBased(entityClass)) {
				selectList.add(OrmConstants.TABLE_FIELD_DOMAIN_ID);
			}
			
			Resource extResource = ResourceUtil.findExtResource(entityClass.getSimpleName());
			// 확정 컬럼 정보가 존재하지 않을 경우, 기본 검색 항목에 추가 
			if (ValueUtil.isEmpty(extResource) || ValueUtil.isEmpty(extResource.getId())) {
				queryObj.setSelect(selectList);
				
			} else {
				List<String> masterColumnList = new ArrayList<String>();
				List<String> extColumnList = new ArrayList<String>();
				List<String> extColumns = ResourceUtil.resourceColumnNames(extResource.getName());

				for (String column : selectList) {
					if (extColumns.contains(column)) {
						extColumnList.add(column);
					} else {
						masterColumnList.add(column);
					}
				}

				queryObj.setSelect(masterColumnList);
				queryObj.setExtselect(extColumnList);
			}
		}

		if (ValueUtil.isNotEmpty(sort)) {
			queryObj.addOrder(this.jsonParser.parse(sort, Order[].class));
		}

		// Entity가 domainId 필드를 가졌다면 무조건 domainId를 기본 검색 조건으로 지정.  
		if (this.isDomainBased(entityClass) && ValueUtil.isNotEmpty(Domain.currentDomain())) {
			queryObj.addFilter(new Filter(SysConstants.ENTITY_FIELD_DOMAIN_ID, Domain.currentDomain().getId()));
		}

		if (limit >= 0 && ValueUtil.isNotEmpty(query)) {
			queryObj.addFilter(this.jsonParser.parse(query, Filter[].class));
		}
		
		return queryObj;
	}

	/**
	 * pagination 검색
	 * 
	 * @param page
	 * @param limit
	 * @param select
	 * @param sort
	 * @param query
	 * @return
	 */
	public Page<?> search(Class<?> entityClass, Integer page, Integer limit, String select, String sort, String query) {
		Query queryObj = this.parseQuery(entityClass, page, limit, select, sort, query);
		return this.search(entityClass, queryObj);
	}

	/**
	 * Reserved Field인 domainId 필드가 존재하는 지 체크 
	 * 
	 * @return
	 */
	protected boolean isDomainBased(Class<?> clazz) {
		return ClassUtil.hasField(clazz, SysConstants.ENTITY_FIELD_DOMAIN_ID);
	}

	/**
	 * Entity 검색
	 * 
	 * @param entityClass
	 * @param query
	 * @return
	 */
	protected <T> Page<?> search(Class<?> entityClass, Query query) {
		// 1. 전처리 작업
		beforeSearchEntities(query);

		// 2. 검색
		@SuppressWarnings("unchecked")
		Page<T> page = (Page<T>) this.queryManager.selectPage(entityClass, query);

		// 3. 후처리 작업
		afterSearchEntities(page, query);
		return page;
	}
	
	/**
	 * Import전에 Reference 필드들의 ID를 찾아 설정한다.
	 * 
	 * @param entityClass
	 * @param instance
	 * @return
	 */
	protected <T> T checkForImport(Class<?> entityClass, T instance) {
		if(AbstractStamp.class.isAssignableFrom(entityClass)) {
			AbstractStamp stamp = (AbstractStamp)instance;
			stamp._checkReferenceFields();
			stamp.validationCheck(OrmConstants.CUD_FLAG_CREATE);
		}
		
		return instance;
	}

	/**
	 * Create, Update, Delete를 한번에 처리
	 * 
	 * @param entityClass
	 * @param dataList
	 * @return
	 */
	protected <T> Boolean cudMultipleData(Class<?> entityClass, List<T> dataList) {
		List<T> createList = new ArrayList<T>();
		List<T> updateList = new ArrayList<T>();
		List<T> deleteList = new ArrayList<T>();
		Method cudMethod = EntityUtil.getCudMethod(entityClass);

		for (T data : dataList) {
			String cudFlag = EntityUtil.getCudValue(data, cudMethod);

			if (ValueUtil.isEqual(cudFlag, OrmConstants.CUD_FLAG_CREATE)) {
				createList.add(data);

			} else if (ValueUtil.isEqual(cudFlag, OrmConstants.CUD_FLAG_UPDATE)) {
				updateList.add(data);

			} else if (ValueUtil.isEqual(cudFlag, OrmConstants.CUD_FLAG_DELETE)) {
				deleteList.add(data);
			}
		}

		this.multipleCud(createList, updateList, deleteList);
		return true;
	}

	/**
	 * TODO cache 사용 여부에 따라서 Bean으로 호출해야 할 지 결정 필요
	 * multiple data create/update/delete
	 * 
	 * @param createList
	 * @param updateList
	 * @param deleteList
	 */
	protected <T> void multipleCud(List<T> createList, List<T> updateList, List<T> deleteList) {
		if (!ValueUtil.isEmpty(deleteList)) {
			for (T data : deleteList) {
				this.deleteOne(data.getClass(), getPkValues(data));
			}
		}

		if (!ValueUtil.isEmpty(updateList)) {
			for (T data : updateList) {
				this.updateOne(data);
			}
		}

		if (!ValueUtil.isEmpty(createList)) {
			for (T data : createList) {
				this.createOne(data);
			}
		}
	}
	
	/**
	 * id로 master 데이터를 조회 한 후 관련된 detail 데이터도 모두 조회하여 리턴
	 * 
	 * @param id
	 * @return
	 */
	public Map<String, Object> findOneIncludedDetails(Object id) {
		return this.findOneIncludedDetails(id, true);
	}
	
	/**
	 * id로 master 데이터를 조회 한 후 관련된 detail 데이터도 모두 조회하여 리턴
	 * 
	 * @param id
	 * @param includeDefaultFields
	 * @return
	 */
	public Map<String, Object> findOneIncludedDetails(Object id, boolean includeDefaultFields) {
		// 1. Master 데이터 조회
		Class<?> entityClass = this.entityClass();
		Map<String, Object> master = new HashMap<String, Object>(); 
		master.put("master", this.getOne(true, includeDefaultFields, entityClass, id));
		
		// 2. Master Entity 클래스로 부터 Detail 클래스의 Table Annotation에서 Detail Class 메타 정보를 찾아서 
		xyz.elidom.dbist.annotation.Table table = entityClass.getAnnotation(xyz.elidom.dbist.annotation.Table.class);
		ChildEntity[] childEntities = table.childEntities();
		
		// 3. Detail Class 메타 정보로 부터 Detail 데이터 조회 
		for(ChildEntity childEntity : childEntities) {
			master.put(childEntity.dataProperty(), this.searchDetails(id, childEntity, includeDefaultFields));
		}
		
		return master;
	}
	
	/**
	 * master id와 detail 메타 정보로 detail 데이터를 조회 
	 * 
	 * @param id
	 * @param childEntity
	 * @return
	 */
	protected Object searchDetails(Object id, ChildEntity childEntity) {
		return this.searchDetails(id, childEntity, true);
	}
	
	/**
	 * master id와 detail 메타 정보로 detail 데이터를 조회 
	 * 
	 * @param id
	 * @param childEntity
	 * @param includeDefaultFields
	 * @return
	 */
	protected Object searchDetails(Object id, ChildEntity childEntity, boolean includeDefaultFields) {
		Class<?> detailClass = childEntity.entityClass();
		Query query = new Query();
		
		if(!includeDefaultFields) {
			query.setUnselect(SysConstants.ENTITY_FIELD_DEFAULT_IGNORED_LIST);
		}
		
		if(this.isDomainBased(detailClass)) {
			query.addFilter(new Filter(OrmConstants.ENTITY_FIELD_DOMAIN_ID, Domain.currentDomainId()));
		}
		
		String[] refFieldArr = childEntity.refFields().split(SysConstants.COMMA);
		Object[] refValueArr = new Object[refFieldArr.length];
		
		for(int i = 0 ; i < refFieldArr.length ; i++) {
			String refFieldName = refFieldArr[i].trim();
			// case1. 필드명이 onType, resourceType인 경우 검색 조건에 마스터 Entity의 이름을 설정
			if(ValueUtil.isEqual(refFieldName, OrmConstants.ENTITY_FIELD_ON_TYPE) || ValueUtil.isEqual(refFieldName, OrmConstants.ENTITY_FIELD_RESOURCE_TYPE)) {
				refValueArr[i] = this.entityClass().getSimpleName();
				query.addFilter(new Filter(refFieldName, refValueArr[i]));
				
			// case2. 필드명이 onId, resourceId인 마스터 Entity의 id를 무조건 String으로 변환하여 설정
			} else if(ValueUtil.isEqual(refFieldName, OrmConstants.ENTITY_FIELD_ON_ID) || ValueUtil.isEqual(refFieldName, OrmConstants.ENTITY_FIELD_RESOURCE_ID)) {
				refValueArr[i] = ValueUtil.toString(id);
				query.addFilter(new Filter(refFieldName, refValueArr[i]));
				
			// else. 그 외의 경우 마스터 Entity의 id 정보를 설정
			} else {
				refValueArr[i] = id;
				query.addFilter(new Filter(refFieldName, refValueArr[i]));
			}
		}
		
		String relationType = childEntity.type();
		// 1 : N 관계인 경우 List로 조회 
		if(ValueUtil.isEqual(relationType, MasterDetailType.ONE_TO_MANY)) {
			return this.queryManager.selectList(detailClass, query);
		// 1 : 1 관계인 경우 Object로 조회 (한 건 조회)
		} else {
			return queryManager.selectByCondition(detailClass, query);
		}
	}	

	/**
	 * Export Search Data To Excel
	 * 
	 * @param entityClass
	 * @param query
	 * @param request
	 * @param response
	 * @return
	 */
	protected Boolean exportExcel(Class<?> entityClass, Query query, HttpServletRequest request, HttpServletResponse response) {
		Page<?> page = this.search(entityClass, query);
		Workbook workbook = this.excelExporter.serialize(this.entityClass(), page.getList());
		return this.excelDownloader.handleRequest(request, response, entityClass.getSimpleName(), workbook);
	}
}