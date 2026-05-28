/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.base.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import xyz.elidom.base.BaseConstants;
import xyz.elidom.base.entity.*;
import xyz.elidom.dbist.dml.Filter;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sec.rest.PermissionController;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.system.service.AbstractRestService;
import xyz.elidom.sys.util.SysValueUtil;
import xyz.elidom.util.BeanUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Transactional
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/menu_details")
@ServiceDesc(description = "MenuDetail Service API")
public class MenuDetailController extends AbstractRestService {
	/**
	 * QUERY - Get Menu Id by Menu Name : 'SELECT ID FROM MENUS WHERE DOMAIN_ID = :domainId AND NAME = :name'
	 */
	private static final String QUERY_MENU_ID_BY_NAME = "SELECT ID FROM menus WHERE DOMAIN_ID = :domainId AND NAME = :name";
	
	@Override
	protected Class<?> entityClass() {
		return MenuDetail.class;
	}

	@GetMapping( produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Search (Pagination) By Search Conditions")
	public Page<?> index(
			@RequestParam(name = "page", required = false) Integer page,
			@RequestParam(name = "limit", required = false) Integer limit, 
			@RequestParam(name = "select", required = false) String select,
			@RequestParam(name = "sort", required = false) String sort,
			@RequestParam(name = "query", required = false) String query) {
		return this.search(this.entityClass(), page, limit, select, sort, query);
	}

	@GetMapping(value = "/{menu_id}/meta", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Find Menu Detail Meta by Menu ID")
	public List<Map<String,Object>> findMenuDetailMeta(@PathVariable("menu_id") String menuId, @RequestParam(name = "no_trans_term", required = false) Boolean noTransTerm) {
		noTransTerm = noTransTerm == null ? false : noTransTerm;
		Query query = new Query();
		query.addFilter(new Filter(OrmConstants.ENTITY_FIELD_DOMAIN_ID, Domain.currentDomain().getId()));
		query.addFilter(new Filter(BaseConstants.FIELD_NAME_MENU_ID, menuId));
		
		List<MenuDetail> menuDetails = this.queryManager.selectList(MenuDetail.class, query);
		List<Map<String,Object>> metaList = new ArrayList<Map<String,Object>>();
		for(MenuDetail menuDetail : menuDetails) {
			metaList.add(this.menuDetailMeta(menuDetail.getId(), false, false));
		}
		
		return metaList;
	}

	@SuppressWarnings({ "unchecked" })
	@Transactional(readOnly=true, propagation=Propagation.NEVER)
	@GetMapping(value="/{id}/menu_meta", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Find all meta data of the menu")
	public Map<String, Object> menuDetailMeta(@PathVariable("id") String id, @RequestParam(name = "no_trans_term", required = false) boolean noTransTerm, @RequestParam(name = "ignore_on_save", required = false) boolean ignoreOnSave) {
		// 1. 메뉴에 대한 권한을 조회
		BeanUtil.get(PermissionController.class).findMenuPermissionsByUser(User.currentUser().getId(), id);

		// 2. 메뉴에 대한 최소한 권한(읽기 권한)이 있다면 메뉴 메타 정보를 조회한다.
		Map<String, Object> result = this.menuDetailMeta(id, ignoreOnSave, BaseConstants.MENU_OBJECT_MENU_NAME, BaseConstants.MENU_OBJECT_COLUMNS_NAME, BaseConstants.MENU_OBJECT_BUTTONS_NAME);

		List<MenuDetailButton> buttons = (List<MenuDetailButton>)result.get(BaseConstants.MENU_OBJECT_BUTTONS_NAME);
//		result.put(BaseConstants.MENU_OBJECT_BUTTONS_NAME, this.filterButtonByAuth(buttons, menuAuthList));
		result.put(BaseConstants.MENU_OBJECT_BUTTONS_NAME, buttons);
		
		result.get(BaseConstants.MENU_OBJECT_COLUMNS_NAME);
		result.get(BaseConstants.MENU_OBJECT_MENU_NAME);
		
//		String locale = User.currentUser().getLocale();
//		menu.setTitle(MessageUtil.getTermByCategories(locale, menu.getName(), BaseConstants.MENU_OBJECT_MENU_NAME));

		return result;
	}
	
	@GetMapping(value = "/{name}/named_meta", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Find all meta data of the menu by menuName")
	public List<Map<String,Object>> namedMenuDetailMeta(@PathVariable("name") String name) {
		String id = this.queryManager.selectBySql(QUERY_MENU_ID_BY_NAME, SysValueUtil.newMap("domainId,name", Domain.currentDomain().getId(), name), String.class);
		return this.findMenuDetailMeta(id, false);
	}


	@GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Find one by ID")
	public MenuDetail findOne(@PathVariable("id") String id) {
		MenuDetail menuDetail = this.getOne(this.entityClass(), id);
		menuDetail.setColumns(this.menuDetailColumns(id));
		menuDetail.setButtons(this.menuDetailButtons(id));
		return menuDetail;
	}

	@GetMapping(value = "/{id}/exist", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Check exists By ID")
	public Boolean isExist(@PathVariable("id") String id) {
		return this.isExistOne(this.entityClass(), id);
	}

	@PostMapping( consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@ApiDesc(description = "Create")
	public MenuDetail create(@RequestBody MenuDetail input) {
		return this.createOne(input);
	}

	@PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Update")
	public MenuDetail update(@PathVariable("id") String id, @RequestBody MenuDetail input) {
		return this.updateOne(input);
	}
	
	@DeleteMapping(value="/{id}", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Delete")
	public void delete(@PathVariable("id") String id) {
		this.deleteOne(this.entityClass(), id);
	}	

	@PostMapping(value = "/update_multiple", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Create, Update or Delete multiple at one time")
	public Boolean multipleUpdate(@RequestBody List<MenuDetail> list) {
		return this.cudMultipleData(this.entityClass(), list);
	}
	
	@GetMapping(value="/search_with_details", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Search all data with details")
	public List<Map<String, Object>> indexWithDetails(
			@RequestParam(name = "page", required = false) Integer page, 
			@RequestParam(name = "limit", required = false) Integer limit, 
			@RequestParam(name = "sort", required = false) String sort,
			@RequestParam(name = "query", required = false) String query,
			@RequestParam(name = "include_default_fields", required = false) boolean includeDefaultFields) {
		
		Page<?> pageResult = this.search(this.entityClass(), page, limit, OrmConstants.ENTITY_FIELD_ID, sort, query);
		List<?> list = pageResult.getList();
		
		List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
		for(Object data : list) {
			String id = ((MenuDetail)data).getId();
			results.add(this.findDetails(id, includeDefaultFields));
		}
		
		return results;
	}	

	@GetMapping(value = "/{id}/include_details", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Find One included all details by ID")
	public Map<String, Object> findDetails(@PathVariable("id") String id, @RequestParam(name = "include_default_fields", required = false) boolean includeDefaultFields) {
		return this.findOneIncludedDetails(id, includeDefaultFields);
	}

	@GetMapping(value = "/{id}/menu_detail_columns", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Search MenuColumn List of MenuDetail")
	public List<MenuDetailColumn> menuDetailColumns(@PathVariable("id") String id) {
		Query query = new Query();
		query.addFilter(new Filter(BaseConstants.FIELD_NAME_MENU_DETAIL_ID, id));
		query.addOrder(BaseConstants.FIELD_NAME_RANK, true);
		return this.queryManager.selectList(MenuDetailColumn.class, query);
	}

	@GetMapping(value = "/{id}/menu_detail_buttons", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Search MenuButton List of MenuDetail")
	public List<MenuDetailButton> menuDetailButtons(@PathVariable("id") String id) {
		return this.queryManager.selectList(MenuDetailButton.class, new MenuDetailButton(id));
	}

	@PostMapping(value = "/{id}/menu_detail_columns/update_multiple", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Update Multiple Menu Detail Columns")
	public List<MenuDetailColumn> updateMenuDetailColumns(@PathVariable("id") String id, @RequestBody List<MenuDetailColumn> menuDetailColumns) {
		this.cudMultipleData(MenuDetailColumn.class, menuDetailColumns);
		return this.menuDetailColumns(id);
	}

	@PostMapping(value = "/{id}/menu_detail_buttons/update_multiple", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Update Multiple Menu Detail Buttons")
	public List<MenuDetailButton> updateMenuDetailButtons(@PathVariable("id") String id, @RequestBody List<MenuDetailButton> menuDetailButtons) {
		this.cudMultipleData(MenuDetailButton.class, menuDetailButtons);
		return this.menuDetailButtons(id);
	}
	
	@PostMapping(value = "/{id}/menu_detail_columns/sync_with_entity_columns", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Synchronize Menu Detail Columns With Entity Detail Columns")
	public List<MenuDetailColumn> syncWithEntityColumns(@PathVariable("id") String id) {
		MenuDetail menuDetail = this.getOne(this.entityClass(), id);
		Resource entity = new Resource();
		entity.setName(menuDetail.getEntityId());
		entity = this.queryManager.selectByCondition(Resource.class, entity);
		List<ResourceColumn> entityColumns = this.queryManager.selectList(ResourceColumn.class, new ResourceColumn(entity.getId()));
		List<MenuDetailColumn> menuColumns = this.copyResourceColumn(id, entityColumns);
		super.multipleCud(menuColumns, null, null);		
		return this.menuDetailColumns(id);
	}
	/**
	 * Menu Meta 정보 조회
	 *
	 * @param id
	 * @param ignoreOnSave
	 * @param menuName
	 * @param columnName
	 * @param buttonName
	 * @param paramName
	 * @return
	 */
	private Map<String, Object> menuDetailMeta(String id, boolean ignoreOnSave, String menuName, String columnName, String buttonName) {
		Map<String, Object> result = new HashMap<String, Object>();
		MenuDetailController menuDetailCtrl = BeanUtil.get(MenuDetailController.class);
		MenuDetail menuDetail = menuDetailCtrl.findOne(id);//findDetails(id, ignoreOnSave);

		if(ignoreOnSave) {
			menuDetail.setDomainId(null);
			menuDetail.setCreator(null);
			menuDetail.setUpdater(null);
			menuDetail.setCreatorId(null);
			menuDetail.setUpdaterId(null);
			menuDetail.setCreatedAt(null);
			menuDetail.setUpdatedAt(null);
		}

		List<MenuDetailColumn> menuColumns = menuDetailCtrl.menuDetailColumns(id);
//		List<MenuDetailButton> menuParams = menuDetailCtrl.menuDetailButtons(id);

		if (ignoreOnSave && SysValueUtil.isNotEmpty(menuColumns)) {
			List<MenuDetailColumn> ignoreColumns = new ArrayList<MenuDetailColumn>();

			for (MenuDetailColumn column : menuColumns) {
				if (column.getIgnoreOnSave()) {
					ignoreColumns.add(column);
				}
			}

			for(MenuDetailColumn ignoreColumn : ignoreColumns) {
				menuColumns.remove(ignoreColumn);
			}
		}

		// 1. menu
		result.put(menuName, menuDetail);
		// 2. menu columns
		result.put(columnName, menuColumns);
		// 3. menu buttons. 사용자의 메뉴 권한에 따라 메뉴 버튼을 필터링한다.
		result.put(buttonName, menuDetailCtrl.menuDetailButtons(id));
		// 4. menu params
//		result.put(paramName, menuParams);
		return result;
	}
	
//	/**
//	 * 시스템 필드를 추가해야 하는 상황에서 공통소스의 Entity를 변경하는 부분이 조심 스러워 필요할 경우 협의 후 진행 -- by : lyonghwan
//	 * menuButtonList를 authList로 필터링한다.
//	 *
//	 * @param menuButtonList
//	 * @param authList
//	 * @return
//	 */
//	private List<MenuDetailButton> filterButtonByAuth(List<MenuDetailButton> menuButtonList, List<String> authList) {
//		if(ValueUtil.isEmpty(authList)) {
//			return new ArrayList<MenuDetailButton>();
//		}
//
//		if(ValueUtil.isEqual(authList.get(0), BaseConstants.MENU_QUERY_ALL_MODE)) {
//			return menuButtonList;
//		}
//
//		List<MenuDetailButton> filteredList = new ArrayList<MenuDetailButton>();
//		for(MenuDetailButton button : menuButtonList) {
//			if(ValueUtil.isEmpty(button.getAuth()) || authList.contains(button.getAuth())) {
//				filteredList.add(button);
//			}
//		}
//
//		return filteredList;
//	}
	
	/**
	 * copy resource columns
	 * 
	 * @param menuDetailId
	 * @param entityColumns
	 * @return
	 */
	private List<MenuDetailColumn> copyResourceColumn(String menuDetailId, List<ResourceColumn> entityColumns) {
		List<MenuDetailColumn> menuDetailColumns = new ArrayList<MenuDetailColumn>();
		for(ResourceColumn entityColumn : entityColumns) {
			MenuDetailColumn menuColumn = new MenuDetailColumn(menuDetailId);
			menuColumn = SysValueUtil.populate(entityColumn, menuColumn);
			// FIXED 하나의 리소스에 대해서 여러 개의 마스터 - 디테일 메뉴를 생성할 때 오류  
			menuColumn.setId(null);
			menuDetailColumns.add(menuColumn);
		}
		
		return menuDetailColumns;
	}
	
}