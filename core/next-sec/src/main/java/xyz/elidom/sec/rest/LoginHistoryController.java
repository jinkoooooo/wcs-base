package xyz.elidom.sec.rest;

import java.util.Date;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
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

import xyz.elidom.dbist.dml.Filter;
import xyz.elidom.dbist.dml.Order;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sec.SecConfigConstants;
import xyz.elidom.sec.SecConstants;
import xyz.elidom.sec.entity.LoginHistory;
import xyz.elidom.sec.entity.UserHistory;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.system.service.AbstractRestService;
import xyz.elidom.sys.util.SessionUtil;
import xyz.elidom.sys.util.SettingUtil;
import xyz.elidom.sys.util.SysValueUtil;
import xyz.elidom.util.DateUtil;

@RestController
@Transactional
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/login_histories")
@ServiceDesc(description = "LoginHistory Service API")
public class LoginHistoryController extends AbstractRestService {

	@Override
	protected Class<?> entityClass() {
		return LoginHistory.class;
	}

	@GetMapping( produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Search (Pagination) By Search Conditions")
	public Page<?> index(@RequestParam(name = "page", required = false) Integer page,
			@RequestParam(name = "limit", required = false) Integer limit,
			@RequestParam(name = "select", required = false) String select,
			@RequestParam(name = "sort", required = false) String sort,
			@RequestParam(name = "query", required = false) String query,
			@RequestParam(name = "ignore_domain", required = false) Boolean ignoreDomain) {
		Query queryObj = this.parseQuery(this.entityClass(), page, limit, select, sort, query);
		if (SysValueUtil.isEmpty(sort)) {
			queryObj.addOrder("created_at", false);
		}

		return this.search(this.entityClass(), queryObj);
	}
	
	
	@GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "User Login Histories")
	public Page<?> getHistories(@RequestParam(name = "page", required = false) Integer page,
			@RequestParam(name = "limit", required = false) Integer limit,
			@RequestParam(name = "select", required = false) String select,
			@RequestParam(name = "sort", required = false) String sort,
			@RequestParam(name = "query", required = false) String query) {
		Query queryObj = this.parseQuery(this.entityClass(), page, limit, select, sort, query);
		if (SysValueUtil.isEmpty(sort)) {
			queryObj.addOrder("created_at", false);
		}
		queryObj.addFilter("access_user_id", User.currentUser().getId());
		return this.search(this.entityClass(), queryObj);
	}

	

	@Override
	protected <T> T beforeSearchEntities(T t) {
		Query queryObj = (Query) t;
		Boolean isIgnoreDomain = false;

		List<Filter> filters = queryObj.getFilter();
		for (Filter filter : filters) {
			if (SysValueUtil.isEqual("ignore_domain", filter.getName())) {
				isIgnoreDomain = SysValueUtil.toBoolean(filter.getValue());
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
	public LoginHistory findOne(@PathVariable("id") String id) {
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
	public LoginHistory create(@RequestBody LoginHistory input) {
		return this.createOne(input);
	}

	@PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Update")
	public LoginHistory update(@PathVariable("id") String id, @RequestBody LoginHistory input) {
		return this.updateOne(input);
	}

	@DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Delete")
	public void delete(@PathVariable("id") String id) {
		this.deleteOne(this.getClass(), id);
	}

	@PostMapping(value = "/update_multiple", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Create, Update or Delete multiple at one time")
	public Boolean multipleUpdate(@RequestBody List<LoginHistory> list) {
		return this.cudMultipleData(this.entityClass(), list);
	}

	/**
	 * 로그인 정보 이력 생성.
	 * 
	 * @param domainId
	 * @param userId
	 * @param isSuccess
	 * @param ipAddress
	 */
	public void saveLoginSuccessHistory(long domainId, String userId, String ipAddress) {
		this.saveLoginHistory(domainId, userId, true, ipAddress);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void saveLoginFailHistory(long domainId, String userId, String ipAddress) {
		this.saveLoginHistory(domainId, userId, false, ipAddress);
	}

	private void saveLoginHistory(long domainId, String userId, boolean isSuccess, String ipAddress) {
		try {
			LoginHistory loginHistory = new LoginHistory();
			loginHistory.setDomainId(domainId);
			loginHistory.setAccessUserId(userId);
			loginHistory.setSuccess(isSuccess);
			loginHistory.setAccessIp(ipAddress);
			if (isSuccess)
				loginHistory.setSessionId(SessionUtil.getSessionId());

			queryManager.insert(loginHistory);
		} catch (Exception e) {
			// logger.warn(e.getMessage(), e);
		}
	}

	/**
	 * 로그 아웃 정보 업데이트.
	 */
	public void updateLogOutInfo() {
		try {
			User user = User.currentUser();
			if (SysValueUtil.isEmpty(user))
				return;

			String sessionId = SessionUtil.getSessionId();

			LoginHistory history = new LoginHistory();
			history.setDomainId(Domain.currentDomainId());
			history.setAccessUserId(user.getId());
			history.setSessionId(sessionId);

			history = queryManager.selectByCondition(LoginHistory.class, history);
			history.setLogoutAt(new Date());

			queryManager.update(history);
		} catch (Exception e) {
			// logger.warn(e.getMessage(), e);
		}
	}

	/**
	 * 마지막 로그인 정보 가져오기 실행.
	 * 
	 * @param userId
	 * @return
	 */
	public LoginHistory getLastLoginInfo(String userId) {
		try {
			Query queryObj = new Query();
			queryObj.setPageIndex(1);
			queryObj.setPageSize(1);
			queryObj.addOrder(new Order("created_at", false));
			queryObj.addFilter(new Filter(SysConstants.ENTITY_FIELD_DOMAIN_ID, Domain.currentDomain().getId()));
			queryObj.addFilter(new Filter("access_user_id", userId));

			List<LoginHistory> history = queryManager.selectList(LoginHistory.class, queryObj);
			return history.size() > 0 ? history.get(0) : new LoginHistory();
		} catch (Exception e) {
			// logger.warn(e.getMessage(), e);
			return null;
		}
	}

	/**
	 * 중복 로그인 실패로 인한 계정 잠금 대상 여부 확인.
	 * 
	 * @param userId
	 * @return
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void doLoginFailLock(User user) {
		try {
			int settingLockCount = SysValueUtil.toInteger(SettingUtil.getValue(SecConfigConstants.LOGIN_FAIL_LOCK_COUNT),
					0);
			if (settingLockCount < 1)
				return;

			Query queryObj = new Query();
			queryObj.setPageIndex(1);
			queryObj.setPageSize(settingLockCount);
			queryObj.addOrder(new Order("created_at", false));
			queryObj.addFilter(new Filter(SysConstants.ENTITY_FIELD_DOMAIN_ID, Domain.currentDomain().getId()));
			queryObj.addFilter(new Filter("access_user_id", user.getId()));

			List<LoginHistory> histories = queryManager.selectList(LoginHistory.class, queryObj);
			if (SysValueUtil.isEmpty(histories) || histories.size() < settingLockCount)
				return;

			// 해당 횟수 내에 한번이라도 정상 로그인을 하였다면 계정 잠금을 실행하지 않음.
			for (LoginHistory history : histories) {
				if (history.getSuccess()) {
					return;
				}
			}

			// Login Fail Lock 대상자일 경우, 사용자 계정 만료 날짜 업데이트.
			user.setAccountExpireDate(DateUtil.currentTimeStr());
			queryManager.update(user);

			// 사용자 계정 상태 변경에 대한 이력 생성.
			queryManager.insert(new UserHistory(user.getId(), SecConstants.USER_PASSWORD_LOCK));
		} catch (Exception e) {
			// logger.warn(e.getMessage(), e);
		}
	}
}