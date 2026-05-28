/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.rest;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.dbist.dml.Page;
import xyz.elidom.dbist.util.StringJoiner;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;
import xyz.elidom.sys.SysConfigConstants;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.entity.ErrorLog;
import xyz.elidom.sys.system.engine.ITemplateEngine;
import xyz.elidom.sys.system.service.AbstractRestService;
import xyz.elidom.sys.system.transport.sender.MailSender;
import xyz.elidom.sys.util.FileUtil;
import xyz.elidom.sys.util.SettingUtil;
import xyz.elidom.sys.util.SysValueUtil;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.DateUtil;

@RestController
@Transactional
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/error_logs")
@ServiceDesc(description = "ErrorLog Service API")
public class ErrorLogController extends AbstractRestService {
	@Override
	protected Class<?> entityClass() {
		return ErrorLog.class;
	}

	@GetMapping( produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Search ErrorLog (Pagination) By Search Conditions")
	public Page<?> index(
			@RequestParam(name = "page", required = false) Integer page,
			@RequestParam(name = "limit", required = false) Integer limit,
			@RequestParam(name = "select", required = false) String select,
			@RequestParam(name = "sort", required = false) String sort,
			@RequestParam(name = "query", required = false) String query) {
		return this.search(this.entityClass(), page, limit, select, sort, query);
	}

	@GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Finde one ErrorLog by ID")
	public ErrorLog findOne(@PathVariable("id") String id) {
		return this.getOne(this.entityClass(), id);
	}

	@GetMapping(value = "/{id}/exist", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Check if ErrorLog exists By ID")
	public Boolean isExist(@PathVariable("id") String id) {
		return this.isExistOne(this.entityClass(), id);
	}

	@PostMapping( consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@ApiDesc(description = "Create ErrorLog")
	public ErrorLog create(ErrorLog input) {
		return this.createOne(input);
	}

	@PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Update ErrorLog")
	public ErrorLog update(@PathVariable("id") String id, ErrorLog input) {
		return this.updateOne(input);
	}
	
	@DeleteMapping(value="/{id}", produces=MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description="Delete ErrorLog")
	public void delete(@PathVariable("id") String id) {
		this.deleteOne(this.entityClass(), id);
	}

	@PostMapping(value = "/update_multiple", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiDesc(description = "Create, Update or Delete multiple ErrorLog at one time")
	public Boolean multipleUpdate(@RequestBody List<ErrorLog> errorLogList) {
		return this.cudMultipleData(this.entityClass(), errorLogList);
	}
	
	@Autowired
	private MailSender mailSender;
	
	@Autowired
	@Qualifier("basic")
    private ITemplateEngine templateEngine;
	
	@Async
	public Boolean sendErrorMail(ErrorLog errorLog) {
		String templatePath = "templates/mail/template/error/error.vm";
		Domain domain = SysValueUtil.isEmpty(errorLog.getDomainId()) ? Domain.systemDomain() : BeanUtil.get(DomainController.class).findOne(errorLog.getDomainId(), null);
		String title = errorLog.getErrorType();
		String link = SettingUtil.getValue(SysConfigConstants.CLIENT_CONTEXT_PATH);
		if(SysValueUtil.isNotEmpty(link)) {
			link += "/#!/error_logs";
		}
		
		Map<String, Object> templateParams = SysValueUtil.newMap("link,title,errorLog,createdAt", link, title, errorLog, DateUtil.currentTimeStr());
		String template = FileUtil.readClassPathResource(templatePath);
		StringWriter writer = new StringWriter();
		this.templateEngine.processTemplate(template, writer, templateParams, null);
		String content = writer.toString();		
		String to = this.getAdminEmail(domain);
		this.mailSender.send(title, null, to, content, templateParams, SysValueUtil.newMap(SysConstants.EMAIL_OPT_MIME_TYPE, SysConstants.EMAIL_MIME_TYPE_TEXT_HTML_UTF_8));		
		return true;
	}
	
	private String getAdminEmail(Domain domain) {
		String sql = "select email from users where domain_id = :domainId and admin_flag = :adminFlag";
		List<String> emailList = this.queryManager.selectListBySql(sql, SysValueUtil.newMap("domainId,adminFlag", domain.getId(), true), String.class, 0, 0);
		StringJoiner joiner = new StringJoiner(OrmConstants.COMMA);
		for(String email : emailList) {
			joiner.add(email);
		}
		
		return joiner.length() > 0 ? joiner.toString() : null;
	}
	
}