/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.exception.server;

import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import xyz.elidom.exception.ExceptionMessageConstants;

/**
 * Service 실행시를 제외하고 런타임에 발생하는 모든 예외는 최종적으로 ElidomRuntimeException으로 처리된다.
 * 
 * @author shortstop
 */
public class ElidomRuntimeException extends ElidomServerException {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = -4708115042674602307L;

	/**
	 * Error Code
	 */
	private static final int STATUS = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
	private static final String TITLE_CODE = ExceptionMessageConstants.TITLE_RUNTIME_ERROR;
	private static final String ERROR_CODE = ExceptionMessageConstants.RUNTIME_ERROR;
	
	public ElidomRuntimeException() {
		super(STATUS, TITLE_CODE, ERROR_CODE, ERROR_CODE , null, null);	
	}

	public ElidomRuntimeException(String value) {
		super(STATUS, TITLE_CODE, value, value, null);
	}

	public ElidomRuntimeException(String value, List<String> params) {
		super(STATUS, TITLE_CODE, value, value, params, null);
	}

	public ElidomRuntimeException(String code, String message) {
		super(STATUS, TITLE_CODE, code, message, null);
	}

	public ElidomRuntimeException(String code, String message, List<String> params) {
		super(STATUS, TITLE_CODE, code, message, params, null);
	}
	
	public ElidomRuntimeException(String code, String message, Throwable cause) {
		super(STATUS, TITLE_CODE, code, message, cause);
	}
	
	public ElidomRuntimeException(String code, String message, List<String> params, Throwable cause) {
		super(STATUS, TITLE_CODE, code, message, params, cause);
	}

	public ElidomRuntimeException(Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, cause.getMessage(), cause);
	}

	public ElidomRuntimeException(String message, Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, message, cause);
	}
	
	public ElidomRuntimeException(String message, List<String> params, Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, message, params, cause);
	}
}