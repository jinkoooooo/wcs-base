/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.base.rest;

import java.util.*;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import xyz.elidom.base.BaseConfigConstants;
import xyz.elidom.base.BaseConstants;
import xyz.elidom.base.entity.Menu;
import xyz.elidom.base.entity.MenuColumn;
import xyz.elidom.base.entity.Resource;
import xyz.elidom.base.entity.ResourceColumn;
import xyz.elidom.base.model.EntityIndex;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.base.util.ResourceUtil;
import xyz.elidom.core.CoreConstants;
import xyz.elidom.core.entity.Code;
import xyz.elidom.core.entity.CodeDetail;
import xyz.elidom.core.rest.CodeController;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.ddl.Ddl;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.dbist.metadata.Table;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.orm.util.DdlUtil;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.event.CacheClearEvent;
import xyz.elidom.sys.model.BaseResponse;
import xyz.elidom.sys.rest.DomainController;
import xyz.elidom.sys.system.service.AbstractRestService;
import xyz.elidom.sys.util.AssertUtil;
import xyz.elidom.sys.util.MessageUtil;
import xyz.elidom.sys.util.SettingUtil;
import xyz.elidom.sys.util.ThrowUtil;
import xyz.elidom.sys.util.SysValueUtil;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ClassUtil;
import xyz.elidom.util.ValueUtil;

@RestController
@Transactional
@RequiredArgsConstructor
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/entities")
@ServiceDesc(description="Entity Service API")
public class ResourceController extends AbstractRestService {
	
	/**
	 * index 메소드 Default Sort
	 */
	private static final String INDEX_DEFAULT_SORT = "[{\"field\": \"name\", \"ascending\": true}]";
	/**
	 * indexWithDetails 메소드 Default Sort
	 */
	private static final String INDEX_WITH_DETAILS_DEFAULT_SORT = "[{\"field\" : \"masterId\", \"ascending\": false}]";
	/**
	 * indexWithDetails 메소드 Default Sort
	 */
	private static final String QUERY_SELECT_ID_BY_ENTITY_NAME = "SELECT ID FROM ENTITIES WHERE DOMAIN_ID = :domainId AND NAME = :name";
	
	@Override
	protected Class<?> entityClass() {
		return Resource.class;
	}

	private final RedisTemplate<String, Object> redisTemplate;

	@GetMapping( produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Search Entity (Pagination) By Search Conditions")
	public Page<?> index(
			@RequestParam(name = "page", required = false) Integer page,
			@RequestParam(name = "limit", required = false) Integer limit,
			@RequestParam(name = "select", required = false) String select,
			@RequestParam(name = "sort", required = false) String sort,
			@RequestParam(name = "query", required = false) String query) {
		
		if(SysValueUtil.isEmpty(sort)) {
			sort = INDEX_DEFAULT_SORT;
		}
		
		return this.search(this.entityClass(), page, limit, select, sort, query);
	}
	
	@GetMapping(value="/{name}/search_records_as_code", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Search Resource Data as Code")	
	public List<CodeDetail> searchResourceDataAsCode(@PathVariable("name") String entityName) {
		ResourceController ctrl = BeanUtil.get(ResourceController.class);
		Resource entity = ctrl.findOne(SysConstants.SHOW_BY_NAME_METHOD, entityName);
		String entityClassName = ResourceUtil.getEntityClassName(entity);
		Class<?> entityClass = ClassUtil.forName(entityClassName);
		boolean domainBased = ClassUtil.hasField(entityClass, SysConstants.ENTITY_FIELD_DOMAIN_ID);
		return ResourceUtil.searchRecordsAsCode(entity, domainBased);
	}
	
	@GetMapping(value="/{id}", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Find Entity By ID")
	@Cacheable(cacheNames="Resource", condition="#name == null", key="'Resource-' + #p0")
	public Resource findOne(@PathVariable("id") String id, @RequestParam(required = false) String name) {
		if(SysConstants.SHOW_BY_NAME_METHOD.equalsIgnoreCase(id)) {
			AssertUtil.assertNotEmpty(SysConstants.TERM_LABEL_NAME, name);
			return this.selectByCondition(Resource.class, new Resource(Domain.currentDomainId(), name));
		} else {
			return this.getOne(this.entityClass(), id);
		}
	}
	
	@GetMapping(value="/{id}/exist", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Check if Entity exists By ID")
	public Boolean isExist(@PathVariable("id") String id) {
		return this.isExistOne(this.entityClass(), id);
	}
	
	@PostMapping(value = "/check_import", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Check Before Import")
	public List<Resource> checkImport(@RequestBody List<Resource> list) {
		for (Resource item : list) {
			this.checkForImport(Resource.class, item);
		}
		
		return list;
	}
	
	@GetMapping(value="/export/{bundle}", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Export Entity && Entity Columns")
	public Object export(HttpServletRequest request, HttpServletResponse response, @PathVariable("bundle") String bundle) {
		Resource condition = new Resource();
		condition.setDomainId(Domain.currentDomainId());
		condition.setBundle(bundle);
		List<Resource> entityList = this.queryManager.selectList(Resource.class, condition);
		Resource entityRsc = this.findOne(SysConstants.SHOW_BY_NAME_METHOD, "Entity");
		Resource entityColRsc = this.findOne(SysConstants.SHOW_BY_NAME_METHOD, "ResourceColumn");
		ResourceController rscCtrl = BeanUtil.get(ResourceController.class);
		entityRsc = rscCtrl.resourceColumns(entityRsc.getId());
		entityColRsc = rscCtrl.resourceColumns(entityColRsc.getId());
		Workbook workbook = ResourceUtil.exportEntitiesToExcel(entityList, entityRsc.resourceColumns(), entityColRsc.resourceColumns());
		return this.excelDownloader.handleRequest(request, response, bundle, workbook);
	}
	
	@PostMapping( consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@ApiDesc(description="Create Entity")
	public Resource create(@RequestBody Resource resource) {
		return this.createOne(resource);
	}
	
	@PutMapping(value="/{id}", consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Update Entity")
	@CachePut(cacheNames="Resource", key="'Resource-' + #id")
	public Resource update(@PathVariable("id") String id, @RequestBody Resource resource) {
		return this.updateOne(resource);
	}

	@DeleteMapping(value="/{id}", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Delete Entity")
	@CacheEvict(cacheNames="Resource", key="'Resource-' + #id")
	public void delete(@PathVariable("id") String id) {
		this.deleteOne(this.entityClass(), id);
	}
	
	@PostMapping(value="/update_multiple", consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Create, Update or Delete multiple Entity at one time")
	public Boolean multipleUpdate(@RequestBody List<Resource> resourceList) {
		ResourceController ctrl = BeanUtil.get(ResourceController.class);
		
		for (Resource r : resourceList) {
			if (SysValueUtil.isEqual(r.getCudFlag_(), CoreConstants.CUD_FLAG_DELETE)) {
				ctrl.delete(r.getId());
			}
		}
		
		for (Resource r : resourceList) {
			if (SysValueUtil.isEqual(r.getCudFlag_(), CoreConstants.CUD_FLAG_UPDATE)) {
				ctrl.update(r.getId(), r);
			}
		}
		
		for (Resource r : resourceList) {
			if (SysValueUtil.isEqual(r.getCudFlag_(), CoreConstants.CUD_FLAG_CREATE)) {
				ctrl.create(r);
			}
		}
		
		return true;
	}
	
	@GetMapping(value="/search_with_details", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Search all data with details")
	public List<Map<String, Object>> indexWithDetails(
			@RequestParam(name = "page", required = false) Integer page,
			@RequestParam(name = "limit", required = false) Integer limit,
			@RequestParam(name = "sort", required = false) String sort,
			@RequestParam(name = "query", required = false) String query,
			@RequestParam(name = "include_default_fields", required = false) boolean includeDefaultFields) {
		
		if(SysValueUtil.isEmpty(sort)) {
			sort = INDEX_WITH_DETAILS_DEFAULT_SORT;
		}
		
		Page<?> pageResult = this.search(this.entityClass(), page, limit, OrmConstants.ENTITY_FIELD_ID, sort, query);
		List<?> list = pageResult.getList();
		
		List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
		for(Object data : list) {
			String id = ((Resource)data).getId();
			results.add(this.findDetails(id, includeDefaultFields));
		}
		
		return results;
	}
	
	@GetMapping(value = "/{id}/include_details", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Find One included all details by ID")
	public Map<String, Object> findDetails(@PathVariable("id") String id, @RequestParam(name = "include_default_fields", required = false) boolean includeDefaultFields) {
		return this.findOneIncludedDetails(id, includeDefaultFields);
	}
	
	@PostMapping(value="/{id}/create_default_columns", consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Create Default Entity Columns")
	@CachePut(cacheNames="ResourceColumn", key="'ResourceColumn-' + #id")
	public Resource checkResourceColumns(@PathVariable("id") String id) {
		Resource resource = this.getOne(this.entityClass(), id);
		List<ResourceColumn> columns = resource.resourceColumns();
		int rank = 0;
		
		// 기본 정보 설정
		if(SysValueUtil.isEmpty(columns)) {
			columns = new ArrayList<ResourceColumn>(5);
			
			for(String defaultField : CoreConstants.TABLE_FIELD_DEFAULT_LIST) {
				if(SysValueUtil.isEqual(defaultField, SysConstants.TABLE_FIELD_ID) || defaultField.indexOf(SysConstants.CHAR_UNDER_SCORE) > 0) {
					rank = rank + 10;
					ResourceColumn column = new ResourceColumn(id, defaultField);
					ResourceUtil.setDefaultColumnInfo(resource, column);
					column.setRank(rank);
					columns.add(column);
				}
			}
			
			this.queryManager.insertBatch(columns);
			
		// 용어, 랭킹 정보, 설명 등이 없다면 기본 정보 설정
		} else {
			for(String defaultField : CoreConstants.TABLE_FIELD_DEFAULT_LIST) {
				if(SysValueUtil.isEqual(defaultField, SysConstants.TABLE_FIELD_ID) || defaultField.indexOf(SysConstants.CHAR_UNDER_SCORE) > 0) {
					ResourceColumn c = null;
					
					for(ResourceColumn column : columns) {
						if(SysValueUtil.isEqualIgnoreCase(defaultField, column.getName())) {
							c = column;
							break;
						}
					}
					
					if(c == null) {
						c = new ResourceColumn(id, defaultField);
						columns.add(c);
					}
				}
			}
			
			for(ResourceColumn column : columns) {
				rank = rank + 10;
				column.setRank(rank);
				ResourceUtil.setDefaultColumnInfo(resource, column);
			}
			
			this.queryManager.upsertBatch(columns);
		}
		
		return resource;
	}
	
	@PostMapping(value="/{id}/update_multiple_entity_columns", consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Create, Update or Delete multiple Entity Columns at one time")
	@CachePut(cacheNames="ResourceColumn", key="'ResourceColumn-' + #id")
	public Resource updateMultipleColumns(@PathVariable("id") String id, @RequestBody List<ResourceColumn> resourceColumnList) {
		Resource resource = this.getOne(this.entityClass(), id);
		boolean isComplexKeyType = SysValueUtil.isEqual(resource.getIdType(), GenerationRule.COMPLEX_KEY);
		int rank = 10;
		
		// 기본 필드에 대해서 Validation
		for (ResourceColumn column : resourceColumnList) {
			// ID Type이 Complex-key일 경우 가상 필드로 설정
			if (isComplexKeyType && column.getName().equalsIgnoreCase(SysConstants.TABLE_FIELD_ID)) {
				column.setVirtualField(true);
			}
			
			column.setColType(SysValueUtil.isEmpty(column.getColType()) ? SysConstants.DATA_TYPE_STRING : column.getColType());
			rank = (column.getRank() == null || column.getRank() == 0) ? rank + 10 : column.getRank();
			column.setRank(rank);
			
			if(SysValueUtil.isEqualIgnoreCase(SysConstants.DATA_TYPE_STRING, column.getColType()) && (column.getColSize() == null || column.getColSize() == 0)) {
				column.setColSize(50);
			}

			ResourceUtil.setDefaultColumnInfo(resource, column);
		}
		
		this.cudMultipleData(ResourceColumn.class, resourceColumnList);
		resource.resourceColumns();
		return resource;
	}
	
	@GetMapping(value="/{id}/entity_columns", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Find Entity Columns By Entity ID")
	@Cacheable(cacheNames="ResourceColumn", key="'ResourceColumn-' + #p0")
	public Resource resourceColumns(@PathVariable("id") String id) {
		Resource resource = this.getOne(this.entityClass(), id);
		resource.resourceColumns();
		return resource;
	}
	
	@GetMapping(value="/{name}/meta", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Find Entity Columns By Entity Name")
	public Resource resourceColumnsByMeta(@PathVariable("name") String name) {
		ResourceController ctrl = BeanUtil.get(ResourceController.class);
		String id = this.queryManager.selectBySql(QUERY_SELECT_ID_BY_ENTITY_NAME, SysValueUtil.newMap(OrmConstants.ENTITY_DOMAIN_ID_AND_NAME, Domain.currentDomain().getId(), name), String.class);
		if(id == null && SysValueUtil.isEqual(name, "Resource")) {
			name = "Entity";
			id = this.queryManager.selectBySql(QUERY_SELECT_ID_BY_ENTITY_NAME, SysValueUtil.newMap(OrmConstants.ENTITY_DOMAIN_ID_AND_NAME, Domain.currentDomain().getId(), name), String.class);
		}
		
		// 1. Find 부분만 캐쉬 적용
		Resource resource = ctrl.findOne(id, null);
		List<ResourceColumn> columns = resource.resourceColumns();
		String locale = User.currentUser().getLocale();
		resource.setTitle(MessageUtil.getTermByCategories(locale, name, OrmConstants.LABEL_KEY, BaseConstants.FIELD_NAME_TITLE, BaseConstants.FIELD_NAME_MENU));
		
		// 2. 아래 코드 및 용어 및 공통 코드를 조회 - 이 부분 때문에 캐쉬 적용 안 함 
		if (SysValueUtil.isNotEmpty(columns)) {
			this.translateEntityColumnNames(locale, columns);
			this.fillCodeData(columns);
			resource.setItems(columns);
		}
		
		return resource;
	}
	
	/**
	 * entityColumns 컬럼들의 컬럼명을 번역한다.
	 * 
	 * @param locale
	 * @param entityColumns
	 */	
	private void translateEntityColumnNames(String locale, List<ResourceColumn> entityColumns) {
		for (ResourceColumn column : entityColumns) {
			String termKey = SysValueUtil.isEmpty(column.getTerm()) ? SysConstants.TERM_LABELS + column.getName() : column.getTerm();
			column.setTerm(MessageUtil.getLocaleTerm(locale, termKey, termKey));
		}
	}
	
	/**
	 * entityColumns 컬럼들 중 Grid 편집기가 CodeCombo인 경우 해당 컬럼에 코드 데이터를 추가한다. 
	 * 
	 * @param entityColumns
	 */
	private void fillCodeData(List<ResourceColumn> entityColumns) {
		if(SysValueUtil.toBoolean(SettingUtil.getValue(BaseConfigConstants.CODE_COMBO_DATA_FILL_AT_SERVER, SysConstants.TRUE_STRING))) {
			CodeController codeCtrl = BeanUtil.get(CodeController.class);
			
			for (ResourceColumn column : entityColumns) {
				if (SysValueUtil.isEqual(BaseConstants.REF_TYPE_COMMON_CODE, column.getRefType()) && SysValueUtil.isNotEmpty(column.getRefName()) && SysValueUtil.isNotEmpty(column.getGridEditor()) && column.getGridEditor().startsWith(BaseConstants.GRID_CODE_EDITOR_PREFIX)) {
					Code code = codeCtrl.findOne(SysConstants.SHOW_BY_NAME_METHOD, column.getRefName());
					column.setCodeList(code.getItems());
					
				} else if(SysValueUtil.isEqual(BaseConstants.REF_TYPE_ENTITY, column.getRefType()) && SysValueUtil.isNotEmpty(column.getRefName()) && SysValueUtil.isNotEmpty(column.getGridEditor()) && column.getGridEditor().equals("resource-code")) {
					List<CodeDetail> codeItems = this.searchResourceDataAsCode(column.getRefName());
					column.setCodeList(codeItems);
				}
			}
		}
	}
	
	@PostMapping(value="/{id}/create_entity_columns", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Create Entity Columns By Entity ID")
	public Resource syncResourceColumnsWithEntity(@PathVariable("id") String id) {
		// 1. Resource 데이터 추출 
		this.syncTableAndResourceColumns(id, true, false);
		
		// 2. Clear Cache Resource Column
		BeanUtil.get(ResourceController.class).clearCache();
		
		// 3. Resource Column 조회 
		return BeanUtil.get(ResourceController.class).resourceColumns(id);
	}
	
	@PostMapping(value="/{id}/sync_resource/from_menu", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Syncrhonize Entity Columns By Menu")
	public BaseResponse syncFromMenu(@PathVariable("id") String id) {
		// 1. Resource 조회
		Resource resource = this.queryManager.select(Resource.class, id);
		
		// 2. 메뉴 조회
		Menu condition = new Menu(resource.getDomainId(), resource.getName());
		Menu menu = this.queryManager.selectByCondition(Menu.class, condition);
		
		if(menu != null) {
			resource.resourceColumns();
			List<ResourceColumn> items = resource.getItems();
			
			Query mcCond = OrmUtil.newConditionForExecution(resource.getDomainId());
			mcCond.addFilter("menuId", menu.getId());
			mcCond.addOrder("rank", true);
			List<MenuColumn> menuCols = this.queryManager.selectList(MenuColumn.class, mcCond);
			
			for(MenuColumn menuCol : menuCols) {
				ResourceColumn rc = null;
				for(ResourceColumn rscCol : items) {
					if(SysValueUtil.isEqualIgnoreCase(rscCol.getName(), menuCol.getName())) {
						rc = rscCol;
						break;
					}
				}
				
				if(rc == null) {
					rc = SysValueUtil.populate(menuCol, new ResourceColumn());
					rc.setEntityId(resource.getId());
					rc.setId(null);
					this.queryManager.insert(rc);
				} else {
					String rcId = rc.getId();
					ResourceColumn newRc = SysValueUtil.populate(menuCol, rc);
					newRc.setEntityId(resource.getId());
					newRc.setId(rcId);
					this.queryManager.update(newRc);
				}
			}
		} else {
			throw ThrowUtil.newValidationErrorWithNoLog("이름이 [" + resource.getName() + "]인 메뉴가 존재하지 않습니다.");
		}
		
		// 5. Clear Cache Resource Column
		BeanUtil.get(ResourceController.class).clearCache();
		
		// 6. Resource Column 조회 
		return new BaseResponse(true, "ok");
	}
	
	@PostMapping(value="/{id}/sync_resource/to_other_domains", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Syncronize entities to other domains")
	public BaseResponse syncReourceToOtherDomains(@PathVariable("id") String id) {
		// 1. Resource 조회
		Resource resource = this.queryManager.select(Resource.class, id);
		resource.resourceColumns();
		List<ResourceColumn> items = resource.getItems();
		
		// 2. Domain 조회
		List<Domain> domains = this.queryManager.selectList(Domain.class, new Domain());
		
		// 3. 도메인 별로 동일한 이름의 엔티티 & 엔티티 컬럼 제거
		for(Domain domain : domains) {
			if(SysValueUtil.isNotEqual(resource.getDomainId(), domain.getId())) {
				Resource rCondition = new Resource(domain.getId(), resource.getName());
				Resource domainRsc = this.queryManager.selectByCondition(Resource.class, rCondition);
				
				if(domainRsc != null) {
					ResourceColumn rcCondition = new ResourceColumn(domainRsc.getId());
					rcCondition.setDomainId(domain.getId());
					this.queryManager.deleteList(ResourceColumn.class, rcCondition);
					this.queryManager.delete(domainRsc);
				}
			}
		}
		
		// 4. 도메인 별로 엔티티 복사
		for(Domain domain : domains) {
			if(SysValueUtil.isNotEqual(resource.getDomainId(), domain.getId())) {
				Resource domainRsc = SysValueUtil.populate(resource, new Resource());
				domainRsc.setDomainId(domain.getId());
				domainRsc.setId(null);
				this.queryManager.insert(domainRsc);
				List<ResourceColumn> newItems = new ArrayList<ResourceColumn>(items.size());
				
				for(ResourceColumn item : items) {
					ResourceColumn domainRscCol = SysValueUtil.populate(item, new ResourceColumn());
					domainRscCol.setDomainId(domain.getId());
					domainRscCol.setEntityId(domainRsc.getId());
					domainRscCol.setId(null);
					newItems.add(domainRscCol);
				}
				
				this.queryManager.insertBatch(newItems);
			}
		}
		
		// 5. Clear Cache Resource Column
		BeanUtil.get(ResourceController.class).clearCache();
		
		// 6. Resource Column 조회 
		return new BaseResponse(true, "ok");
	}
	
	@PutMapping(value="/sync_managed_columns", consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Synchronize multiple Table, Entity, Menu Columns at one time")
	public String syncManagedColumnsWithEntity(@RequestBody List<String> resourceIdList) {
		for(String resourceId : resourceIdList) {
			this.syncTableAndResourceColumns(resourceId, true, true);
		}
		
		BeanUtil.get(ResourceController.class).clearCache();
		BeanUtil.get(MenuController.class).clearCache();
		return SysConstants.OK_STRING;
	}	
	
	@PutMapping(value = "/clear_resource_cache", produces = MediaType.APPLICATION_JSON_VALUE)
	@CacheEvict(cacheNames = "Resource", allEntries = true)
	public boolean clearResourceCache() {
		Set<String> keys = redisTemplate.keys("Resource*");
		if (ValueUtil.isNotEmpty(keys)) {
			redisTemplate.delete(keys);
		}

		return true;
	}
	
	@PutMapping(value = "/clear_resource_column_cache", produces = MediaType.APPLICATION_JSON_VALUE)
	@CacheEvict(cacheNames = "ResourceColumn", allEntries = true)
	public boolean clearResourceColumnCache() {
		Set<String> keys = redisTemplate.keys("ResourceColumn*");
		if (ValueUtil.isNotEmpty(keys)) {
			redisTemplate.delete(keys);
		}

		return true;
	}
	
	@PutMapping(value = "/clear_cache", produces = MediaType.APPLICATION_JSON_VALUE)
	public boolean resourceClearCache() {
		return BeanUtil.get(DomainController.class).requestClearCache("resource");
	}

	public boolean clearCache() {
		ResourceController resourceCtrl = BeanUtil.get(ResourceController.class);
		resourceCtrl.clearResourceCache();
		resourceCtrl.clearResourceColumnCache();
		return true;
	} 
	
	@ApiDesc(description = "Find Extends Entity By Name")
	@Cacheable(cacheNames = "Resource", key = "'Resource-custom-' + #p0")
	public Resource findExtResource(String name) {
		Resource extEntity = new Resource();
		ResourceController resourceController = BeanUtil.get(this.getClass());

		try {
			Resource master = resourceController.findOne(SysConstants.SHOW_BY_NAME_METHOD, name);
			if (SysValueUtil.isEmpty(master) || !SysValueUtil.toBoolean(master.getExtEntity()))
				return extEntity;

			StringJoiner sql = new StringJoiner("\n");
			sql.add("SELECT ID FROM ENTITIES");
			sql.add("WHERE DOMAIN_ID = :domainId");
			sql.add("AND MASTER_ID = :masterId");
			sql.add("AND NAME LIKE '%Ext'");

			Map<String, Object> paramMap = new HashMap<String, Object>();
			paramMap.put("domainId", Domain.currentDomainId());
			paramMap.put("masterId", master.getId());

			extEntity = queryManager.selectBySql(sql.toString(), paramMap, Resource.class);
			if (SysValueUtil.isNotEmpty(extEntity)) {
				extEntity = resourceController.resourceColumns(extEntity.getId());
			}
		} catch (Exception e) {
			return extEntity;
		}

		return extEntity;
	}
	
	/**
	 * EntityClass 기준으로 테이블 컬럼, 엔티티 컬럼을 동기화 
	 * 
	 * @param id
	 * @param resouceColumnSync
	 * @param menuColumnSync
	 * @return
	 */
	private int syncTableAndResourceColumns(String id, boolean resouceColumnSync, boolean menuColumnSync) {
		// 1. Resource 데이터 추출
		Resource resource = this.queryManager.select(Resource.class, id);
		Class<?> entityClass = Resource.findClassByEntityName(resource.getName());
		
		// 2. 엔티티 - 테이블 컬럼 동기화
		Table domainTable = BeanUtil.get(Ddl.class).getTable(Domain.class);
		int changeCount = DdlUtil.syncEntityColumns(domainTable.getDomain(), entityClass);
		
		// 3. 엔티티 - 엔티티 컬럼 동기화
		if(resouceColumnSync) {
			ResourceUtil.syncEntityColumnsWithEntity(entityClass, resource);
		}
		
		// 4. 엔티티 - 메뉴 컬럼 동기화
		if(menuColumnSync) {
			String sql = "select id from menus where domain_id = :domainId and name = :resourceName and resource_type = 'ENTITY' and resource_name = :resourceName";
			String menuId = this.queryManager.selectBySql(sql, SysValueUtil.newMap("domainId,resourceName", resource.getDomainId(), resource.getName()), String.class);
			if(menuId != null) {
				ResourceUtil.syncMenuColumnsWithEntity(menuId);
			}
		}
		
		// 5. 인덱스 동기화
		DdlController ddlCtrl = BeanUtil.get(DdlController.class);
		Ddl ddl = BeanUtil.get(Ddl.class);
		String tableName = resource.getTableName();
		List<EntityIndex> indexes = ddlCtrl.indexList(id);
		
		// 5.1 인덱스 리스트
		List<String> indexNameList = new ArrayList<String>();
		for(EntityIndex index : indexes) {
			indexNameList.add(index.getEntityIdxName());
		}
		
		// 5.2 인덱스 전체 삭제
		String dropIndexTemplate = ddl.getDdlMapper().dropIndexTemplate();
		ddl.executeDDL(tableName, dropIndexTemplate, SysValueUtil.newMap("tableName,indexes", tableName, indexNameList));
		
		// 5.3 인덱스 생성
		for(EntityIndex index : indexes) {			
			String createIndexTemplate = ddl.getDdlMapper().indexTemplate();
			Map<String, Object> indexMap = index.toMapByEntity();
			@SuppressWarnings("unchecked")
			Map<String, Object> paramMap = SysValueUtil.newMap("tableName,indexes,idxTableSpaceName", tableName, SysValueUtil.toList(indexMap), ddl.getIndexTableSpace());
			ddl.executeDDL(tableName, createIndexTemplate, paramMap);
		}

		return changeCount;
	}
	
    @EventListener(
        classes = CacheClearEvent.class,
        condition = "#event.targetResource == 'resource'"
    )
    public void handleCacheClear(CacheClearEvent event) {
        // 도메인 캐시 초기화 로직
        this.clearCache();
    }
}