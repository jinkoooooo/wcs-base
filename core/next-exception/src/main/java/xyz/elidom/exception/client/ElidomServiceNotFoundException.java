/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.exception.client;

import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import xyz.elidom.exception.ExceptionMessageConstants;

/**
 * 클라이언트에서 요청한 서비스가 없는 경우, 즉 클라이언트에서 잘못된 서비스 URL로 요청한 경우.
 * 
 * @author shortstop
 */
public class ElidomServiceNotFoundException extends ElidomClientException {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = -1945660787658404060L;

	/**
	 * Error Code
	 */
	private static final int STATUS = HttpServletResponse.SC_NOT_FOUND;
	private static final String TITLE_CODE = ExceptionMessageConstants.TITLE_SERVICE_NOT_FOUND;
	private static final String ERROR_CODE = ExceptionMessageConstants.SERVICE_NOT_FOUND;
	
	public ElidomServiceNotFoundException() {
		super(STATUS, TITLE_CODE, ERROR_CODE, ERROR_CODE , null, null);	
	}

	public ElidomServiceNotFoundException(String value) {
		super(STATUS, TITLE_CODE, value, value, null);
	}
	
	public ElidomServiceNotFoundException(String value, List<String> params) {
		super(STATUS, TITLE_CODE, value, value, params, null);
	}

	public ElidomServiceNotFoundException(String code, String message) {
		super(STATUS, TITLE_CODE, code, message, null);
	}

	public ElidomServiceNotFoundException(String code, String message, List<String> params) {
		super(STATUS, TITLE_CODE, code, message, params, null);
	}
	
	public ElidomServiceNotFoundException(String code, String message, Throwable cause) {
		super(STATUS, TITLE_CODE, code, message, cause);
	}
	
	public ElidomServiceNotFoundException(String code, String message, List<String> params, Throwable cause) {
		super(STATUS, TITLE_CODE, code, message, params, cause);
	}

	public ElidomServiceNotFoundException(Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, cause.getMessage(), cause);
	}

	public ElidomServiceNotFoundException(String message, Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, message, cause);
	}
	
	public ElidomServiceNotFoundException(String message, List<String> params, Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, message, params, cause);
	}
}