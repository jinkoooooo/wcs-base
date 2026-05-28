/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.exception;

/**
 * Constants
 * 
 * @author Minu.Kim
 */
public class ExceptionMessageConstants {

	/*************************************************************************************************************
	 * Title Code
	 *************************************************************************************************************/

	/**
	 * Bad Request
	 */
	public static final String TITLE_BAD_REQUEST = "TITLE_BAD_REQUEST";

	/**
	 * Converting (Input Parameters) Error
	 */
	public static final String TITLE_PARSE_INPUT_ERROR = "TITLE_PARSE_INPUT_ERROR";

	/**
	 * Invalid Parameter Error
	 */
	public static final String TITLE_INVALID_PARAM = "TITLE_INVALID_PARAM";

	/**
	 * Record Not Found Error
	 */
	public static final String TITLE_RECORD_NOT_FOUND = "TITLE_RECORD_NOT_FOUND";

	/**
	 * Service Not Found Error
	 */
	public static final String TITLE_SERVICE_NOT_FOUND = "TITLE_SERVICE_NOT_FOUND";

	/**
	 * Record Already Exist Error
	 */
	public static final String TITLE_ALREADY_EXIST = "TITLE_ALREADY_EXIST";

	/**
	 * Database Error
	 */
	public static final String TITLE_DATA_BASE_ERROR = "TITLE_DATA_BASE_ERROR";

	/**
	 * Service Invalid Status Error
	 */
	public static final String TITLE_INVALID_STATUS = "TITLE_INVALID_STATUS";

	/**
	 * Converting (Output Parameters) Error
	 */
	public static final String TITLE_PARSE_OUTPUT_ERROR = "TITLE_PARSE_OUTPUT_ERROR";

	/**
	 * Runtime Error
	 */
	public static final String TITLE_RUNTIME_ERROR = "TITLE_RUNTIME_ERROR";

	/**
	 * Script Error
	 */
	public static final String TITLE_SCRIPT_RUNTIME_ERROR = "TITLE_SCRIPT_RUNTIME_ERROR";

	/**
	 * Service Error
	 */
	public static final String TITLE_SERVICE_ERROR = "TITLE_SERVICE_ERROR";

	/**
	 * Validation Error
	 */
	public static final String TITLE_VALIDATION_ERROR = "TITLE_VALIDATION_ERROR";

	/*************************************************************************************************************
	 * Client
	 *************************************************************************************************************/

	/**
	 * 클라이언트의 잘못된 요청입니다.
	 * Bad Request
	 */
	public static final String BAD_REQUEST = "BAD_REQUEST";

	/**
	 * Input Data 변환 시, 에러가 발생하였습니다.
	 * Error occured at converting input Data.
	 */
	public static final String PARSE_INPUT_ERROR = "PARSE_INPUT_ERROR";

	/**
	 * 파라미터 (이름: {0}, 값: {1})가 올바르지 않습니다.
	 * Parameter (name : {0}, value : {1}) is not valid.
	 */
	public static final String INVALID_PARAM = "INVALID_PARAM";

	/**
	 * 데이터가 존재하지 않습니다.
	 * Record not found.
	 */
	public static final String RECORD_NOT_FOUND = "RECORD_NOT_FOUND";

	/**
	 * 서비스가 존재하지 않습니다.
	 * Service does not exist.
	 */
	public static final String SERVICE_NOT_FOUND = "SERVICE_NOT_FOUND";

	/**
	 * 인증되지 않은 사용자 입니다.
	 * Unauthorized user.
	 */
	public static final String NOT_AUTHORIZED_USER = "NOT_AUTHORIZED_USER";

	/**
	 * 사용자 계정이 잠겨 있습니다.
	 * Account Locked.
	 */
	public static final String ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
	
	/**
	 * 계정이 활성화 되어 있지 않습니다.
	 * Inactive Account.
	 */
	public static final String INACTIVE_ACCOUNT = "INACTIVE_ACCOUNT";

	/**********************************************************************************************************
	 * Server
	 *************************************************************************************************************/
	
	/**
	 * 해당 데이터에 대한 권한이 존재하지 않습니다.
	 * Do not have permission to access data.
	 */
	public static final String NOT_PERMISSION = "NOT_PERMISSION";

	/**
	 * 생성하려는 Data가 이미 존재합니다.
	 * Data aleady exist.
	 */
	public static final String ALREADY_EXIST = "ALREADY_EXIST";

	/**
	 * Query 실행 중 에러가 발생하였습니다.
	 * Error occured during query excuting.
	 */
	public static final String DATA_BASE_ERROR = "DATA_BASE_ERROR";

	/**
	 * 상태가 유효하지 않습니다.
	 * Invalid status.
	 */
	public static final String INVALID_STATUS = "INVALID_STATUS";

	/**
	 * Output Data 변환 시, 에러가 발생하였습니다.
	 * Error occred at converting output Data.
	 */
	public static final String PARSE_OUTPUT_ERROR = "PARSE_OUTPUT_ERROR";

	/**
	 * 예상치 못한 에러가 발생하였습니다.
	 * Unexpected error occured during at runtime.
	 */
	public static final String RUNTIME_ERROR = "RUNTIME_ERROR";

	/**
	 * Script 실행 중 에러가 발생하였습니다.
	 * Error occured while run script.
	 */
	public static final String SCRIPT_RUNTIME_ERROR = "SCRIPT_RUNTIME_ERROR";

	/**
	 * 기능 실행 중 에러가 발생하였습니다.
	 * Error occured while run service.
	 */
	public static final String SERVICE_ERROR = "SERVICE_ERROR";

	/**
	 * Error occured during validation.
	 * 유효성 검사 중 에러가 발생하였습니다.
	 */
	public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
}