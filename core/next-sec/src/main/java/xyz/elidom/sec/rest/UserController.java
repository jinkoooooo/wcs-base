/* Copyright © HatioLab Inc. All rights reserved. */
package xyz.elidom.sec.rest;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
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
import xyz.elidom.exception.client.ElidomBadRequestException;
import xyz.elidom.exception.client.ElidomRecordNotFoundException;
import xyz.elidom.exception.server.ElidomServiceException;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sec.util.SecurityUtil;
import xyz.elidom.sys.SysConfigConstants;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.SysMessageConstants;
import xyz.elidom.sys.entity.PasswordHistory;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.rest.PasswordHistoryController;
import xyz.elidom.sys.system.auth.model.CheckPassword;
import xyz.elidom.sys.system.engine.ITemplateEngine;
import xyz.elidom.sys.system.service.AbstractRestService;
import xyz.elidom.sys.system.transport.sender.MailSender;
import xyz.elidom.sys.util.FileUtil;
import xyz.elidom.sys.util.MessageUtil;
import xyz.elidom.sys.util.SessionUtil;
import xyz.elidom.sys.util.SettingUtil;
import xyz.elidom.sys.util.SysValueUtil;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.DateUtil;

/**
 * UserController
 * 
 * @author shortstop
 */
@RestController
@Transactional
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/users")
@ServiceDesc(description = "User Service API")
public class UserController extends AbstractRestService {
	
	/**
	 * Logger
	 */
	private Logger logger = LoggerFactory.getLogger(UserController.class);
	
	@Autowired
	@Qualifier("basic")
    private ITemplateEngine templateEngine;
	
	@Autowired
	private MailSender mailSender;
	
	
	@Override
	protected Class<?> entityClass() {
		return User.class;
	}

	@GetMapping( produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Search User (Pagination) By Search Conditions")
	public Page<?> index(
			@RequestParam(name = "page", required = false) Integer page, 
			@RequestParam(name = "limit", required = false) Integer limit,
			@RequestParam(name = "select", required = false) String select, 
			@RequestParam(name = "sort", required = false) String sort,
			@RequestParam(name = "query", required = false) String query) {
		
		Query queryObj = this.parseQuery(this.entityClass(), page, limit, select, sort, query);
		queryObj.addFilter(new Filter("accountType", "noteq", SysConstants.ACCOUNT_TYPE_TOKEN));
		return this.search(this.entityClass(), queryObj);
	}
	
	@Override
	protected boolean isDomainBased(Class<?> clazz) {
		return false;
	}
	
	@Override
	public Page<?> search(Class<?> entityClass, Integer page, Integer limit, String select, String sort, String query) {
		Query input = new Query();
		input.setPageIndex(page == null ? 1 : page.intValue());
		limit = (limit == null) ? 50 : limit.intValue();
		input.setPageSize(limit);
		String[] selectFields = SysValueUtil.isEmpty(select) ? null : select.split(SysConstants.COMMA);

		if (!SysValueUtil.isEmpty(selectFields)) {
			List<String> selectColumns = new ArrayList<String>();
			for (int i = 0; i < selectFields.length; i++) {
				selectColumns.add(selectFields[i]);
			}
			
			input.setSelect(selectColumns);
		}

		if (SysValueUtil.isNotEmpty(sort)) {
			Order[] orders = this.jsonParser.parse(sort, Order[].class);
			input.addOrder(orders);
		}

		if (SysValueUtil.isNotEmpty(query)) {
			Filter[] filters = this.jsonParser.parse(query, Filter[].class);
			input.addFilter(filters);
		}

		return this.search(entityClass, input);
	}

	@GetMapping(value = "/{id:.+}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Find User By ID")
	public User findOne(@PathVariable("id") String id) {
		return this.getOne(true, this.entityClass(), id);
	}

	@GetMapping(value = "/exist/{id:.+}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Check if Users exists By ID")
	public Boolean isExist(@PathVariable("id") String id) {
		return this.isExistOne(this.entityClass(), id);
	}
	
	@PostMapping(value = "/check_import", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Check Before Import")
	public List<User> checkImport(@RequestBody List<User> list) {
		for (User item : list) {
			this.checkForImport(User.class, item);
		}
		
		return list;
	}

	@PostMapping( consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@ApiDesc(description = "Create User")
	public User create(@RequestBody User user) {
		return this.createOne(user);
	}
	
	@PostMapping(value = "/create", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@ApiDesc(description = "Create User")
	public User createUser(@RequestBody User user) {
		String pass = "";
		
		try {
			pass = new String(Base64.decodeBase64(user.getPassword()), "UTF-8");
			pass = SecurityUtil.encodePassword(pass);
			user.setPassword(null);
			user.setEncryptedPassword(pass);
			user.setAccountType(SysConstants.ACCOUNT_TYPE_USER);
			
		} catch(Exception e) {
			throw new ElidomServiceException(e);
		}		
		
		return this.createOne(user);
	}
	

	@PutMapping(value = "/{id:.+}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Update User")
	public User update(@PathVariable("id") String id, @RequestBody User user) {
		return this.updateOne(user);
	}

	@DeleteMapping(value = "/{id:.+}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Delete User By ID")
	public void delete(@PathVariable("id") String id) {
		this.deleteOne(this.entityClass(), id);
	}

	@PostMapping(value = "/update_multiple", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Create, Update or Delete multiple Users at one time")
	public Boolean multipleUpdate(@RequestBody List<User> userList) {
		return this.cudMultipleData(this.entityClass(), userList);
	}
	
	@GetMapping(value = "/{id:.+}/check_password_expired", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Check if Users exists By ID")
	public Map<String, Object> checkPasswordExpired(@PathVariable("id") String id) {
		User user = this.getOne(true, this.entityClass(), id);
		Map<String, Object> result = SysValueUtil.newMap("password_expired", false);
		
		boolean isEnable = SysValueUtil.toBoolean(SettingUtil.getValue(SysConfigConstants.USER_PASSWORD_EXPIRE_ENABLE), false);
		result.put("user_password_expired", isEnable);
		
		if (SysValueUtil.isNotEqual(user.getAccountType(), SysConstants.ACCOUNT_TYPE_USER)) {
			return result;
		}
		
		if(isEnable) {
			String todayStr = DateUtil.todayStr();
			String pwExpiredDate = user.getPasswordExpireDate();
			if(pwExpiredDate == null) {
				pwExpiredDate = todayStr;
			}
			
			result.put("password_expired_date", pwExpiredDate);
			if(todayStr.compareTo(pwExpiredDate) > 0) {
				result.put("password_expired", true);
			}
		}
		
		return result;
	}
	
	@PutMapping(value = "/{id:.+}/release_lock", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Release Account Locked")
	public User releaseLock(@PathVariable("id") String id) {
		User user = this.queryManager.select(User.class, id);
		if(user == null) {
			throw new ElidomRecordNotFoundException(SysMessageConstants.USER_NOT_EXIST, "User does not exist");
		}
		
		if(user.getAccountExpireDate() != null) {
			user.setAccountExpireDate(null);
		}
		
		user.setStatus("activated");
		user.setActiveFlag(true);
		user.setFailCount(0);
		this.queryManager.update(user, "accountExpireDate", "status", "activeFlag", "failCount");
		return user;
	}
	
	@PutMapping(value = "/{id:.+}/lock", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Lock Account")
	public User lockAccount(@PathVariable("id") String id) {
		User user = this.queryManager.select(User.class, id);
		if(user == null) {
			throw new ElidomRecordNotFoundException(SysMessageConstants.USER_NOT_EXIST, "User does not exist");
		}
		
		user.setStatus("locked");
		user.setActiveFlag(false);
		this.queryManager.update(user, "status", "activeFlag");
		return user;
	}
	
	@PostMapping(value = "/change_pass/{id:.+}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Change Password.")
	public boolean changePass(@PathVariable("id") String id, @RequestBody CheckPassword checkPass) {
		User user = this.findOne(id);
		if (SysValueUtil.isEmpty(user)) {
			throw new ElidomRecordNotFoundException(SysMessageConstants.USER_NOT_EXIST, "User does not exist");
		}
		
		String newPass = "";
		String currentPass = "";
		
		try {
			newPass = new String(Base64.decodeBase64(checkPass.getNewPass()), "UTF-8");
			if(SysValueUtil.isNotEmpty(checkPass.getCurrentPass())) {
				currentPass = new String(Base64.decodeBase64(checkPass.getCurrentPass()), "UTF-8");	
			}
			
		} catch(Exception e) {
			throw new ElidomServiceException(e);
		}
		String encryptPass = SecurityUtil.encodePasswordV2(newPass);
		
		if(SysValueUtil.isNotEmpty(currentPass)) {
			String encCurrentPass = SecurityUtil.encodePassword(currentPass);
			
			if(SysValueUtil.isNotEqual(user.getEncryptedPassword(), encCurrentPass)) {
				throw new ElidomServiceException(MessageUtil.getTerm("terms.text.password_mismatch"));
			}
		}
		
		/**
		 * 최근 사용한 비밀번호 여부와 변경 횟수 검사.
		 */
		PasswordHistoryController passwordHistoryController = BeanUtil.get(PasswordHistoryController.class);
		passwordHistoryController.validationCheck(id, encryptPass);

		/**
		 * 비밀번호 변경 시, Expire Date를 설정한 기간만큼 연장.
		 */
		boolean isEnable = SysValueUtil.toBoolean(SettingUtil.getValue(SysConfigConstants.USER_PASSWORD_EXPIRE_ENABLE), false);
		if(isEnable) {
			String period = SettingUtil.getValue(SysConfigConstants.USER_PASSWORD_CHANGE_PERIOD_DAY, "90");
			String parseDate = DateUtil.addDateToStr(new Date(), SysValueUtil.toInteger(period));
			user.setPasswordExpireDate(parseDate);
		}else {
			user.setPasswordExpireDate(null);
		}
		
		SessionUtil.removeAttribute(SysConstants.ACCOUNT_STATUS);

		user.setPasswordUpdatedAt(new Date());
		user.setEncryptedPassword(encryptPass);
		user.setResetPasswordToken("");
		
		/**
		 * Node 프레임웍과 페스워드 동기화를 위해 해당 필드에 패스워드 저장 
		 */
		try {
			// 시크릿 키 생성 
			SecretKeySpec secretKey = new SecretKeySpec(user.getSalt().getBytes("utf-8"), "HmacSHA256");

			// 지정된  MAC 알고리즘을 구현하는 Mac 객체를 작성합니다.
			Mac hasher = Mac.getInstance("HmacSHA256");
					
			// 키를 사용해 이 Mac 객체를 초기화
			hasher.init(secretKey);
			
			// 암호화 하려는 데이터의 바이트의 배열을 처리해 MAC 조작을 종료
			byte[] hash = hasher.doFinal(newPass.getBytes());
					
			// Base 64 Encode to String
			user.setPassword(Base64.encodeBase64String(hash));
			
		} catch(Exception e) {
			throw new ElidomServiceException(e);
		}

		this.updateOne(user);

		/**
		 * 비밀번호 변경 이력 생성.
		 */
		passwordHistoryController.create(new PasswordHistory(encryptPass, id));
		return true;
	}
	
	@PostMapping(value = "/change_pass_later/{id:.+}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Change Password Later.")
	public boolean changePassLater(@PathVariable("id") String id) {
		String laterDay = SettingUtil.getValue(SysConfigConstants.USER_PASSWORD_CHANGE_LATER_DAY, "30");
		String parseDate = DateUtil.addDateToStr(new Date(), SysValueUtil.toInteger(laterDay));

		User user = this.findOne(id);
		user.setPasswordExpireDate(parseDate);
		this.updateOne(user);

		SessionUtil.removeAttribute(SysConstants.ACCOUNT_STATUS);
		return true;
	}
	
	@GetMapping(value = "/active/{id:.+}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Activate account")
	public String activeAccount(@PathVariable("id") String id) {
		User account = this.getOne(true, this.entityClass(), id);
		
		if (account.getActiveFlag() && SysValueUtil.isEqualIgnoreCase(account.getStatus(), "activated")) {
			throw new ElidomBadRequestException(SysMessageConstants.USER_ALREADY_ACTIVATED, "Already activated account");
		}
		
		this.releaseLock(id);
		
		String title = MessageUtil.getMessage(SysMessageConstants.USER_COMPLETE_ACTIVE_ACCOUNT, "Your account is activated!");
		String loginLink = SettingUtil.getValue(SysConfigConstants.CLIENT_CONTEXT_PATH, "http://factory.hatiolab.com");
		Map<String, Object> templateParams = SysValueUtil.newMap("loginLink,title,systemName,domain,email,userId,userName", loginLink, title, account.getDomain().getBrandName(), account.getDomain().getBrandName(), account.getEmail(), account.getId(), account.getName());
		UserController ctrl = BeanUtil.get(UserController.class);
		ctrl.sendMailToRequester(SysConfigConstants.MAIL_TEMPLATE_ACCOUNT_ACTIVATION_APPROVED, templateParams);
		return MessageUtil.getMessage(SysMessageConstants.RESULT_SENT_TO_REQUESTER, "Results of processing your request has been sent.");
	}
	
	@GetMapping(value = "/inactive/{id:.+}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Deactivate account")
	public String inactiveAccount(@PathVariable("id") String id) {
		User account = this.getOne(true, this.entityClass(), id);
		
		if(!account.getActiveFlag() && SysValueUtil.isEqualIgnoreCase(account.getStatus(), "locked")) {
			throw new ElidomBadRequestException(SysMessageConstants.USER_INACTIVATED_ACCOUNT, "Deactivated account.");
		}
				
		account.setActiveFlag(false);
		account.setStatus("locked");
		this.queryManager.update(account, "activeFlag", "status");
		
		String title = MessageUtil.getMessage(SysMessageConstants.USER_INACTIVE_ACCOUNT, "Your account is deactivated");
		String loginLink = SettingUtil.getValue(SysConfigConstants.CLIENT_CONTEXT_PATH, "http://factory.hatiolab.com");
		Map<String, Object> templateParams = SysValueUtil.newMap("loginLink,title,systemName,domain,email,userId,userName", loginLink, title, account.getDomain().getBrandName(), account.getDomain().getBrandName(), account.getEmail(), account.getId(), account.getName());
		UserController ctrl = BeanUtil.get(UserController.class);
		ctrl.sendMailToRequester(SysConfigConstants.MAIL_TEMPLATE_ACCOUNT_INACTIVATION_RESULT, templateParams);
		return MessageUtil.getMessage(SysMessageConstants.RESULT_SENT_TO_REQUESTER, "Results of processing your request has been sent.");
	}
	
	/**
	 * Send mail to requester
	 * 
	 * @param templatePath
	 * @param templateParams
	 */
	@Async
	public void sendMailToRequester(String templatePath, Map<String, Object> templateParams) {
		templateParams.put("processedAt", DateUtil.currentTimeStr());
		String title = (String)templateParams.get("title");
		String to = (String)templateParams.get("email");
		String content = this.convertTemplate(templatePath, templateParams);
		this.logger.info(content);
		this.mailSender.send(title, null, to, content, templateParams, SysValueUtil.newMap("mimeType", "text/html"));
	}
	
	/**
	 * translate template
	 * 
	 * @param templatePath
	 * @param templateParams
	 * @return
	 */
	private String convertTemplate(String templatePath, Map<String, Object> templateParams) {
		templatePath = this.makeTemplatePath(templatePath);
		String template = FileUtil.readClassPathResource(templatePath);
		StringWriter writer = new StringWriter();
		this.templateEngine.processTemplate(template, writer, templateParams, null);
		return writer.toString();
	}
	
	/**
	 * 메일 templatePath를 완성하여 리턴 
	 * 
	 * @param templatePath
	 * @return
	 */
	private String makeTemplatePath(String templatePath) {
		templatePath = SysConstants.MAIL_TEMPLATE_PATH_PREFIX + templatePath; 
		templatePath = templatePath.replace(OrmConstants.DOT, OrmConstants.SLASH);
		templatePath += SysConstants.MAIL_TEMPLATE_PATH_SUFFIX;
		return templatePath;
	}
}