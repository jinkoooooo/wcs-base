/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.exception.server;

import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import xyz.elidom.exception.ExceptionMessageConstants;

/**
 * Service 실행시 발생하는 모든 예외는 최종적으로 ElidomServiceException으로 처리된다.
 * 
 * @author shortstop
 */
public class ElidomServiceException extends ElidomServerException {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 1948844590648308814L;
	
	private static final int STATUS = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
	private static final String TITLE_CODE = ExceptionMessageConstants.TITLE_SERVICE_ERROR;
	private static final String ERROR_CODE = ExceptionMessageConstants.SERVICE_ERROR;
	
	public ElidomServiceException() {
		super(STATUS, TITLE_CODE, ERROR_CODE, ERROR_CODE , null, null);	
	}

	public ElidomServiceException(String value) {
		super(STATUS, TITLE_CODE, value, value, null);
	}

	public ElidomServiceException(String value, List<String> params) {
		super(STATUS, TITLE_CODE, value, value, params, null);
	}

	public ElidomServiceException(String code, String message) {
		super(STATUS, TITLE_CODE, code, message, null);
	}

	public ElidomServiceException(String code, String message, List<String> params) {
		super(STATUS, TITLE_CODE, code, message, params, null);
	}
	
	public ElidomServiceException(String code, String message, Throwable cause) {
		super(STATUS, TITLE_CODE, code, message, cause);
	}
	
	public ElidomServiceException(String code, String message, List<String> params, Throwable cause) {
		super(STATUS, TITLE_CODE, code, message, params, cause);
	}

	public ElidomServiceException(Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, cause.getMessage(), cause);
	}

	public ElidomServiceException(String message, Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, message, cause);
	}
	
	public ElidomServiceException(String message, List<String> params, Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, message, params, cause);
	}
}