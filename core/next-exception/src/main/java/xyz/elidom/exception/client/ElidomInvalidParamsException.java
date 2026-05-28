/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.exception.client;

import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import xyz.elidom.exception.ExceptionMessageConstants;

/**
 * 서비스 실행 전 validation checking시 invalid한 경우. 이 중에서도 반드시 파라미터가 잘 못 전달된 경우 처럼
 * 클라이언트 사이드 폴트인 경우에만 해당.
 * 
 * @author shortstop
 */
public class ElidomInvalidParamsException extends ElidomClientException {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 2845778351026764341L;

	/**
	 * Error Code
	 */
	private static final int STATUS = HttpServletResponse.SC_BAD_REQUEST;
	private static final String TITLE_CODE = ExceptionMessageConstants.TITLE_INVALID_PARAM;
	private static final String ERROR_CODE = ExceptionMessageConstants.INVALID_PARAM;
	
	public ElidomInvalidParamsException() {
		super(STATUS, TITLE_CODE, ERROR_CODE, ERROR_CODE , null, null);	
	}

	public ElidomInvalidParamsException(String value) {
		super(STATUS, TITLE_CODE, value, value, null);
	}
	
	public ElidomInvalidParamsException(String value, List<String> params) {
		super(STATUS, TITLE_CODE, value, value, params, null);
	}

	public ElidomInvalidParamsException(String code, String message) {
		super(STATUS, TITLE_CODE, code, message, null);
	}
	
	public ElidomInvalidParamsException(String code, String message, List<String> params) {
		super(STATUS, TITLE_CODE, code, message, params, null);
	}
	
	public ElidomInvalidParamsException(String code, String message, Throwable cause) {
		super(STATUS, TITLE_CODE, code, message, cause);
	}
	
	public ElidomInvalidParamsException(String code, String message, List<String> params, Throwable cause) {
		super(STATUS, TITLE_CODE, code, message, params, cause);
	}

	public ElidomInvalidParamsException(Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, cause.getMessage(), cause);
	}

	public ElidomInvalidParamsException(String message, Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, message, cause);
	}
	
	public ElidomInvalidParamsException(String message, List<String> params, Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, message, params, cause);
	}
}