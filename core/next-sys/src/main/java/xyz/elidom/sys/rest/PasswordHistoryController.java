package xyz.elidom.sys.rest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.dbist.dml.Filter;
import xyz.elidom.dbist.dml.Order;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.exception.server.ElidomServiceException;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sys.SysConfigConstants;
import xyz.elidom.sys.SysMessageConstants;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.entity.PasswordHistory;
import xyz.elidom.sys.system.service.AbstractRestService;
import xyz.elidom.sys.util.DateUtil;
import xyz.elidom.sys.util.SettingUtil;
import xyz.elidom.util.ValueUtil;

@RestController
@Transactional
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/password_histories")
@ServiceDesc(description = "PasswordHistory Service API")
public class PasswordHistoryController extends AbstractRestService {

	@Override
	protected Class<?> entityClass() {
		return PasswordHistory.class;
	}

	@GetMapping( produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Search (Pagination) By Search Conditions")
	public Page<?> index(@RequestParam(name = "page", required = false) Integer page, 
			@RequestParam(name = "limit", required = false) Integer limit,
			@RequestParam(name = "select", required = false) String select, 
			@RequestParam(name = "sort", required = false) String sort,
			@RequestParam(name = "query", required = false) String query) {
		return this.search(this.entityClass(), page, limit, select, sort, query);
	}

	@GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Find one by ID")
	public PasswordHistory findOne(@PathVariable("id") String id) {
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
	public PasswordHistory create(@RequestBody PasswordHistory input) {
		return this.createOne(input);
	}

	@PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Update")
	public PasswordHistory update(@PathVariable("id") String id, @RequestBody PasswordHistory input) {
		return this.updateOne(input);
	}

	@DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Delete")
	public void delete(@PathVariable("id") String id) {
		this.deleteOne(this.getClass(), id);
	}

	@PostMapping(value = "/update_multiple", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Create, Update or Delete multiple at one time")
	public Boolean multipleUpdate(@RequestBody List<PasswordHistory> list) {
		return this.cudMultipleData(this.entityClass(), list);
	}
	
	/**
	 * 비밀번호 변경 전 유효성 검사.
	 * 
	 * @param userId
	 * @param pass
	 * @return
	 */
	public boolean validationCheck(String userId, String pass) {
		long domainId = Domain.currentDomainId();

		// 최근 사용 여부 확인.
		int recentUseCount = ValueUtil.toInteger(SettingUtil.getValue(SysConfigConstants.USER_RECENT_USED_PASS_COUNT), 0);
		if (recentUseCount > 0) {
			Query query = new Query();
			query.addFilter(new Filter("domain_id", domainId));
			query.addFilter(new Filter("user_id", userId));
			query.addOrder(new Order("created_at", false));
			query.setPageSize(recentUseCount);

			List<PasswordHistory> list = queryManager.selectList(PasswordHistory.class, query);
			if (ValueUtil.isNotEmpty(list)) {
				for (PasswordHistory history : list) {
					if (ValueUtil.isEqual(history.getName(), pass))
						throw new ElidomServiceException(SysMessageConstants.USER_ALREADY_USED_PASS, "Password has been used already. Please enter another password.");
				}
			}
		}

		// 당일 변경 횟수.
		int limitModifiedCount = ValueUtil.toInteger(SettingUtil.getValue(SysConfigConstants.USER_PASS_MODIFY_COUNT), 0);
		if (limitModifiedCount > 0) {
			final String DATE_FORMAT = "yyyy-MM-dd";

			try {
				Date fromDate = new SimpleDateFormat(DATE_FORMAT).parse(DateUtil.todayStr());
				Date toDate = new SimpleDateFormat(DATE_FORMAT).parse(DateUtil.addDateToStr(new Date(), 1));

				StringJoiner sql = new StringJoiner("\n");
				sql.add("SELECT COUNT(ID) FROM PASSWORD_HISTORIES ");
				sql.add("WHERE DOMAIN_ID = :domain_id ");
				sql.add("AND USER_ID = :user_id");
				sql.add("AND CREATED_AT BETWEEN :fromDate AND :toDate");

				Map<String, Object> paramMap = new HashMap<String, Object>();
				paramMap.put("domain_id", domainId);
				paramMap.put("user_id", userId);
				paramMap.put("fromDate", fromDate);
				paramMap.put("toDate", toDate);

				Integer modifiedCount = queryManager.selectBySql(sql.toString(), paramMap, Integer.class);
				if (modifiedCount >= limitModifiedCount)
					throw new ElidomServiceException(SysMessageConstants.USER_PASS_MODIFY_EXCEED_COUNT, "Password Change count exceed. Please try again tomorrow.");

			} catch (ParseException e) {
				throw new ElidomServiceException(e);
			}
		}

		return true;
	}
}