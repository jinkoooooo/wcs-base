/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.exception.server;

import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import xyz.elidom.exception.ExceptionMessageConstants;

public class ElidomScriptRuntimeException extends ElidomServerException {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = -4704175840707826616L;

	private static final int STATUS = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
	private static final String TITLE_CODE = ExceptionMessageConstants.TITLE_SCRIPT_RUNTIME_ERROR;
	private static final String ERROR_CODE = ExceptionMessageConstants.SCRIPT_RUNTIME_ERROR;
	
	public ElidomScriptRuntimeException() {
		super(STATUS, TITLE_CODE, ERROR_CODE, ERROR_CODE , null, null);	
	}

	public ElidomScriptRuntimeException(String value) {
		super(STATUS, TITLE_CODE, value, value, null);
	}
	
	public ElidomScriptRuntimeException(String value, List<String> params) {
		super(STATUS, TITLE_CODE, value, value, params, null);
	}

	public ElidomScriptRuntimeException(String code, String message) {
		super(STATUS, TITLE_CODE, code, message, null);
	}

	public ElidomScriptRuntimeException(String code, String message, List<String> params) {
		super(STATUS, TITLE_CODE, code, message, params, null);
	}
	
	public ElidomScriptRuntimeException(String code, String message, Throwable cause) {
		super(STATUS, TITLE_CODE, code, message, cause);
	}
	
	public ElidomScriptRuntimeException(String code, String message, List<String> params, Throwable cause) {
		super(STATUS, TITLE_CODE, code, message, params, cause);
	}

	public ElidomScriptRuntimeException(Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, cause.getMessage(), cause);
	}

	public ElidomScriptRuntimeException(String message, Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, message, cause);
	}
	
	public ElidomScriptRuntimeException(String message, List<String> params, Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, message, params, cause);
	}
}