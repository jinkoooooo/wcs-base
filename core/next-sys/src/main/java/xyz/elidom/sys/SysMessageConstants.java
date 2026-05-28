/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys;

import xyz.elidom.orm.OrmMessageConstants;

/**
 * 시스템 모듈 메시지 상수 정의
 * 
 * @author Minu.Kim
 */
public class SysMessageConstants extends OrmMessageConstants {

	/**
	 * keys count and values count mismatch!
	 * 키 카운트와 벨류 카운트가 일치하지 않습니다.
	 */
	public static final String KEY_AND_VALUE_MISMATCH = "KEY_AND_VALUE_MISMATCH";

	/**
	 * {0}은(는) 인스턴스화 할수 없습니다. ({1})
	 * {0} Could not instantiate Class ({1})
	 */
	public static final String COULD_NOT_INSTANTIATE = "COULD_NOT_INSTANTIATE";

	/**
	 * 지원되지 않는 method입니다.
	 * Not support method
	 */
	public static final String NOT_SUPPORTED_METHOD = "NOT_SUPPORTED_METHOD";

	/**
	 * Restful Service URL({0}) 이 올바르지 않습니다.
	 * Invalid Restful Service URL({0}).
	 */
	public static final String INVALID_SERVICE_URL = "INVALID_SERVICE_URL";

	/**
	 * URL({0})에서 서비스를 찾을 수 없습니다.
	 * Service Not Found By URL({0}).
	 */
	public static final String NOT_FOUND_URL = "NOT_FOUND_URL";

	/**
	 * 클래스 ({0})의 필드({1})에 값 설정 실패
	 * Failed to assign value to field({1}) of class ({0})
	 */
	public static final String FAIL_TO_REFLECT_SET = "FAIL_TO_REFLECT_SET";

	/**
	 * 클래스 ({0})의 필드({1})에 값 추출 실패
	 * Failed to get value from field({1}) of class ({0})
	 */
	public static final String FAIL_TO_REFLECT_GET = "FAIL_TO_REFLECT_GET";

	/***************************************************************************************
	 * Server
	 ***************************************************************************************/

	/**
	 * Null 객체는 허용될 수 없습니다.
	 * Null Object is not allowed.
	 */
	public static final String NULL_OBJECT_NOT_ALLOWED = "NULL_OBJECT_NOT_ALLOWED";

	/**
	 * SQL이 올바르지 않습니다.
	 * Bad SQL Grammar
	 */
	public static final String BAD_SQL_GRAMMAR = "BAD_SQL_GRAMMAR";

	/**
	 * 데어터 정합성이 맞지 않습니다.
	 * Data Integrity Violation
	 */
	public static final String INTEGRITY_VIOLATION = "INTEGRITY_VIOLATION";

	/**
	 * 네트워크에서 해당 호스트를 찾을 수 없습니다.
	 * Network is unreachable.
	 */
	public static final String NETWORK_UNREACHABLE = "NETWORK_UNREACHABLE";

	/**
	 * 데이터베이스 호스트를 찾을 수 없습니다.
	 * Database is unreachable.
	 */
	public static final String DATABASE_UNREACHABLE = "DATABASE_UNREACHABLE";

	/**
	 * 트랜잭션 처리시간이 종료되었습니다.
	 * Transaction timed out.
	 */
	public static final String TRANSACTION_TIME_OUT = "TRANSACTION_TIME_OUT";

	/**
	 * Data 복사 하는 중 에러가 발생했습니다.
	 * Failed to clone data.
	 */
	public static final String FAIL_TO_CLONE_DATA = "FAIL_TO_CLONE_DATA";

	/**
	 * {0}의 데이터({1})가 유효하지 않습니다.
	 * Data ({1}) of {0} is invalid.
	 */
	public static final String INVALID_DATA = "INVALID_DATA";

	/*************************************************************************************************************
	 * Common
	 *************************************************************************************************************/

	/**
	 * 이미 처리된 항목입니다.
	 * Already been proceeded.
	 */
	public static final String ALREADY_BEEN_PROCEEDED = "ALREADY_BEEN_PROCEEDED";

	/**
	 * ID 또는 비밀번호가 올바르지 않습니다.
	 * ID or Password is not correct.
	 */
	public static final String USER_INVALID_ID_OR_PASS = "USER_INVALID_ID_OR_PASS";

	/**
	 * 비활성화된 계정입니다.
	 * Deactivated account.
	 */
	public static final String USER_INACTIVATED_ACCOUNT = "USER_INACTIVATED_ACCOUNT";

	/**
	 * 이미 활성화되어 있는 사용자입니다.
	 * Already activated account.
	 */
	public static final String USER_ALREADY_ACTIVATED = "USER_ALREADY_ACTIVATED";

	/**
	 * 사용자가 존재하지 않습니다.
	 * User does not exist
	 */
	public static final String USER_NOT_EXIST = "USER_NOT_EXIST";

	/**
	 * 사용자가 이미 존재합니다.
	 * User already exists
	 */
	public static final String USER_ALREADY_EXIST = "USER_ALREADY_EXIST";

	/**
	 * 이미 접수된 요청입니다.
	 * This request already has been received.
	 */
	public static final String REQUEST_ALREADY_RECEIVED = "REQUEST_ALREADY_RECEIVED";

	/**
	 * 요청하신 권한은 이미 신청자님께서 가지고 있습니다.
	 * You already have the authorization.
	 */
	public static final String ALREADY_HAVE_AUTHROIZATION = "ALREADY_HAVE_AUTHROIZATION";

	/**
	 * 사용자께서는 {0}-{1}에 대한 접근 권한이 없습니다.
	 * You have no authroity to access {0}-{1}
	 */
	public static final String HAS_NO_AUTHORITY = "HAS_NO_AUTHORITY";

	/**
	 * {0}은 이미 처리되었습니다.
	 * {0} has already finished.
	 */
	public static final String ALREADY_FINISHED = "ALREADY_FINISHED";

	/**
	 * 요청하신 내용이 관리자에게 접수되었습니다.
	 * Your request has been sent to administrator.
	 */
	public static final String REQUEST_SENT_TO_ADMIN = "REQUEST_SENT_TO_ADMIN";

	/**
	 * 요청하신 내용이 '승인' 처리되었습니다.
	 * Your request has been processed 'approved'.
	 */
	public static final String REQUEST_PROCESSED_APPROVED = "REQUEST_PROCESSED_APPROVED";

	/**
	 * 요청하신 내용이 '반려' 처리되었습니다.
	 * Your request has been processed 'reject'.
	 */
	public static final String REQUEST_PROCESSED_REJECTED = "REQUEST_PROCESSED_REJECTED";

	/**
	 * 요청하신 내용이 '{0}' 처리되었습니다.
	 * Your request has been processed '{0}'.
	 */
	public static final String REQUEST_PROCESSED = "REQUEST_PROCESSED";

	/**
	 * 요청하신 내용의 처리 결과 메일이 신청자에게 전송되었습니다.
	 * Results of processing your request has been sent.
	 */
	public static final String RESULT_SENT_TO_REQUESTER = "RESULT_SENT_TO_REQUESTER";

	/**
	 * 이 요청은 계정 승인 요청이 아닙니다.
	 * This request is not for a account approval.
	 */
	public static final String NOT_A_ACCOUNT_APPR_REQUEST = "NOT_A_ACCOUNT_APPR_REQUEST";

	/**
	 * 이 요청은 계정 패스워드 초기화 요청이 아닙니다.
	 * This request is not for a password initialization.
	 */
	public static final String NOT_A_PASSWORD_INIT_REQUEST = "NOT_A_PASSWORD_INIT_REQUEST";

	/**
	 * 이 요청은 계정 활성화 요청이 아닙니다.
	 * This request is not for a account activation.
	 */
	public static final String NOT_A_ACCOUNT_ACTIVATION_REQUEST = "NOT_A_ACCOUNT_ACTIVATION_REQUEST";

	/**
	 * 이 요청은 권한 추가 요청이 아닙니다.
	 * This request is not for a authorization.
	 */
	public static final String NOT_A_AUTHORIZATION_REQUEST = "NOT_A_AUTHORIZATION_REQUEST";

	/**
	 * 요청한 서버 정보 ({0})로 Domain 정보를 찾을 수 없습니다.
	 * Cannot find Domain information by request server ({0})
	 */
	public static final String DOMAIN_NOT_EXIST = "DOMAIN_NOT_EXIST";

	/**
	 * 도메인 ({0})는 시스템 도메인이므로 삭제할 수 없습니다.
	 * Domain ({0}) is System Domain, so it's not possible to delete!
	 */
	public static final String SYSTEM_DOMAIN_CANNOT_BE_DELETED = "SYSTEM_DOMAIN_CANNOT_BE_DELETED";

	/**
	 * 개발 모드가 아닌 경우 이 요청은 실행될 수 없습니다.
	 * This request is allowed only Development Mode.
	 */
	public static final String DEV_MODE_FUNCTION_NOT_SUPPORTED = "DEV_MODE_FUNCTION_NOT_SUPPORTED";

	/**
	 * 인증 정보가 올바르지 않습니다.
	 * Authentication is incorrect.
	 */
	public static final String INCORRECT_AUTHENTICATION = "INCORRECT_AUTHENTICATION";

	/**
	 * 시스템 계정 등록 요청.
	 * System account registering request.
	 */
	public static final String USER_REQUEST = "USER_REQUEST";

	/**
	 * 해당 도메인에 관리자로 지정된 사용자가 없어서 관리자에게 메일을 보낼 수 없습니다.
	 * Administrators of the domain does not exist, so you can not send mail to the administrator.
	 */
	public static final String DOMAIN_ADMIN_NOT_EXIST = "DOMAIN_ADMIN_NOT_EXIST";

	/**
	 * 계정 신청이 승인되었습니다.
	 * Account approval request has been accepted.
	 */
	public static final String USER_APPROVAL = "USER_APPROVAL";

	/**
	 * 계정 신청이 반려되었습니다.
	 * Account approval request has been rejected.
	 */
	public static final String USER_REJECT = "USER_REJECT";

	/**
	 * 계정 활성화 요청드립니다.
	 * Request account activation.
	 */
	public static final String USER_REQUEST_ACTIVE_ACCOUNT = "USER_REQUEST_ACTIVE_ACCOUNT";

	/**
	 * 계정 활성화 요청이 완료되었습니다.
	 * Activation request is completed.
	 */
	public static final String USER_COMPLETE_ACTIVE_ACCOUNT = "USER_COMPLETE_ACTIVE_ACCOUNT";

	/**
	 * 계정이 비활성화 되었습니다.
	 * Inactivation user account.
	 */
	public static final String USER_INACTIVE_ACCOUNT = "USER_INACTIVE_ACCOUNT";

	/**
	 * 비밀번호 초기화 요청드립니다.
	 * Please reset my password.
	 */
	public static final String USER_REQUEST_INIT_PASS = "USER_REQUEST_INIT_PASS";

	/**
	 * 비밀번호 초기화가 완료되었습니다.
	 * Completed to reset password.
	 */
	public static final String USER_COMPLETE_INIT_PASS = "USER_COMPLETE_INIT_PASS";

	/**
	 * 비밀번호 초기화 요청이 반려되었습니다.
	 * Rejected to reset password.
	 */
	public static final String USER_REJECT_INIT_PASS = "USER_REJECT_INIT_PASS";

	/**
	 * 최근에 사용한 비밀번호 입니다. 새로운 비밀번호로 변경해주세요.
	 */
	public static final String USER_ALREADY_USED_PASS = "USER_ALREADY_USED_PASS";
	
	/**
	 * 초기 비밀번호가 설정되어 있지 않습니다. 설정 메뉴에서 초기 비밀번호를 설정해주세요.
	 */
	public static final String USER_EMPTY_INIT_PASS = "USER_EMPTY_INIT_PASS";

	/**
	 * 비밀번호 변경 횟 수를 초과하였습니다. 내일 다시 시도해주세요.
	 */
	public static final String USER_PASS_MODIFY_EXCEED_COUNT = "USER_PASS_MODIFY_EXCEED_COUNT";

	/**
	 * 사용자 권한 추가 요청드립니다.
	 * System access authorization request
	 */
	public static final String USER_REQUEST_ADD_AUTH = "USER_REQUEST_ADD_AUTH";

	/**
	 * 사용자 권한 추가 요청이 승인되었습니다.
	 * Approved system access authorization request
	 */
	public static final String USER_COMPLETE_ADD_AUTH = "USER_COMPLETE_ADD_AUTH";

	/**
	 * 사용자 권한 추가 요청이 반려되었습니다.
	 * Rejected system access authorization request
	 */
	public static final String USER_REJECT_ADD_AUTH = "USER_REJECT_ADD_AUTH";

	/**
	 * Root path가 올바르지 않습니다.
	 * Root path is not valid.
	 */
	public static final String FILE_ROOT_PATH_NOT_EXIST = "FILE_ROOT_PATH_NOT_EXIST";

	/**
	 * File 생성 중 에러가 발생하였습니다.
	 * Error occured during file creating.
	 */
	public static final String CREATE_FILE_ERROR = "CREATE_FILE_ERROR";

	/**
	 * File {0} 읽기 중 에러가 발생하였습니다.
	 * Failed to read file {0}.
	 */
	public static final String READ_FILE_ERROR = "READ_FILE_ERROR";

	/**
	 * Table이 존재하지 않습니다.
	 * Table does not exist.
	 */
	public static final String TABLE_DOES_NOT_EXIST = "TABLE_DOES_NOT_EXIST";

	/**
	 * 지원되지 않는 {0} Type입니다. [{1}]
	 * Not Supported {0} Type. [{1}]
	 */
	public static final String NOT_SUPPORTED_TYPE = "NOT_SUPPORTED_TYPE";

	/**
	 * 알 수 없는 타입 형식입니다. [{0}]
	 * Unknown Type : {0}
	 */
	public static final String UNKNOWN_TYPE = "UNKNOWN_TYPE";

	/**
	 * 엔티티 이름이 올바르지 않습니다. [{0}]
	 * Invalid entity name. [{0}]
	 */
	public static final String IS_NOT_ENTITY = "IS_NOT_ENTITY";

	/**
	 * 이 타입은 반드시 [{0}] 옵션이 필요합니다.
	 * The type should be [{0}] option.
	 */
	public static final String NEED_OPTION = "NEED_OPTION";

	/**
	 * File Download 실행 중, 에러가 발생하였습니다.
	 * Error occured during file download.
	 */
	public static final String FILE_DOWNLOAD_ERROR = "FILE_DOWNLOAD_ERROR";

	/**
	 * 업로드 가능한 파일 용량을 초과하였습니다.
	 * Excess limit file size.
	 */
	public static final String UPLOAD_FILE_SIZE_LIMIT_ERROR = "UPLOAD_FILE_SIZE_LIMIT_ERROR";

	/**
	 * 기본 Path가 비어 있습니다.
	 * Default Path is empty.
	 */
	public static final String FILE_DEFAULT_PATH_IS_EMPTY = "FILE_DEFAULT_PATH_IS_EMPTY";

	/**
	 * 아직 지원되지 않는 기능입니다.
	 * Not Supported Yet!
	 */
	public static final String NOT_SUPPORTED_YET = "NOT_SUPPORTED_YET";

	/**
	 * 파일({0})을 찾을수가 없습니다.
	 * File({0}) Not Found.
	 */
	public static final String FILE_NOT_FOUND = "FILE_NOT_FOUND";

	/**
	 * 한개 이상의 데이터가 포함되어 삭제할 수 없습니다.
	 * Can not delete it, It Has one or more data.
	 */
	public static final String HAS_ONE_OR_MORE_DATA = "HAS_ONE_OR_MORE_DATA";

	/**
	 * [{0}] 필드가 존재하지 않습니다.
	 * [{0}] Field does not exist.
	 */
	public static final String FIELD_NOT_EXIST = "FIELD_NOT_EXIST";

	/**
	 * 키 파라미터에서 빈 키를 발견했습니다.
	 * Empty key found at key parameters
	 */
	public static final String NOT_FOUND_KEYS = "NOT_FOUND_KEY";

	/**
	 * Entity[{0}] 중 'cudFlag가 존재하지 않습니다.
	 * There is no 'cudFlag' in the entity class [{0}]
	 */
	public static final String NOT_FOUND_CUD_FLAG = "NOT_FOUND_CRUD_FLAG";

	/**
	 * cudFlag의 값을 가져오는데 실패하였습니다.
	 * Failed to get cudFlag value
	 */
	public static final String EMPTY_CUD_FLAG_VALUE = "EMPTY_CUD_FLAG_VALUE";

	/**
	 * Failed to setup detail meta!
	 */
	public static final String FAILED_META_SETUP = "FAILED_META_SETUP";

	/**
	 * [{0}] (은)는 유효한 파일이 아닙니다.
	 * Invalid file [{0}].
	 */
	public static final String INVALID_FILE = "INVALID_FILE";

	/**
	 * Data Type[{0}]이 올바르지 않습니다.
	 * Data Type[{0}] is not valid.
	 */
	public static final String INVALID_DATA_TYPE = "INVALID_DATA_TYPE";

	/**
	 * Not Connected.
	 * 연결되지 않았습니다.
	 */
	public static final String NOT_CONNECTED = "NOT_CONNECTED";

	/**
	 * {1} Error occurred when doing {0}
	 * {0} 하던 도중에 {1} 에러가 발생했습니다.
	 */
	public static final String ERROR_WHEN_DO_SOMETHING = "ERROR_WHEN_DO_SOMETHING";

	/**
	 * Failed to parse {0} code
	 * {0} 데이터 파싱 중 에러가 발생했습니다.
	 */
	public static final String FAIL_TO_PARSE = "FAIL_TO_PARSE";

	/**
	 * Failed to Compile {0} Code
	 * {0} 코드 컴파일 중 에러가 발생했습니다.
	 */
	public static final String FAIL_TO_COMPILE = "FAIL_TO_COMPILE";

	/**
	 * Failed to Process {0} Template
	 * {0} 템플릿 프로세스를 실패하였습니다.
	 */
	public static final String FAIL_TO_PROCESS_TEMPLATE = "FAIL_TO_PROCESS_TEMPLATE";

	/**
	 * Entity class has no default constructor
	 * 엔티티 클래스에 기본 생성자가 없습니다.
	 */
	public static final String HAS_NO_CONSTRUCTOR = "HAS_NO_CONSTRUCTOR";

	/**
	 * 잘못된 쿼리 - 파라미터 카운트가 일치하지 않습니다. [{0}:{1}]
	 * Invalid query - Parameter count mismatch.[{0}:{1}]
	 */
	public static final String MISMATCH_PARAM_COUNT = "MISMATCH_PARAM_COUNT";

	/**
	 * 유효하지 않은 {0} 클래스 ({1}) - 클래스가 {2}의 구현체가 아닙니다.
	 * Invalid {0} Class ({1}) - Class is not instance of {2}.
	 */
	public static final String IS_NOT_INSTANCE_OF = "IS_NOT_INSTANCE_OF";

	/**
	 * 파일 복사에 실패하였습니다.
	 * Failed to copy file!
	 */
	public static final String FILE_COPY_FAILED = "FILE_COPY_FAILED";

	/**
	 * 스트리밍 에러
	 * Streaming error
	 */
	public static final String STREAMING_ERROR = "STREAMING_ERROR";

	/**
	 * 하나 이상의 데이터가 존재합니다.
	 * There are data more than one.
	 */
	public static final String MORE_THAN_ONE_ERROR = "MORE_THAN_ONE";

	/**
	 * 서비스 생성에 실패하였습니다.
	 * Failed to generate service!
	 */
	public static final String GENERATE_FAIL = "GENERATE_FAIL";

	/**
	 * 실행 중인 Listener가 아닙니다.
	 * It's not running listener.
	 */
	public static final String IS_NOT_IN_USE_LISTENER = "IS_NOT_IN_USE_LISTENER";

	/**
	 * {0} ({1})은(는) [{2}] 상태가 아닙니다.
	 * The {0} ({1}) is not [{2}] Status.
	 */
	public static final String MISMATCH_STATUS = "MISMATCH_STATUS";

	/**
	 * {0}와(과) {1}이(가) 맞지 않습니다.
	 * {0} is not equal {1}.
	 */
	public static final String A_IS_NOT_EQUAL_B = "A_IS_NOT_EQUAL_B";

	/**
	 * 진행 중인 {0}(이)가 아닙니다.
	 * {0} is not on the process.
	 */
	public static final String DOES_NOT_PROCEED = "DOES_NOT_PROCEED";

	/**
	 * 이미 실행 중인 {0}입니다.
	 * {0} is alread on process.
	 */
	public static final String ALREADY_PROCEED = "ALREADY_PROCEED";

	/**
	 * {0} 이(가) 실행중이지 않습니다.
	 * {0} is not Running!
	 */
	public static final String IS_NOT_RUNNING = "IS_NOT_RUNNING";

	/**
	 * Mail 발송에 실패하였습니다.
	 * Failed to send mail
	 */
	public static final String FAIL_TO_SEND_MAIL = "FAIL_TO_SEND_MAIL";

	/**
	 * 엔티티 ({0})의 ID Type ({1})이 유효하지 않습니다.
	 * Invalid ID Type ({1}) of entity ({0})
	 */
	public static final String INVALID_ENTITY_ID_TYPE = "INVALID_ENTITY_ID_TYPE";

	/**
	 * {0} 은(는) 이미 {1} 상태입니다.
	 * The {0} is already {1} status.
	 */
	public static final String A_ALREADY_B_STATUS = "A_ALREADY_B_STATUS";

	/**
	 * {0}의 {1} 수량이 {2} 수량을 초과하였습니다.
	 * {1} quantity has exceeded the {2} quantity of {0}.
	 */
	public static final String OVER_QUANTITY = "OVER_QUANTITY";

	/**
	 * {0}에 {1}이 설정되지 않았습니다.
	 * {1} is not set to {0}.
	 */
	public static final String A_IS_NOT_SET_TO_B = "A_IS_NOT_SET_TO_B";

	/*************************************************************************************************************
	 * ASSERT
	 *************************************************************************************************************/
	/**
	 * 문자열 '{0}'은 '{1}'을 포함하고 있습니다.
	 * Text '{0}' contains the string '{1}'.
	 */
	public static final String A_CONTAINS_B = "A_CONTAINS_B";

	/**
	 * 문자열 '{0}'은 '{1}'을 포함하고 있습니다.
	 * Text '{0}' contains the string '{1}'.
	 */
	public static final String A_DOES_NOT_CONTAINS_B = "A_DOES_NOT_CONTAINS_B";

	/**
	 * Class '{0}' 은 Class '{1}'의 파생 클래스가 아닙니다.
	 * Class '{0}' is not assignable to Class '{1}'.
	 */
	public static final String TYPEA_IS_NOT_ASSIGNABLE_OF_TYPEB = "TYPEA_IS_NOT_ASSIGNABLE_OF_TYPEB";

	/**
	 * Object '{0}' 은 Class '{1}' 타입이 아닙니다.
	 * Object '{0}' is not instance of Class '{1}'.
	 */
	public static final String OBJECT_IS_NOT_INSTANCEOF_CLASS = "OBJECT_IS_NOT_INSTANCEOF_CLASS";

	/**
	 * 값'{0}'이 참이 아닙니다.
	 * Value '{0}' is not true.
	 */
	public static final String BOOLEAN_IS_FALSE = "BOOLEAN_IS_FALSE";

	/**
	 * 컬렉션에 빈 값이 포함되어 있습니다.
	 * Collection contains a null element.
	 */
	public static final String COLLECTION_HAS_NULL_ELEMENT = "COLLECTION_HAS_NULL_ELEMENT";

	/**
	 * 값 '{0}'이 빈 값 입니다.
	 * Value of '{0}' is empty.
	 */
	public static final String VALUE_IS_EMPTY = "VALUE_IS_EMPTY";

	/**
	 * 값 '{0}'은 빈 값이 아닙니다.
	 * Value of '{0}' is not empty.
	 */
	public static final String VALUE_IS_NOT_EMPTY = "VALUE_IS_NOT_EMPTY";

}