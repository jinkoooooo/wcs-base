/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.exception.client;

import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import xyz.elidom.exception.ExceptionMessageConstants;

/**
 * Request의 JSON Parameter를 Input Object로 변환시 예외
 * 
 * @author shortstop
 */
public class ElidomInputException extends ElidomClientException {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = -185976125718245529L;

	/**
	 * Error Code
	 */
	private static final int STATUS = HttpServletResponse.SC_BAD_REQUEST;
	private static final String TITLE_CODE = ExceptionMessageConstants.TITLE_PARSE_INPUT_ERROR;
	private static final String ERROR_CODE = ExceptionMessageConstants.PARSE_INPUT_ERROR;
	
	public ElidomInputException() {
		super(STATUS, TITLE_CODE, ERROR_CODE, ERROR_CODE , null, null);	
	}

	public ElidomInputException(String value) {
		super(STATUS, TITLE_CODE, value, value, null);
	}
	
	public ElidomInputException(String value, List<String> params) {
		super(STATUS, TITLE_CODE, value, value, params, null);
	}

	public ElidomInputException(String code, String message) {
		super(STATUS, TITLE_CODE, code, message, null);
	}

	public ElidomInputException(String code, String message, List<String> params) {
		super(STATUS, TITLE_CODE, code, message, params, null);
	}
	
	public ElidomInputException(String code, String message, Throwable cause) {
		super(STATUS, TITLE_CODE, code, message, cause);
	}
	
	public ElidomInputException(String code, String message, List<String> params, Throwable cause) {
		super(STATUS, TITLE_CODE, code, message, params, cause);
	}

	public ElidomInputException(Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, cause.getMessage(), cause);
	}

	public ElidomInputException(String message, Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, message, cause);
	}
	
	public ElidomInputException(String message, List<String> params, Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, message, params, cause);
	}
}