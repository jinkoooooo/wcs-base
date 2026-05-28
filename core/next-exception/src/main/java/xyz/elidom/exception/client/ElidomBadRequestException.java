/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.exception.client;

import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import xyz.elidom.exception.ExceptionMessageConstants;

/**
 * 일반적인 클라이언트의 잘못된 요청으로 인한 예외 정의
 * 
 * @author shortstop
 */
public class ElidomBadRequestException extends ElidomClientException {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 2372045556564047160L;

	/**
	 * Error Code
	 */
	private static final int STATUS = HttpServletResponse.SC_BAD_REQUEST;
	private static final String TITLE_CODE = ExceptionMessageConstants.TITLE_BAD_REQUEST;
	private static final String ERROR_CODE = ExceptionMessageConstants.BAD_REQUEST;
	
	public ElidomBadRequestException() {
		super(STATUS, TITLE_CODE, ERROR_CODE, ERROR_CODE , null, null);	
	}

	public ElidomBadRequestException(String value) {
		super(STATUS, TITLE_CODE, value, value, null);
	}
	
	public ElidomBadRequestException(String value, List<String> params) {
		super(STATUS, TITLE_CODE, value, value, params, null);
	}

	public ElidomBadRequestException(String code, String message) {
		super(STATUS, TITLE_CODE, code, message, null);
	}

	public ElidomBadRequestException(String code, String message, List<String> params) {
		super(STATUS, TITLE_CODE, code, message, params, null);
	}
	
	public ElidomBadRequestException(String code, String message, Throwable cause) {
		super(STATUS, TITLE_CODE, code, message, cause);
	}
		
	public ElidomBadRequestException(String code, String message, List<String> params, Throwable cause) {
		super(STATUS, TITLE_CODE, code, message, params, cause);
	}

	public ElidomBadRequestException(Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, cause.getMessage(), cause);
	}

	public ElidomBadRequestException(String message, Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, message, cause);
	}
	
	public ElidomBadRequestException(String message, List<String> params, Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, message, params, cause);
	}
}