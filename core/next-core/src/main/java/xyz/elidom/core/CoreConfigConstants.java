/* Copyright © Nearsolution Inc. All rights reserved. */
/**
 * 
 */
package xyz.elidom.core;

import xyz.elidom.sys.SysConfigConstants;

/**
 * Core 모듈의 설정에 필요한 상수 정의
 * 
 * @author Minu.Kim
 */
public class CoreConfigConstants extends SysConfigConstants {

	/**********************************************************************************************
	 * 			Initial Setup - 데이터베이스 테이블, 시퀀스, 함수 등 생성 및 초기데이터 셋업 설정. 
	 * 			주의 (production mode에서는 initial setup 절대 불가)
	 **********************************************************************************************/
	/**
	 * initial setup flag
	 */
	public static final String ELIDOM_INITIAL_SETUP = "elidom.initial.setup";
	/**
	 * initial setup을 위한 시스템 도메인 아이디
	 */
	public static final String ELIDOM_INITIAL_DOMAIN_ID = "elidom.initial.domain.id";	
	/**
	 * initial setup을 위한 시스템 도메인 명
	 */
	public static final String ELIDOM_INITIAL_DOMAIN_NAME = "elidom.initial.domain.name";
	/**
	 * initial setup을 위한 시스템 도메인 브랜드 명
	 */
	public static final String ELIDOM_INITIAL_DOMAIN_BRAND_NAME = "elidom.initial.domain.brand_name";
	/**
	 * initial setup을 위한 시스템 도메인 URL
	 */
	public static final String ELIDOM_INITIAL_DOMAIN_URL = "elidom.initial.domain.url";
	/**
	 * initial setup을 위한 관리자 계정 ID
	 */
	public static final String ELIDOM_INITIAL_ADMIN_ID = "elidom.initial.admin.id";	
	/**
	 * initial setup을 위한 관리자 계정 이름
	 */
	public static final String ELIDOM_INITIAL_ADMIN_NAME = "elidom.initial.admin.name";
	/**
	 * initial setup을 위한 관리자 계정 이메일
	 */
	public static final String ELIDOM_INITIAL_ADMIN_EMAIL = "elidom.initial.admin.email";
	/**
	 * initial setup을 위한 관리자 계정 패스워드
	 */
	public static final String ELIDOM_INITIAL_ADMIN_PASSWD = "elidom.initial.admin.passwd";
	/**
	 * initial setup을 위한 첨부파일 저장소 루트 경로
	 */
	public static final String ELIDOM_INITIAL_STORAGE_ROOT = "elidom.initial.storage.root";
	/**
	 * Quartz 실행 여부 설정
	 */
	public static final String QUARTZ_SCHEDULER_ENABLE = "quartz.scheduler.enable";

	/**********************************************************************************************
	 *									Settings Table 설정
	 **********************************************************************************************/
	/**
	 * File storage root path
	 */
	public static final String FILE_ROOT_PATH = "file.root.path";
	
	/**
	 * 첨부파일 삭제시 물리적 파일도 삭제할 지 여부 
	 */
	public static final String ATTACH_FILE_DELETE = "attach.file.delete";	

	/**
	 * File Upload 시 Max Size 설정.
	 */
	public static final String UPLOAD_FILE_LIMIT_SIZE = "upload.file.limit.size";
	
	/**
	 * 첨부 파일 조회 서비스 URL
	 */
	public static final String FILE_SERVICE_PATH = "file.context.path";

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
}