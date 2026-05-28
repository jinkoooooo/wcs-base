/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sec.rest;

import java.util.List;
import java.util.UUID;

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
import xyz.elidom.sec.SecConstants;
import xyz.elidom.sec.system.auth.SecAuthProvider;
import xyz.elidom.sec.util.SecurityUtil;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.system.service.AbstractRestService;
import xyz.elidom.util.ValueUtil;

@RestController
@Transactional
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/tokens")
@ServiceDesc(description = "Token Service API")
public class TokenController extends AbstractRestService {

	@Override
	protected Class<?> entityClass() {
		return User.class;
	}

	@GetMapping( produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Search Tokens(Pagination) By Search Conditions")
	public Page<?> index(
			@RequestParam(name = "page", required = false) Integer page,
			@RequestParam(name = "limit", required = false) Integer limit,
			@RequestParam(name = "select", required = false) String select,
			@RequestParam(name = "sort", required = false) String sort,
			@RequestParam(name = "query", required = false) String query) {
		Query queryObj = this.parseQuery(this.entityClass(), page, limit, select, sort, query);
		queryObj.addFilter(new Filter("accountType", SecConstants.ACCOUNT_TYPE_TOKEN));
		return this.search(this.entityClass(), queryObj);
	}

	@GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Find one from Token by id")
	public User findOne(@PathVariable("id") String id) {
		return this.getOne(true, this.entityClass(), id);
	}

	@GetMapping(value = "/{id}/exist", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Check if Token exists by ID")
	public Boolean isExist(@PathVariable("id") String id) {
		return this.isExistOne(this.entityClass(), id);
	}

	@PostMapping( consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@ApiDesc(description = "Create Token")
	public User create(@RequestBody User token) {
		String uuid = UUID.randomUUID().toString();
		token.setLogin(uuid.substring(0, uuid.lastIndexOf(SysConstants.DASH)));
		token.setAccountType(SecConstants.AUTH_TYPE_TOKEN);
		return this.createOne(token);
	}

	@PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Update Token")
	public User update(@PathVariable("id") String id, @RequestBody User token) {
		return this.updateOne(token);
	}

	@DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Delete Token")
	public void delete(@PathVariable("id") String id) {
		this.deleteOne(this.entityClass(), id);
	}

	@PostMapping(value = "/update_multiple", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Create, Update or Delete Multiple Token at one time")
	public Boolean multipleUpdate(@RequestBody List<User> tokenList) {
		tokenList.forEach(token -> {
			if(ValueUtil.isEqual(token.getCudFlag_(), OrmConstants.CUD_FLAG_CREATE)) {
				String uuid = UUID.randomUUID().toString();
				token.setLogin(uuid.substring(0, uuid.lastIndexOf(SysConstants.DASH)));
				token.setAccountType(SecConstants.AUTH_TYPE_TOKEN);
				token.setEncryptedPassword(SecurityUtil.encodePassword(SecAuthProvider.randomPassword()));
			}
		});
		
		return this.cudMultipleData(this.entityClass(), tokenList);
	}
}