/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.exception.server;

import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import xyz.elidom.exception.ExceptionMessageConstants;

/**
 * 서비스 실행 결과 Object를 Output JSON String으로 변환시 예외
 * 
 * @author shortstop
 */
public class ElidomOutputException extends ElidomServerException {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = -2226004003230297478L;

	/**
	 * Error Code
	 */
	private static final int STATUS = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
	private static final String TITLE_CODE = ExceptionMessageConstants.TITLE_PARSE_OUTPUT_ERROR;
	private static final String ERROR_CODE = ExceptionMessageConstants.PARSE_OUTPUT_ERROR;
	
	public ElidomOutputException() {
		super(STATUS, TITLE_CODE, ERROR_CODE, ERROR_CODE , null, null);	
	}

	public ElidomOutputException(String value) {
		super(STATUS, TITLE_CODE, value, value, null);
	}

	public ElidomOutputException(String value, List<String> params) {
		super(STATUS, TITLE_CODE, value, value, params, null);
	}

	public ElidomOutputException(String code, String message) {
		super(STATUS, TITLE_CODE, code, message, null);
	}

	public ElidomOutputException(String code, String message, List<String> params) {
		super(STATUS, TITLE_CODE, code, message, params, null);
	}
	
	public ElidomOutputException(String code, String message, Throwable cause) {
		super(STATUS, TITLE_CODE, code, message, cause);
	}
	
	public ElidomOutputException(String code, String message, List<String> params, Throwable cause) {
		super(STATUS, TITLE_CODE, code, message, params, cause);
	}

	public ElidomOutputException(Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, cause.getMessage(), cause);
	}

	public ElidomOutputException(String message, Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, message, cause);
	}
	
	public ElidomOutputException(String message, List<String> params, Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, message, params, cause);
	}
}