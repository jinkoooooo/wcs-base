/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.exception.server;

import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import xyz.elidom.exception.ExceptionMessageConstants;

/**
 * 생성하려는 Data가 이미 존재하는 경우의 예외
 * 
 * @author shortstop
 */
public class ElidomAlreadyExistException extends ElidomServerException {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 2114045556564047160L;

	/**
	 * Error Code
	 */
	private static final int STATUS = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
	private static final String TITLE_CODE = ExceptionMessageConstants.TITLE_ALREADY_EXIST;
	private static final String ERROR_CODE = ExceptionMessageConstants.ALREADY_EXIST;
	
	public ElidomAlreadyExistException() {
		super(STATUS, TITLE_CODE, ERROR_CODE, ERROR_CODE , null, null);	
	}

	public ElidomAlreadyExistException(String value) {
		super(STATUS, TITLE_CODE, value, value, null);
	}
	
	public ElidomAlreadyExistException(String value, List<String> params) {
		super(STATUS, TITLE_CODE, value, value, params, null);
	}

	public ElidomAlreadyExistException(String code, String message) {
		super(STATUS, TITLE_CODE, code, message, null);
	}

	public ElidomAlreadyExistException(String code, String message, List<String> params) {
		super(STATUS, TITLE_CODE, code, message, params, null);
	}
	
	public ElidomAlreadyExistException(String code, String message, Throwable cause) {
		super(STATUS, TITLE_CODE, code, message, cause);
	}
	
	public ElidomAlreadyExistException(String code, String message, List<String> params, Throwable cause) {
		super(STATUS, TITLE_CODE, code, message, params, cause);
	}

	public ElidomAlreadyExistException(Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, cause.getMessage(), cause);
	}

	public ElidomAlreadyExistException(String message, Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, message, cause);
	}
	
	public ElidomAlreadyExistException(String message, List<String> params, Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, message, params, cause);
	}
}