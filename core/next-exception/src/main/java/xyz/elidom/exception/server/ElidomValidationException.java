/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.exception.server;

import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import xyz.elidom.exception.ExceptionMessageConstants;

/**
 * Validation Error
 * 
 * @author lyonghwan
 */
public class ElidomValidationException extends ElidomServerException {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 6008154685108624769L;

	/**
	 * Error Code
	 */
	private static final int STATUS = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
	private static final String TITLE_CODE = ExceptionMessageConstants.TITLE_VALIDATION_ERROR;
	private static final String ERROR_CODE = ExceptionMessageConstants.VALIDATION_ERROR;
	
	public ElidomValidationException() {
		super(STATUS, TITLE_CODE, ERROR_CODE, ERROR_CODE , null, null);	
	}

	public ElidomValidationException(String value) {
		super(STATUS, TITLE_CODE, value, value, null);
	}

	public ElidomValidationException(String value, List<String> params) {
		super(STATUS, TITLE_CODE, value, value, params, null);
	}

	public ElidomValidationException(String code, String message) {
		super(STATUS, TITLE_CODE, code, message, null);
	}

	public ElidomValidationException(String code, String message, List<String> params) {
		super(STATUS, TITLE_CODE, code, message, params, null);
	}
	
	public ElidomValidationException(String code, String message, Throwable cause) {
		super(STATUS, TITLE_CODE, code, message, cause);
	}
	
	public ElidomValidationException(String code, String message, List<String> params, Throwable cause) {
		super(STATUS, TITLE_CODE, code, message, params, cause);
	}

	public ElidomValidationException(Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, cause.getMessage(), cause);
	}

	public ElidomValidationException(String message, Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, message, cause);
	}
	
	public ElidomValidationException(String message, List<String> params, Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, message, params, cause);
	}
}