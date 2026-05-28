/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sec.rest;

import java.util.List;

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
import xyz.elidom.sec.entity.UsersRole;
import xyz.elidom.sys.system.service.AbstractRestService;

@RestController
@Transactional
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/users_roles")
@ServiceDesc(description="Role Service API")
public class UsersRoleController extends AbstractRestService {

	@Override
	protected Class<?> entityClass() {
		return UsersRole.class;
	}

	@GetMapping( produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Search User Role Relations (Pagination) By Search Conditions")
	public Page<?> index(
			@RequestParam(name = "page", required = false) Integer page,
			@RequestParam(name = "limit", required = false) Integer limit,
			@RequestParam(name = "select", required = false) String select,
			@RequestParam(name = "sort", required = false) String sort,
			@RequestParam(name = "query", required = false) String query) {
		return this.search(this.entityClass(), page, limit, null, sort, query);
	}

	@GetMapping(value="/{id}", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Find one from UsersRole by id")
	public UsersRole findOne(@PathVariable("id") String id) {
		return this.getOne(true, this.entityClass(), id);
	}

	@GetMapping(value="/{id}/exist", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Check if UsersRole exists by ID")
	public Boolean isExist(@PathVariable("id") String id) {
		return this.isExistOne(this.entityClass(), id);
	}

	@PostMapping( consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@ApiDesc(description="Create UsersRole")
	public UsersRole create(@RequestBody UsersRole usersRole) {
		return this.createOne(usersRole);
	}

	@PutMapping(value="/{id}", consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Update UsersRole")
	public UsersRole update(@PathVariable("id") String id, @RequestBody UsersRole usersRole) {
		return this.updateOne(usersRole);
	}

	@DeleteMapping(value="/{id}", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Delete UsersRole")
	public void delete(@PathVariable("id") String id) {
		this.deleteOne(this.entityClass(), id);
	}

	@PostMapping(value="/update_multiple", consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Create, Update or Delete Multiple UsersRole at one time")
	public Boolean multipleUpdate(@RequestBody List<UsersRole> usersRoleList) {
		return this.cudMultipleData(this.entityClass(), usersRoleList);
	} 
}