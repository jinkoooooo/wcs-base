/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.exception.server;

import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import xyz.elidom.exception.ExceptionMessageConstants;

/**
 * 서비스 실행 전 validation checking시 invalid한 경우. 이 중에서도 서버 사이드 폴트인 경우에 해당
 * 
 * @author shortstop
 */
public class ElidomInvalidStatusException extends ElidomServerException {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 6008154685108624769L;

	/**
	 * Error Code
	 */
	private static final int STATUS = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
	private static final String TITLE_CODE = ExceptionMessageConstants.TITLE_INVALID_STATUS;
	private static final String ERROR_CODE = ExceptionMessageConstants.INVALID_STATUS;
	
	public ElidomInvalidStatusException() {
		super(STATUS, TITLE_CODE, ERROR_CODE, ERROR_CODE , null, null);	
	}

	public ElidomInvalidStatusException(String value) {
		super(STATUS, TITLE_CODE, value, value, null);
	}
	
	public ElidomInvalidStatusException(String value, List<String> params) {
		super(STATUS, TITLE_CODE, value, value, params, null);
	}

	public ElidomInvalidStatusException(String code, String message) {
		super(STATUS, TITLE_CODE, code, message, null);
	}

	public ElidomInvalidStatusException(String code, String message, List<String> params) {
		super(STATUS, TITLE_CODE, code, message, params, null);
	}
	
	public ElidomInvalidStatusException(String code, String message, Throwable cause) {
		super(STATUS, TITLE_CODE, code, message, cause);
	}
	
	public ElidomInvalidStatusException(String code, String message, List<String> params, Throwable cause) {
		super(STATUS, TITLE_CODE, code, message, params, cause);
	}

	public ElidomInvalidStatusException(Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, cause.getMessage(), cause);
	}

	public ElidomInvalidStatusException(String message, Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, message, cause);
	}
	
	public ElidomInvalidStatusException(String message, List<String> params, Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, message, params, cause);
	}
}