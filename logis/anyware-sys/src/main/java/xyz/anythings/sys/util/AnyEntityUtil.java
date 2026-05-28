package xyz.anythings.sys.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.common.util.ReflectionUtils;
import xyz.anythings.sys.AnyConstants;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.exception.ElidomException;
import xyz.elidom.exception.server.ElidomDatabaseException;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.util.EntityUtil;
import xyz.elidom.sys.util.ThrowUtil;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ValueUtil;

/**
 * Entity 관련 유틸리티
 * 	- 모든 엔티티에 대해서 단 건 조회, 리스트 조회, 페이지네이션, 마스터 디테일 구조의 디테일 리스트 조회 공통 기능 
 * 
 * @author shortstop
 */
public class AnyEntityUtil extends EntityUtil {

	/**
	 * id로 엔티티 조회
	 * 
	 * @param exceptionWhenEmpty 존재하지 않으면 exception
	 * @param clazz
	 * @param id
	 * @return
	 */
	public static <T> T findEntityById(boolean exceptionWhenEmpty, Class<T> clazz, String id) {
		T obj = BeanUtil.get(IQueryManager.class).select(clazz, id);
		
		if(obj == null && exceptionWhenEmpty) {
			throw ThrowUtil.newNotFoundRecord("terms.menu." + clazz.getName(), id);
		} else {
			return obj;
		}
	}
	
	/**
	 * id로 락을 걸면서 엔티티 조회
	 * 
	 * @param exceptionWhenEmpty
	 * @param clazz
	 * @param id
	 * @return
	 */
	public static <T> T findEntityByIdWithLock(boolean exceptionWhenEmpty, Class<T> clazz, String id) {
		Query condition = OrmUtil.newConditionForExecution();
		condition.addFilter("id", id);
		T obj = BeanUtil.get(IQueryManager.class).selectByConditionWithLock(clazz, condition);
		
		if(obj == null) {
			throw ThrowUtil.newNotFoundRecord("terms.menu." + clazz.getName(), id);
		} else {
			return obj;
		}
	}
	
	/**
	 * id로 락을 걸면서 selectFields 대상으로만 엔티티 조회
	 * 
	 * @param exceptionWhenEmpty
	 * @param clazz
	 * @param id
	 * @param selectFields
	 * @return
	 */
	public static <T> T findEntityByIdWithLock(boolean exceptionWhenEmpty, Class<T> clazz, String id, String ... selectFields) {
		Query condition = OrmUtil.newConditionForExecution();
		condition.addSelect(selectFields);
		condition.addFilter("id", id);
		T obj = BeanUtil.get(IQueryManager.class).selectByConditionWithLock(clazz, condition);
		
		if(obj == null) {
			throw ThrowUtil.newNotFoundRecord("terms.menu." + clazz.getName(), id);
		} else {
			return obj;
		}
	}
	
	/**
	 * id로 락을 걸면서 unselectFields를 제외한 필드 대상으로만 엔티티 조회
	 * 
	 * @param domainId
	 * @param exceptionWhenEmpty
	 * @param clazz
	 * @param id
	 * @param unselectFields
	 * @return
	 */
	public static <T> T findEntityByIdByUnselectedWithLock(Long domainId, boolean exceptionWhenEmpty, Class<T> clazz, String id, String ... unselectFields) {
		Query condition = OrmUtil.newConditionForExecution(domainId);
		condition.addUnselect(unselectFields);
		condition.addFilter("id", id);
		T obj = BeanUtil.get(IQueryManager.class).selectByConditionWithLock(clazz, condition);
		
		if(obj == null) {
			throw ThrowUtil.newNotFoundRecord("terms.menu." + clazz.getName(), id);
		} else {
			return obj;
		}
	}
	
	/**
	 * id로 락을 걸면서 unselectFields를 제외한 필드 대상으로만 엔티티 조회
	 * 
	 * @param exceptionWhenEmpty
	 * @param clazz
	 * @param id
	 * @param unselectFields
	 * @return
	 */
	public static <T> T findEntityByIdByUnselectedWithLock(boolean exceptionWhenEmpty, Class<T> clazz, String id, String ... unselectFields) {
		Query condition = OrmUtil.newConditionForExecution();
		condition.addUnselect(unselectFields);
		condition.addFilter("id", id);
		T obj = BeanUtil.get(IQueryManager.class).selectByConditionWithLock(clazz, condition);
		
		if(obj == null && exceptionWhenEmpty) {
			throw ThrowUtil.newNotFoundRecord("terms.menu." + clazz.getSimpleName(), id);
		} else {
			return obj;
		}
	}
	
	/**
	 * code로 엔티티 조회
	 * 
	 * @param domainId
	 * @param exceptionWhenEmpty
	 * @param clazz
	 * @param codeName
	 * @param codeValue
	 * @return
	 */
	public static <T> T findEntityByCode(Long domainId, boolean exceptionWhenEmpty, Class<T> clazz, String codeName, String codeValue) {
		Query query = OrmUtil.newConditionForExecution(domainId);
		query.addFilter(codeName, codeValue);
		T obj = BeanUtil.get(IQueryManager.class).selectByCondition(clazz, query);
		
		if(obj == null && exceptionWhenEmpty ) {
			throw ThrowUtil.newNotFoundRecord("terms.menu." + clazz.getName(), codeValue);
		} else {
			return obj;
		}
	}
	
	/**
	 * code로 락을 걸면서 엔티티 조회
	 * 
	 * @param domainId
	 * @param exceptionWhenEmpty
	 * @param clazz
	 * @param codeName
	 * @param codeValue
	 * @return
	 */
	public static <T> T findEntityByCodeWithLock(Long domainId, boolean exceptionWhenEmpty, Class<T> clazz, String codeName, String codeValue) {
		Query query = OrmUtil.newConditionForExecution(domainId);
		query.addUnselect(AnyConstants.DEFAULT_UNSELECT_QUERY_FIELDS);
		query.addFilter(codeName, codeValue);
		T obj = BeanUtil.get(IQueryManager.class).selectByConditionWithLock(clazz, query);
		
		if(obj == null && exceptionWhenEmpty) {
			throw ThrowUtil.newNotFoundRecord("terms.menu." + clazz.getName(), codeValue);
		} else {
			return obj;
		}
	}
	
	/**
	 * clazz 엔티티에 대해서 fieldName, fieldValue으로 락을 걸며 엔티티 조회
	 *
	 * @param domainId
	 * @param exceptionWhenEmpty
	 * @param clazz
	 * @param fieldNames
	 * @param fieldValues
	 * @return
	 */
	public static <T> T findEntityByWithLock(Long domainId, boolean exceptionWhenEmpty, Class<T> clazz, String fieldNames, Object ... fieldValues) {
		return findEntityBy(domainId, exceptionWhenEmpty, true, clazz, null, fieldNames, fieldValues);
	}
	
	/**
	 * clazz 엔티티에 대해서 fieldName, fieldValue으로 락을 걸며 엔티티 조회 - 필드는 selectFields
	 *
	 * @param domainId
	 * @param exceptionWhenEmpty
	 * @param clazz
	 * @param selectFields
	 * @param fieldNames
	 * @param fieldValues
	 * @return
	 */
	public static <T> T findEntityByWithLock(Long domainId, boolean exceptionWhenEmpty, Class<T> clazz, String selectFields, String fieldNames, Object ... fieldValues) {
		return findEntityBy(domainId, exceptionWhenEmpty, true, clazz, selectFields, new String[] {}, fieldNames, fieldValues);
	}
	
	/**
	 * clazz 엔티티에 대해서 fieldName, fieldValue으로 락을 걸며 엔티티 조회 - 필드는 selectFields, unselectFields
	 * 
	 * @param domainId
	 * @param exceptionWhenEmpty
	 * @param clazz
	 * @param unselectedFields
	 * @param fieldNames
	 * @param fieldValues
	 * @return
	 */
	public static <T> T findEntityByWithLock(Long domainId, boolean exceptionWhenEmpty, Class<T> clazz, String[] unselectedFields, String fieldNames, Object ... fieldValues) {
		return findEntityBy(domainId, exceptionWhenEmpty, true, clazz, "*", unselectedFields, fieldNames, fieldValues);
	}
	
	/**
	 * clazz 엔티티에 대해서 fieldName, fieldValue으로 엔티티 조회
	 *
	 * @param domainId
	 * @param exceptionWhenEmpty
	 * @param clazz
	 * @param fieldNames
	 * @param fieldValues
	 * @return
	 */
	public static <T> T findEntityBy(Long domainId, boolean exceptionWhenEmpty, Class<T> clazz, String fieldNames, Object ... fieldValues) {
		return findEntityBy(domainId, exceptionWhenEmpty, clazz, null, fieldNames, fieldValues);
	}
	
	/**
	 * clazz 엔티티에 대해서 fieldName, fieldValue으로 엔티티 조회
	 *
	 * @param domainId
	 * @param exceptionWhenEmpty
	 * @param clazz
	 * @param selectFields
	 * @param fieldNames
	 * @param fieldValues
	 * @return
	 */
	public static <T> T findEntityBy(Long domainId, boolean exceptionWhenEmpty, Class<T> clazz, String selectFields, String fieldNames, Object ... fieldValues) {
		return findEntityBy(domainId, exceptionWhenEmpty, false, clazz, selectFields, fieldNames, fieldValues);
	}
	
	/**
	 * clazz 엔티티에 대해서 fieldName, fieldValue으로 엔티티 조회
	 * 
	 * @param domainId
	 * @param exceptionWhenEmpty
	 * @param withLock
	 * @param clazz
	 * @param selectFields
	 * @param fieldNames
	 * @param fieldValues
	 * @return
	 */
	public static <T> T findEntityBy(Long domainId, boolean exceptionWhenEmpty, boolean withLock, Class<T> clazz, String selectFields, String fieldNames, Object ... fieldValues) {
		return findEntityBy(domainId, exceptionWhenEmpty, withLock, clazz, selectFields, new String[] {}, fieldNames, fieldValues);
	}
	
	/**
	 * clazz 엔티티에 대해서 fieldName, fieldValue으로 엔티티 조회
	 * 
	 * @param domainId
	 * @param exceptionWhenEmpty
	 * @param withLock
	 * @param clazz
	 * @param selectFields
	 * @param unselectFields
	 * @param fieldNames
	 * @param fieldValues
	 * @return
	 */
	public static <T> T findEntityBy(Long domainId, boolean exceptionWhenEmpty, boolean withLock, Class<T> clazz, String selectFields, String[] unselectFields, String fieldNames, Object ... fieldValues) {
		Query condition = OrmUtil.newConditionForExecution(domainId);

		if(ValueUtil.isNotEmpty(selectFields) && ValueUtil.isNotEqual(selectFields, "*")) {
			condition.addSelect(selectFields.split(SysConstants.COMMA));
		}
		
		unselectFields = (ValueUtil.isEmpty(unselectFields) && withLock) ? AnyConstants.DEFAULT_UNSELECT_QUERY_FIELDS : unselectFields;
 		if(ValueUtil.isNotEmpty(unselectFields)) {
			condition.addUnselect(unselectFields);
		}
 		
		String[] keyArr = fieldNames.split(SysConstants.COMMA);

		if (keyArr.length != fieldValues.length) {
			throw ThrowUtil.newMismatchMapKeyValue();
		}

		for (int i = 0; i < keyArr.length; i++) {
			condition.addFilter(keyArr[i], fieldValues[i]);
		}
		
		IQueryManager queryMgr = BeanUtil.get(IQueryManager.class);
		T obj = withLock ? queryMgr.selectByConditionWithLock(clazz, condition) : queryMgr.selectByCondition(clazz, condition);
		
		if(obj == null && exceptionWhenEmpty) {
			throw ThrowUtil.newNotFoundRecord("terms.menu." + clazz.getSimpleName());
		}

		return obj;
	}
	
	/**
	 * clazz 엔티티에 대해서 fieldName, fieldValue으로 엔티티 검색
	 *
	 * @param domainId
	 * @param exceptionWhenEmpty
	 * @param clazz
	 * @param fieldNames
	 * @param fieldValues
	 * @return
	 */
	public static <T> List<T> searchEntitiesBy(Long domainId, boolean exceptionWhenEmpty, Class<T> clazz, String fieldNames, Object ... fieldValues) {
		return searchEntitiesBy(domainId, exceptionWhenEmpty, clazz, null, fieldNames, fieldValues);
	}
	
	/**
	 * clazz 엔티티에 대해서 fieldName, fieldValue으로 엔티티 검색
	 *
	 * @param domainId
	 * @param exceptionWhenEmpty
	 * @param clazz
	 * @param selectFields
	 * @param fieldNames
	 * @param fieldValues
	 * @return
	 */
	public static <T> List<T> searchEntitiesBy(Long domainId, boolean exceptionWhenEmpty, Class<T> clazz, String selectFields, String fieldNames, Object ... fieldValues) {
		Query condition = OrmUtil.newConditionForExecution(domainId);

		if(ValueUtil.isNotEmpty(selectFields)) {
			condition.addSelect(selectFields.split(SysConstants.COMMA));
		}

 		String[] keyArr = fieldNames.split(SysConstants.COMMA);

		if (keyArr.length != fieldValues.length) {
			throw ThrowUtil.newMismatchMapKeyValue();
		}

		for (int i = 0; i < keyArr.length; i++) {
			condition.addFilter(keyArr[i], fieldValues[i]);
		}
		
		List<T> list = BeanUtil.get(IQueryManager.class).selectList(clazz, condition);
		
		if(ValueUtil.isEmpty(list) && exceptionWhenEmpty) {
			throw ThrowUtil.newNotFoundRecord("terms.menu." + clazz.getName());
		}

		return list;
	}
	
	/**
	 * clazz에서 fieldName, fieldValue 로 사이즈 조회 
	 * @param domainId
	 * @param clazz
	 * @param fieldNames
	 * @param fieldValues
	 * @return
	 */
	public static int selectSizeByEntity(Long domainId, Class<?> clazz, String fieldNames, Object ... fieldValues) {
		Query condition = OrmUtil.newConditionForExecution(domainId);
		
		String[] keyArr = fieldNames.split(SysConstants.COMMA);

		if (keyArr.length != fieldValues.length) {
			throw ThrowUtil.newMismatchMapKeyValue();
		}

		for (int i = 0; i < keyArr.length; i++) {
			condition.addFilter(keyArr[i], fieldValues[i]);
		}
		
		return BeanUtil.get(IQueryManager.class).selectSize(clazz, condition);
	}
	
	
	/**
	 * 마스터의 상세 리스트 조회
	 * 
	 * @param domainId
	 * @param clazz
	 * @param masterField
	 * @param masterId
	 * @return
	 */
	public static <T> List<T> searchDetails(Long domainId, Class<T> clazz, String masterField, String masterId) {
		Query condition = OrmUtil.newConditionForExecution(domainId);
		condition.addFilter(masterField, masterId);
		return BeanUtil.get(IQueryManager.class).selectList(clazz, condition);
	}
	
	/**
	 * clazz 엔티티에 대해서 fieldName, fieldValue으로 엔티티 페이지네이션 검색
	 *
	 * @param domainId
	 * @param exceptionWhenEmpty
	 * @param clazz
	 * @param limit
	 * @param page
	 * @param selectFields
	 * @param fieldNames
	 * @param fieldValues
	 * @return
	 */
	public static <T> Page<T> searchPagesBy(Long domainId, boolean exceptionWhenEmpty, Class<T> clazz, int limit, int page, String fieldNames, Object ... fieldValues) {
		return searchPagesBy(domainId, exceptionWhenEmpty, clazz, limit, page, null, fieldNames, fieldValues);
	}
	
	/**
	 * clazz 엔티티에 대해서 fieldName, fieldValue으로 엔티티 페이지네이션 검색
	 *
	 * @param domainId
	 * @param exceptionWhenEmpty
	 * @param clazz
	 * @param limit
	 * @param page
	 * @param fieldNames
	 * @param fieldValues
	 * @return
	 */
	public static <T> Page<T> searchPagesBy(Long domainId, Class<T> clazz, int limit, int page, String selectFields, String fieldNames, Object ... fieldValues) {
		Query condition = OrmUtil.newConditionForExecution(domainId, limit, page);

		if(ValueUtil.isNotEmpty(selectFields)) {
			condition.addSelect(selectFields.split(SysConstants.COMMA));
		}

 		String[] keyArr = fieldNames.split(SysConstants.COMMA);

		if (keyArr.length != fieldValues.length) {
			throw ThrowUtil.newMismatchMapKeyValue();
		}

		for (int i = 0; i < keyArr.length; i++) {
			condition.addFilter(keyArr[i], fieldValues[i]);
		}
		
		return BeanUtil.get(IQueryManager.class).selectPage(clazz, condition);
	}
	
	/**
	 * sql과 파라미터로 쿼리 리턴
	 * 
	 * @param domainId
	 * @param exceptionWhenEmpty
	 * @param resultClazz
	 * @param sql
	 * @param paramNames
	 * @param paramValues
	 * @return
	 */
	public static <T> T findItem(Long domainId, boolean exceptionWhenEmpty, Class<T> resultClazz, String sql, String paramNames, Object ... paramValues) {
		String[] keys = paramNames.split(SysConstants.COMMA);
		Map<String, Object> params = new HashMap<String, Object>();
		
		for(int i = 0 ; i < keys.length ; i++) {
			params.put(keys[i], paramValues[i]);
		}
		
		return findItem(domainId, exceptionWhenEmpty, resultClazz, sql, params);
	}
	
	/**
	 * sql과 파라미터로 쿼리 결과를 리턴 
	 * 
	 * @param domainId
	 * @param exceptionWhenEmpty
	 * @param resultClazz
	 * @param sql
	 * @param params
	 * @return
	 */
	public static <T> T findItem(Long domainId, boolean exceptionWhenEmpty, Class<T> resultClazz, String sql, Map<String, Object> params) {
		T obj = BeanUtil.get(IQueryManager.class).selectBySql(sql, params, resultClazz);
		
		if(obj == null && exceptionWhenEmpty) {
			throw new ElidomRuntimeException("Failed to find item by sql!");
		}
		
		return obj;
	}
	
	/**
	 * 엔티티 에서 하나의 컬럼에 대한 결과만 조회 한다. 
	 * 
	 * @param domainId
	 * @param exceptionWhenEmpty
	 * @param resultClazz -- 결과 컬럼 타입 
	 * @param entityClazz -- 조회할 Entity
	 * @param selectField -- 조회할 컬림 
	 * @param fieldNames  -- 조회 조건 필드s
	 * @param fieldValues -- 조회 조건 값s
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T findItemOneColumn(Long domainId, boolean exceptionWhenEmpty, Class<T> resultClazz, Class<?> entityClazz, String selectField, String fieldNames, Object ... fieldValues) {
			
		try {
			// 1. entity To Object 
			Object entityClass = entityClazz.getDeclaredConstructor().newInstance();
			// 2. entity 조회 
			entityClass = AnyEntityUtil.findEntityBy(domainId, exceptionWhenEmpty, entityClazz, selectField, fieldNames, fieldValues);
			// 3. entity 에서 하나의 필드만 리턴 오브젝트에 할당 
			T retObj = (T) ReflectionUtils.getField(entityClazz, selectField).get(entityClass);
			// 4. 리턴 
			return retObj;
		} catch (ElidomException ee) {
			throw ee;
		} catch (Exception e) {
			throw new ElidomDatabaseException(e);
		}
	}
	
	/**
	 * sql과 파라미터로 쿼리 리턴
	 * 
	 * @param domainId
	 * @param exceptionWhenEmpty
	 * @param resultClazz
	 * @param sql
	 * @param paramNames
	 * @param paramValues
	 * @return
	 */
	public static <T> List<T> searchItems(Long domainId, boolean exceptionWhenEmpty, Class<T> resultClazz, String sql, String paramNames, Object ... paramValues) {
		String[] keys = paramNames.split(SysConstants.COMMA);
		Map<String, Object> params = new HashMap<String, Object>();
		
		for(int i = 0 ; i < keys.length ; i++) {
			params.put(keys[i], paramValues[i]);
		}
		
		return searchItems(domainId, exceptionWhenEmpty, resultClazz, sql, params);
	}
	
	/**
	 * sql과 파라미터로 쿼리 결과를 리턴 
	 * 
	 * @param domainId
	 * @param exceptionWhenEmpty
	 * @param resultClazz
	 * @param sql
	 * @param params
	 * @return
	 */
	public static <T> List<T> searchItems(Long domainId, boolean exceptionWhenEmpty, Class<T> resultClazz, String sql, Map<String, Object> params) {
		List<T> list = BeanUtil.get(IQueryManager.class).selectListBySql(sql, params, resultClazz, 0, 0);
		
		if(ValueUtil.isEmpty(list) && exceptionWhenEmpty) {
			throw new ElidomRuntimeException("Failed to search items by sql!");
		}
		
		return list;
	}

}
