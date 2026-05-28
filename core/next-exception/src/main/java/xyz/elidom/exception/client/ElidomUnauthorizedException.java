/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.exception.client;

import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import xyz.elidom.exception.ExceptionMessageConstants;

/**
 * Unauthorized 예외 
 * 
 * @author shortstop
 */
public class ElidomUnauthorizedException extends ElidomClientException {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 2372045556564047160L;

	/**
	 * Error Code
	 */
	private static final int STATUS = HttpServletResponse.SC_UNAUTHORIZED;
	private static final String TITLE_CODE = ExceptionMessageConstants.NOT_AUTHORIZED_USER;
	private static final String ERROR_CODE = ExceptionMessageConstants.NOT_AUTHORIZED_USER;
	
	public ElidomUnauthorizedException() {
		super(STATUS, TITLE_CODE, ERROR_CODE, ERROR_CODE , null, null);	
	}

	public ElidomUnauthorizedException(String value) {
		super(STATUS, TITLE_CODE, value, value, null);
	}
	
	public ElidomUnauthorizedException(String value, List<String> params) {
		super(STATUS, TITLE_CODE, value, value, params, null);
	}

	public ElidomUnauthorizedException(String code, String message) {
		super(STATUS, TITLE_CODE, code, message, null);
	}

	public ElidomUnauthorizedException(String code, String message, List<String> params) {
		super(STATUS, TITLE_CODE, code, message, params, null);
	}
	
	public ElidomUnauthorizedException(String code, String message, Throwable cause) {
		super(STATUS, TITLE_CODE, code, message, cause);
	}
		
	public ElidomUnauthorizedException(String code, String message, List<String> params, Throwable cause) {
		super(STATUS, TITLE_CODE, code, message, params, cause);
	}

	public ElidomUnauthorizedException(Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, cause.getMessage(), cause);
	}

	public ElidomUnauthorizedException(String message, Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, message, cause);
	}
	
	public ElidomUnauthorizedException(String message, List<String> params, Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, message, params, cause);
	}
}
