package xyz.elidom.sec.rest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.dbist.dml.Filter;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sec.entity.UserHistory;
import xyz.elidom.sys.system.service.AbstractRestService;
import xyz.elidom.util.ValueUtil;

@RestController
@Transactional
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/user_histories")
@ServiceDesc(description = "UserHistory Service API")
public class UserHistoryController extends AbstractRestService {

	@Override
	protected Class<?> entityClass() {
		return UserHistory.class;
	}

	@GetMapping( produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Search (Pagination) By Search Conditions")
	public Page<?> index(
			@RequestParam(name = "page", required = false) Integer page,
			@RequestParam(name = "limit", required = false) Integer limit,
			@RequestParam(name = "select", required = false) String select,
			@RequestParam(name = "sort", required = false) String sort,
			@RequestParam(name = "query", required = false) String query) {
		Query queryObj = this.parseQuery(this.entityClass(), page, limit, select, sort, query);
		if (ValueUtil.isEmpty(sort)) {
			queryObj.addOrder("created_at", false);
		}
		return this.search(this.entityClass(), queryObj);
	}
	
	@Override
	protected <T> T beforeSearchEntities(T t) {
		Query queryObj = (Query) t;
		Boolean isIgnoreDomain = false;

		List<Filter> filters = queryObj.getFilter();
		for (Filter filter : filters) {
			if (ValueUtil.isEqual("ignore_domain", filter.getName())) {
				isIgnoreDomain = ValueUtil.toBoolean(filter.getValue());
			}
		}

		if (isIgnoreDomain) {
			queryObj.removeFilter("domainId");
			queryObj.removeFilter("ignore_domain");
		}

		return t;
	}

	@GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Find one by ID")
	public UserHistory findOne(@PathVariable("id") String id) {
		return this.getOne(this.entityClass(), id);
	}

	@GetMapping(value = "/{id}/exist", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Check exists By ID")
	public Boolean isExist(@PathVariable("id") String id) {
		return this.isExistOne(this.entityClass(), id);
	}

	@PostMapping( consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@ApiDesc(description = "Create")
	public UserHistory create(@RequestBody UserHistory input) {
		return this.createOne(input);
	}

	@PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Update")
	public UserHistory update(@PathVariable("id") String id, @RequestBody UserHistory input) {
		return this.updateOne(input);
	}

	@DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Delete")
	public void delete(@PathVariable("id") String id) {
		this.deleteOne(this.getClass(), id);
	}

	@PostMapping(value = "/update_multiple", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Create, Update or Delete multiple at one time")
	public Boolean multipleUpdate(@RequestBody List<UserHistory> list) {
		return this.cudMultipleData(this.entityClass(), list);
	}
}