/* Copyright © Nearsolution Inc. All rights reserved. */
/**
 * 
 */
package xyz.elidom.sys;

import xyz.elidom.orm.OrmConfigConstants;

/**
 * Sys 모듈의 설정에 필요한 상수 정의
 * 
 * @author Minu.Kim
 */
public class SysConfigConstants extends OrmConfigConstants {
	
	/********************************************************************************
	 *							Settings Table 설정
	 ********************************************************************************/
	/**
	 * 비밀번호 만료 설정 여부
	 */
	public static final String USER_PASSWORD_EXPIRE_ENABLE = "user.password.expire.enable";
	
	/**
	 * 비밀번호 설정에 대한 변경 주기(Day)
	 */
	public static final String USER_PASSWORD_CHANGE_PERIOD_DAY = "user.password.change.period.day";
	
	/**
	 * 비밀번호 설정 연장 일 수(Day)
	 */
	public static final String USER_PASSWORD_CHANGE_LATER_DAY = "user.password.change.later.day";
	
	public static final String USER_RECENT_USED_PASS_COUNT = "user.recent.used.pass.count";
	
	public static final String USER_PASS_MODIFY_COUNT = "user.pass.modify.count";
	
	/**
	 * 기본 로케일 - locale.default
	 */
	public static final String DEFAULT_LOCALE = "locale.default";

	/**
	 * Error 발생 시 메일 전송 여부 - error.mail.send.enable
	 */
	public static final String ERROR_MAIL_SEND_ENABLE = "error.mail.send.enable";
	/**
	 * Error 발생 시 메일 전송한다고 설정시에도 전송하지 않을 status - error.mail.send.disable.status
	 */
	public static final String ERROR_MAIL_SEND_DISABLE_STATUS = "error.mail.send.disable.status";
	
	/**
	 * Error 발생 시, Console에 Trace 출력 여부 설정 - error.print.stack.trace
	 */
	public static final String ERROR_PRINT_STACK_TRACE = "error.print.stack.trace";

	/**
	 * DB에 저장하고자 하는 Error Status 목록.(ex : 500, 401, 403...) 값이 지정되어 있지 않으면, 
	 * 모든 Status에 대하여 Log를 저장. - error.save.status.list
	 */
	public static final String ERROR_SAVE_STATUS_LIST = "error.save.status.list";

	/**
	 * Error Log를 저장하지 않을 Exception 설정 - error.unwritable.exception.types
	 */
	public static final String ERROR_UNWRITABLE_EXCEPTION_TYPES = "error.unwritable.exception.types";

	/**
	 * Not Modifiable Module List - module.not.modifiable.list
	 */
	public static final String MODULE_NOT_MODIFIABLE_LIST = "module.not.modifiable.list";
	
	/**
	 * Server URL - server.contrext.path
	 */
	public static final String SERVER_CONTEXT_PATH = "server.context.path";

	/**
	 * 시스템 접속 URL ex) http://127.0.0.1:3000/std - client.context.path
	 */
	public static final String CLIENT_CONTEXT_PATH = "client.context.path";
	
	/**
	 * 화면 페이지 당 레코드 개수 정보 - screen.pagination.page.limit
	 */
	public static final String SCREEN_PAGE_LIMIT = "screen.pagination.page.limit";

	/**
	 * 기본 검색 기간 (default : 7) - search.default.period
	 */
	public static final String SEARCH_DEFAULT_PERIOD = "search.default.period";
	
	/**********************************************************************************************
	 *									Application.Properties 설정
	 **********************************************************************************************/
	/**
	 * Spring 기본 설정
	 */
	public static final String SPRING_PROFILES = "spring.profiles";
	public static final String PROFILE_PRDUCTION_MODE = "production";
	public static final String PROFILE_DEVELOPMENT_MODE = "development";
	public static final String SERVER_PORT = "server.port";
	public static final String SERVER_SESSION_TIMEOUT = "server.session.timeout";
	public static final String SPRING_DATASOURCE_NAME = "spring.datasource.name";
	public static final String SPRING_DATASOURCE_DRIVERCLASSNAME = "spring.datasource.driverClassName";
	public static final String SPRING_DATASOURCE_URL = "spring.datasource.url";
	public static final String SPRING_DATASOURCE_USERNAME = "spring.datasource.username";
	public static final String SPRING_DATASOURCE_PASSWORD = "spring.datasource.password";
	public static final String SPRING_DATASOURCE_MAX_ACTIVE = "spring.datasource.max-active";
	public static final String MULTIPART_MAXFILESIZE = "spring.http.multipart.max-file-size";
	public static final String MULTIPART_MAXREQUESTSIZE = "spring.http.multipart.max-request-size";
	public static final String SPRING_JACKSON_DATE_FORMAT = "spring.jackson.date-format";
	public static final String SPRING_JACKSON_PROPERTY_NAMING_STRATEGY = "spring.jackson.property-naming-strategy";
	public static final String SPRING_JACKSON_SERIALIZATION_INDENT_OUTPUT = "spring.jackson.serialization.INDENT_OUTPUT";
	public static final String SPRING_JACKSON_SERIALIZATION_FAIL_ON_EMPTY_BEANS = "spring.jackson.serialization.FAIL_ON_EMPTY_BEANS";
	public static final String SPRING_JACKSON_SERIALIZATION_INCLUSION = "spring.jackson.serialization-inclusion";
	public static final String SPRING_JACKSON_TIME_ZONE = "spring.jackson.time-zone";
	public static final String MANAGEMENT_SECURITY_ENABLED = "management.security.enabled";
	public static final String SECURITY_BASIC_ENABLED = "security.basic.enabled";
	public static final String SPRING_REDIS_HOST = "spring.redis.host";
	public static final String SPRING_REDIS_PASSWORD = "spring.redis.password";
	public static final String SPRING_REDIS_PORT = "spring.redis.port";
	/**
	 * Job 스케줄링 활성화 여부
	 */
	public static final String JOB_SCHEDULER_ENABLED = "job.scheduler.enable";
	
	/**
	 * Validation Error를 로깅할 지 여부
	 */
	public static final String LOG_VALIDATION_ERROR_ENABLED = "log.validation.error.enabled";
	/**
	 * Error 발생 시, Error 정보를 DB에 저장.
	 */
	public static final String ERROR_SAVE_ENABLE = "error.save.enable";

	/**
	 * Server Info
	 */
	public static final String INFO_ID = "info.id";
	public static final String INFO_NAME = "info.name";
	public static final String INFO_VERSION = "info.version";
	public static final String INFO_STAGE = "info.stage";

	/**
	 * Dbist
	 */
	public static final String DML_DOMAIN = "dml.domain";
	public static final String DBIST_DDL_ENABLE = "dbist.ddl.enable";
	public static final String DBIST_BASE_ENTITY_PATH = "dbist.base.entity.path";
	public static final String DBIST_VALIDATE_BEFORE_SAVE = "dbist.entity.validateBeforeSave";
	
	/**
	 * AOP
	 */
	public static final String SQLASPECT_ENABLED = "sqlAspect.enabled";
	public static final String SQLASPECT_PRETTYPRINT = "sqlAspect.prettyPrint";
	public static final String SQLASPECT_COMBINEDPRINT = "sqlAspect.combinedPrint";
	public static final String SQLASPECT_INCLUDEELAPSEDTIME = "sqlAspect.includeElapsedTime";
	public static final String TRANSACTIONMANAGER_DEFAULTTIMEOUT = "transactionManager.defaultTimeout";

	/**
	 * E-Mail
	 */
	public static final String MAIL_SMTP_HOST = "mail.smtp.host";
	public static final String MAIL_SMTP_PORT = "mail.smtp.port";
	public static final String MAIL_SMTP_USER = "mail.smtp.user";
	public static final String MAIL_SMTP_PASSWORD = "mail.smtp.password";
	public static final String MAIL_SMTP_PROTOCOL = "mail.smtp.protocol";
	public static final String MAIL_SMTP_AUTH = "mail.smtp.auth";
	public static final String MAIL_SMTP_STARTTLS_ENABLE = "mail.smtp.starttls.enable";
	
	public static final String MAIL_TRANSPORT_PROTOCOL = "mail.transport.protocol";
	
	/**
	 * Service Log
	 */
	
	/**
	 * 서비스 실행 정보에 대한 로그 출력 여부 설정.
	 */
	public static final String SERVICE_LOG_PRINT_ENABLE = "service.log.print.enable";
	
	/**
	 * 실행 결과값에 대한 출력 여부 설정.
	 */
	public static final String SERVICE_LOG_PRINT_WITH_RESULT = "service.log.print.with.result";

	/**
	 * Service Log를 생성 할 URL 목록.
	 */
	public static final String SERVICE_LOG_AVAILABLE_URLS = "servcie.log.available.urls";

	/**
	 * Service Log 생성 시 제외 할 URL 목록.
	 */
	public static final String SERVICE_LOG_EXCEPT_URLS = "servcie.log.except.urls";

	/**
	 * HTTP Sender에 대한 Log를 출력 여부 설정.
	 */
	public static final String HTTP_SENDER_LOG_ENABLE = "http.sender.log.enable";

	/**
	 * HTTP Sender에 대한 Log를 생성 할 URL 목록.
	 */
	public static final String HTTP_SENDER_LOG_AVAILABLE_URLS = "http.sender.log.available.urls";

	/**
	 * HTTP Sender Log 생성 시 제외 할 URL 목록.
	 */
	public static final String HTTP_SENDER_LOG_EXCEPT_URLS = "http.sender.log.except.urls";
	
	/**
	 * Log를 삭제할 기간 설정(day).
	 */
	public static final String SERVICE_LOG_DELETE_PERIOD_DAY = "service.log.delete.period.day";

	/**
	 * 삭제 할 로그 폴더 설정.
	 */
	public static final String SERVICE_LOG_DELETE_DIRECTORIES = "service.log.delete.directories";

	/**
	 * UTC 시간 활성화 여부 (Default : false)
	 */
	public static final String ENABLE_UTC_TIME = "enable.utc.time";
	
	
	/********************************************************************************
	 *							사용자 프로세스 메일링 템플릿 설정
	 ********************************************************************************/	
	/**
	 * 사용자 프로세스 신청 공통 Mail Template
	 */
	public static final String MAIL_TEMPLATE_ACCOUNT_COMMON_REQUEST = "mail.template.account.common.request";
	/**
	 * 사용자 프로세스 공통 처리 완료 Mail Template
	 */
	public static final String MAIL_TEMPLATE_ACCOUNT_COMMON_APPROVED = "mail.template.account.common.approved";
	/**
	 * 사용자 프로세스 공통 반려 Mail Template
	 */
	public static final String MAIL_TEMPLATE_ACCOUNT_COMMON_REJECTED = "mail.template.account.common.rejected";
	
	/**
	 * 계정 등록 신청 Mail Template
	 */
	public static final String MAIL_TEMPLATE_ACCOUNT_REGISTER_REQUEST = "mail.template.account.register.request";
	/**
	 * 계정 신청 승인 Mail Template
	 */
	public static final String MAIL_TEMPLATE_ACCOUNT_REGISTER_APPROVED = "mail.template.account.register.approved";
	/**
	 * 계정 신청 반려 Mail Template
	 */
	public static final String MAIL_TEMPLATE_ACCOUNT_REGISTER_REJECTED = "mail.template.account.register.rejected";
	/**
	 * 비밀번호 초기화 처리 결과 Mail Template
	 */
	public static final String MAIL_TEMPLATE_ACCOUNT_INIT_PASSWORD_RESULT = "mail.template.account.init.password.result";
	/**
	 * 비밀번호 초기화 요청 Mail Template
	 */
	public static final String MAIL_TEMPLATE_ACCOUNT_INIT_PASSWORD_REQUEST = "mail.template.account.init.password.request";
	/**
	 * 계정 활성화 요청 Mail Template
	 */
	public static final String MAIL_TEMPLATE_ACCTOUN_ACTIVATION_REQUEST = "mail.template.account.activation.request";
	/**
	 * 계정 활성화 승인 결과 Mail Template
	 */
	public static final String MAIL_TEMPLATE_ACCOUNT_ACTIVATION_APPROVED = "mail.template.account.activation.approved";
	/**
	 * 계정 활성화 요청 거절 결과 Mail Template
	 */
	public static final String MAIL_TEMPLATE_ACCOUNT_ACTIVATION_REJECTED = "mail.template.account.activation.rejected";
	/**
	 * 계정 비 활성화 처리 결과 Mail Template
	 */
	public static final String MAIL_TEMPLATE_ACCOUNT_INACTIVATION_RESULT = "mail.template.account.inactivation.result";
	
	/**
	 * Error Notification Mail Template
	 */
	public static final String MAIL_TEMPLATE_ERROR_NOTIFICATION = "mail.template.error.error";
	
	/**********************************************************************************************
	 *									Password 관련 설정
	 **********************************************************************************************/	
	/**
	 * Set Initialized Password.
	 */
	public static final String SECURITY_INIT_PASS = "security.init.pass";

	/**
	 * Random Password 사용여부 설정.
	 */
	public static final String SECURITY_USE_RANDOM_INIT_PASS = "security.use.random.init.pass";
	
	/**
	 * Password Encoder Algorithm 
	 */
	public static final String SECURITY_PASSWORD_ENCODER_ALGORITHM = "security.password.encoder.algorithm";	
		
}