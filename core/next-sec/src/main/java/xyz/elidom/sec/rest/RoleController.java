/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sec.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.dbist.dml.Filter;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sec.entity.Permission;
import xyz.elidom.sec.entity.Role;
import xyz.elidom.sec.entity.UsersRole;
import xyz.elidom.sec.model.MenuAuth;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.system.service.AbstractRestService;
import xyz.elidom.sys.system.service.params.BasicInOut;
import xyz.elidom.sys.util.AssertUtil;
import xyz.elidom.sys.util.SysValueUtil;

@RestController
@Transactional
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/roles")
@ServiceDesc(description="Role Service API")
public class RoleController extends AbstractRestService {
	/**
	 * Users of role SQL
	 */
	private static final String USERS_OF_ROLE_SQL = "select id, login, email, name from users where id in (select user_id from users_roles where role_id = :roleId)";
	/**
	 * Delete Permissions of role SQL
	 */
	private static final String DELETE_PERMISSIONS_SQL = "DELETE FROM PERMISSIONS WHERE ROLE_ID = :roleId AND RESOURCE_TYPE = 'Menu' AND RESOURCE_ID IN (SELECT ID FROM MENUS WHERE ID = :parentMenuId or PARENT_ID = :parentMenuId)";
	/**
	 * Permitted Resource SQL
	 */
	private static final String PERMITTED_RESOURCES_SQL = 
		new StringBuffer("SELECT menus.id, menus.name, menus.parent_id, permissions.resource_type resource_type, permissions.resource_id resource_id, permissions.action_name action_name ")
		.append(" FROM menus LEFT OUTER JOIN permissions ON menus.id = permissions.resource_id and permissions.role_id = :roleId and permissions.resource_type='Menu' ")
		.append("WHERE menus.domain_id = :domainId and menus.parent_id = :parentMenuId and menus.hidden_flag IS NOT TRUE ORDER BY menus.rank asc").toString();
	
	/**
	 * PERMISSION - show
	 */
	private static final String PERMISSION_SHOW = "show";
	/**
	 * PERMISSION - create
	 */
	private static final String PERMISSION_CREATE = "create";
	/**
	 * PERMISSION - update
	 */
	private static final String PERMISSION_UPDATE = "update";
	/**
	 * PERMISSION - delete
	 */
	private static final String PERMISSION_DELETE = "delete";
	
	/**
	 * userId Field Name - userId
	 */
	private static final String FIELD_USER_ID = "userId";
	/**
	 * roleId Field Name - userId
	 */
	private static final String FIELD_ROLE_ID = "roleId";
	
	/**
	 * items key - items
	 */
	private static final String KEY_ITEMS = "items";
	/**
	 * Menu entity Name - Menu
	 */
	private static final String MENU_ENTITY_NAME = "Menu";
	/**
	 * Menu Of Role Permissions Parameters Key - domainId,roleId,parentMenuId
	 */
	private static final String MENU_OF_ROLE_PERMISSION_PARAMS_KEY = "domainId,roleId,parentMenuId";
	/**
	 * Delete Parameters Key - roleId,parentMenuId
	 */
	private static final String DELETE_PERMISSION_PARAMS_KEY = "roleId,parentMenuId";
	/**
	 * 기본 소팅 조건 - '[{\"field\": \"name\", \"ascending\": true}]'
	 */
	private static final String DEFAULT_SORT_COND = "[{\"field\": \"name\", \"ascending\": true}]";	
	
	@Override
	protected Class<?> entityClass() {
		return Role.class;
	}
	
	@GetMapping( produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Search Role (Pagination) By Search Conditions")
	public Page<?> index(
			@RequestParam(name = "page", required = false) Integer page,
			@RequestParam(name = "limit", required = false) Integer limit,
			@RequestParam(name = "select", required = false) String select,
			@RequestParam(name = "sort", required = false) String sort,
			@RequestParam(name = "query", required = false) String query) {
		
		if(SysValueUtil.isEmpty(sort)) {
			sort = DEFAULT_SORT_COND;
		}
		
		return this.search(this.entityClass(), page, limit, select, sort, query);
	}
	
	@GetMapping(value="/{id}", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Find one Role by ID")
	public Role findOne(@PathVariable("id") String id, @RequestParam(required = false) String name) {
		if(SysConstants.SHOW_BY_NAME_METHOD.equalsIgnoreCase(id)) {
			AssertUtil.assertNotEmpty(SysConstants.TERM_LABEL_NAME, name);
			return this.selectByCondition(true, Role.class, new Role(name));
		} else {
			return this.getOne(true, this.entityClass(), id);
		}
	}
	
	@GetMapping(value="/{id}/exist", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Check if Role exists by ID")
	public Boolean isExist(@PathVariable("id") String id) {
		return this.isExistOne(this.entityClass(), id);
	}
	
	@PostMapping(value = "/check_import", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Check Before Import")
	public List<Role> checkImport(@RequestBody List<Role> list) {
		for (Role item : list) {
			this.checkForImport(Role.class, item);
		}
		
		return list;
	}
	
	@PostMapping( consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@ApiDesc(description="Create Role")
	public Role create(@RequestBody Role role) {
		return this.createOne(role);
	}
	
	@PutMapping(value="/{id}", consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Update Role")
	public Role update(@PathVariable("id") String id, @RequestBody Role role) {
		return this.updateOne(role);
	}

	@DeleteMapping(value="/{id}", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Delete Role")
	public void delete(@PathVariable("id") String id) {
		this.deleteOne(this.entityClass(), id);
	}
	
	@PostMapping(value="/update_multiple", consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Create, Update or Delete multiple Role at one time")
	public Boolean multipleUpdate(@RequestBody List<Role> roleList) {
		return this.cudMultipleData(this.entityClass(), roleList);
	}

	@GetMapping(value="/{role_id}/permitted_resources/{menu_id}", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Get permitted resources")
	public HashMap<Object, Object> permittedResources(@PathVariable("role_id") String id, @PathVariable("menu_id") String parentMenuId) {
		Role role = this.getOne(true, Role.class, id);	    
		Map<String, Object> paramMap = SysValueUtil.newMap(MENU_OF_ROLE_PERMISSION_PARAMS_KEY, role.getDomainId(), id, parentMenuId);
		HashMap<Object, Object> permittedResources = new HashMap<Object, Object>(1);
		permittedResources.put(KEY_ITEMS, this.queryManager.selectListBySql(PERMITTED_RESOURCES_SQL, paramMap, Permission.class, 0, 0));
		return permittedResources;
	}
	
	@GetMapping(value="/{id}/role_users", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Find users by roleId")
	public List<User> roleUser(@PathVariable("id") String id) {
		Map<String, Object> paramMap = SysValueUtil.newMap(FIELD_ROLE_ID, id);
		return this.queryManager.selectListBySql(USERS_OF_ROLE_SQL, paramMap, User.class, 0, 0);
	}
	
	@PostMapping(value="/{id}/update_users", consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Update users")
	public BasicInOut updateUsers(@PathVariable("id") String id, @RequestBody List<User> users) {
		if(!users.isEmpty()) {
			List<UsersRole> createUsersRoleList = new ArrayList<UsersRole>();
			List<String> deleteUserIdList = new ArrayList<String>();
			
			for(int i = 0 ; i < users.size() ; i++) {
				User user = users.get(i);
				if(OrmConstants.CUD_FLAG_CREATE.equalsIgnoreCase(user.getCudFlag_())) {
					createUsersRoleList.add(new UsersRole(user.getId(), id));
				} else if(OrmConstants.CUD_FLAG_DELETE.equalsIgnoreCase(user.getCudFlag_())) {
					deleteUserIdList.add(user.getId());
				}
			}
			
			if(!deleteUserIdList.isEmpty()) {
				Query query = new Query();
				query.addFilter(new Filter(FIELD_ROLE_ID, id));
				query.addFilter(new Filter(FIELD_USER_ID, OrmConstants.IN, deleteUserIdList));
				
				List<UsersRole> deleteList = this.queryManager.selectList(UsersRole.class, query);
				queryManager.deleteBatch(deleteList);
			}
			
			if(!createUsersRoleList.isEmpty()) {
				this.queryManager.insertBatch(createUsersRoleList);
			}
		}
		
		return new BasicInOut();
	}
	
	@PostMapping(value="/{id}/update_permissions", consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Update permissions")
	public BasicInOut updatePermissions(
			@PathVariable("id") String id, 
			@RequestBody List<MenuAuth> authList,
			@RequestParam(name = "parent_menu_id", required = true) String parentMenuId,
			@RequestParam(name = "delete_all", required = false) Boolean deleteAll) {
		
		if(!authList.isEmpty() || (deleteAll != null && deleteAll)) {
			// 1. Parent Menu 권한과 Parent Menu의 Sub Menu 권한을 모두 삭제한 후
			Map<String, Object> paramMap = SysValueUtil.newMap(DELETE_PERMISSION_PARAMS_KEY, id, parentMenuId);
			this.queryManager.executeBySql(DELETE_PERMISSIONS_SQL, paramMap);
			
			// 2. 모두 삭제가 아니면 authList 정보로 Parent Menu 권한과 Parent Menu의 Sub Menu 권한을 모두 생성한다.
			if(deleteAll == null || !deleteAll) {
				for(MenuAuth auth : authList) {
					String menuId = auth.getMenuId();
					
					if(auth.isShow()) {
						this.createPermission(id, menuId, PERMISSION_SHOW);
					}
					
					if(auth.isCreate()) {
						this.createPermission(id, menuId, PERMISSION_CREATE);
					}
					
					if(auth.isUpdate()) {
						this.createPermission(id, menuId, PERMISSION_UPDATE);
					}
					
					if(auth.isDelete()) {
						this.createPermission(id, menuId, PERMISSION_DELETE);
					}
				}
				
				// 3. Parent Menu 권한도 추가
				this.createPermission(id, parentMenuId, PERMISSION_SHOW);
			}
		}
		
		return new BasicInOut();
	}
	
	/**
	 * Create Permission
	 * 
	 * @param roleId
	 * @param menuId
	 * @param actionName
	 */
	private void createPermission(String roleId, String menuId, String actionName) {
		Permission permission = new Permission();
		permission.setRoleId(roleId);
		permission.setResourceType(MENU_ENTITY_NAME);
		permission.setResourceId(menuId);
		permission.setActionName(actionName);
		this.createOne(permission);
	}
}