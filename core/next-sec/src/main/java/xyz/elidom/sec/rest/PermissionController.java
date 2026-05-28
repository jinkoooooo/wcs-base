/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sec.rest;

import java.util.List;
import java.util.Map;

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

import xyz.elidom.dbist.dml.Page;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sec.entity.Permission;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.system.service.AbstractRestService;
import xyz.elidom.sys.util.SysValueUtil;

@RestController
@Transactional
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/permissions")
@ServiceDesc(description="Permission Service API")
public class PermissionController extends AbstractRestService {

	/**
	 * Key ALL - all
	 */
	private static final String ALL_KEY = "all";
	/**
	 * Menu Permission of User SQL Parameters Key - userId,menuId
	 */
	private static final String USER_MENU_PERMISSION_PARAMS_KEY = "userId,menuId";
	/**
	 * Menu Permission of User SQL
	 */
	private String USER_MENU_PERMISSION_SQL = "select action_name from permissions where role_id in (select role_id from users_roles where user_id = :userId) and resource_type = 'Menu' and resource_id = :menuId";

	@Override
	protected Class<?> entityClass() {
		return Permission.class;
	}

	public void setPermissionAuthQuery(String sql) {
		USER_MENU_PERMISSION_SQL = sql;
	}

	public String getPermissionAuthQuery() {
		return USER_MENU_PERMISSION_SQL;
	}

	@GetMapping( produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Search Permission (Pagination) By Search Conditions")
	public Page<?> index(
			@RequestParam(name = "page", required = false) Integer page,
			@RequestParam(name = "limit", required = false) Integer limit,
			@RequestParam(name = "select", required = false) String select,
			@RequestParam(name = "sort", required = false) String sort,
			@RequestParam(name = "query", required = false) String query) {
		return this.search(this.entityClass(), page, limit, select, sort, query);
	}

	@GetMapping(value="/{user_id}/{menu_id}", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Find menu permissions by User ID")
	public List<String> findMenuPermissionsByUser(@PathVariable("user_id") String userId, @PathVariable("menu_id") String menuId) {

		// 1. 먼저 사용자의 권한이 superuser 이거나 해당 도메인의 admin이면 full 권한
		if(User.isCurrentUserAdmin()) {
			return SysValueUtil.newStringList(ALL_KEY);

			// 2. full 권한이 없다면 해당 메뉴의 권한 체크
		} else {
			Map<String, Object> paramsMap = SysValueUtil.newMap(USER_MENU_PERMISSION_PARAMS_KEY, userId, menuId);
			return this.queryManager.selectListBySql(this.getPermissionAuthQuery(), paramsMap, String.class, 0, 0);
		}
	}

	@GetMapping(value="/{id}", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Find permission by ID")
	public Permission findOne(@PathVariable("id") String id) {
		return this.getOne(true, this.entityClass(), id);
	}

	@GetMapping(value="/{id}/exist", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Check if permissions exists by ID")
	public Boolean isExist(@PathVariable("id") String id) {
		return this.isExistOne(this.entityClass(), id);
	}

	@PostMapping( consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@ApiDesc(description="Create Permission")
	public Permission create(@RequestBody Permission permission) {
		return this.createOne(permission);
	}

	@PutMapping(value="/{id}", consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Update Permission")
	public Permission update(@PathVariable("id") String id, @RequestBody Permission permission) {
		return this.updateOne(permission);
	}

	@DeleteMapping(value="/{id}", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Delete Permission By ID")
	public void delete(@PathVariable("id") String id) {
		this.deleteOne(this.entityClass(), id);
	}

	@PostMapping(value="/update_multiple", consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Create, Update or Delete multiple Permissions at one time")
	public Boolean multipleUpdate(@RequestBody List<Permission> permissionList) {
		return this.cudMultipleData(this.entityClass(), permissionList);
	}
}