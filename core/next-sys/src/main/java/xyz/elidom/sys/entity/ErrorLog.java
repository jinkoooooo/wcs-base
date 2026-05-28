/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.entity;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Index;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.entity.basic.ElidomStampHook;
import xyz.elidom.util.FormatUtil;

@Table(name = "error_logs", idStrategy = GenerationRule.UUID, indexes = {
	@Index(name = "ix_err_log_0", columnList = "domain_id,code"),
	@Index(name = "ix_err_log_1", columnList = "domain_id,status"),
	@Index(name = "ix_err_log_2", columnList = "domain_id,error_type"),
	@Index(name = "ix_err_log_3", columnList = "domain_id,uri"),
	@Index(name = "ix_err_log_4", columnList = "domain_id,issue_date")
})
public class ErrorLog extends ElidomStampHook {
	
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = -2961910818162472263L;
	
	@PrimaryKey
	@Column(name = "id", nullable = false, length = OrmConstants.FIELD_SIZE_UUID)
	private String id;
	
	@Column(name = "code", length=OrmConstants.FIELD_SIZE_LONG_NAME)
	private String code;

	@Column(name = "message", length=OrmConstants.FIELD_SIZE_MAX_TEXT)
	private String message;

	@Column(name = "status", length=10)
	private String status;
	
	@Column(name = "error_type", length=OrmConstants.FIELD_SIZE_LONG_NAME)
	private String errorType;

	@Column(name = "uri", length=OrmConstants.FIELD_SIZE_VALUE_255)
	private String uri;

	@Column(name = "method", length=10)
	private String method;
	
	@Column(name = "header", length=OrmConstants.FIELD_SIZE_VALUE_2000)
	private String header;

	@Column(name = "params", length=OrmConstants.FIELD_SIZE_VALUE_2000)
	private String params;

	@Column(name = "stack_trace", length=OrmConstants.FIELD_SIZE_MAX_TEXT)
	private String stackTrace;
	
	@Column(name = "issue_date", length=22, nullable=false)
	private String issueDate;
		
	public ErrorLog() {
	}
	
	public ErrorLog(Long domainId, String code, String message, String status, String errorType, String uri, String method, String header, String params, String stackTrace, String issueDate) {
		this.setDomainId(domainId);
		this.setCode(code);
		this.setMessage(message);
		this.setStatus(status);
		this.setErrorType(errorType);
		this.setUri(uri);
		this.setMethod(method);
		this.setHeader(header);
		this.setParams(params);
		this.setStackTrace(stackTrace);
		this.setIssueDate(issueDate);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = FormatUtil.substr(message, 0, 3999);
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
	
	public String getErrorType() {
		return errorType;
	}

	public void setErrorType(String errorType) {
		this.errorType = errorType;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = FormatUtil.substr(uri, 0, 254);
	}
	
	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}	

	public String getHeader() {
		return header;
	}

	public void setHeader(String header) {
		this.header = FormatUtil.substr(header, 0, 1023);
	}

	public String getParams() {
		return params;
	}

	public void setParams(String params) {
		this.params = FormatUtil.substr(params, 0, 1023);
	}

	public String getStackTrace() {
		return stackTrace;
	}

	public void setStackTrace(String stackTrace) {
		this.stackTrace = FormatUtil.substr(stackTrace, 0, 3999);
	}
	
	public String getIssueDate() {
		return issueDate;
	}
	
	public void setIssueDate(String issueDate) {
		this.issueDate = issueDate;
	}
	
	/*@Override
	public void afterCreate() {
		super.afterCreate();
		
		String value = SettingUtil.getValue(this.domainId, SysConfigConstants.ERROR_MAIL_SEND_ENABLE, SysConstants.FALSE_STRING);
		if(ValueUtil.isEqualIgnoreCase(SysConstants.TRUE_STRING, value)) {
			String disableStatusList = SettingUtil.getValue(SysConfigConstants.ERROR_MAIL_SEND_DISABLE_STATUS, OrmConstants.EMPTY_STRING);
			String[] disableStatuses = StringUtils.tokenizeToStringArray(disableStatusList, OrmConstants.COMMA);
			
			if(ValueUtil.isEmpty(this.status) || ValueUtil.isEmpty(disableStatuses) || !Arrays.asList(disableStatuses).contains(String.valueOf(this.status))) {
				BeanUtil.get(ErrorLogController.class).sendErrorMail(this);
			}
		}
	}*/
	
}