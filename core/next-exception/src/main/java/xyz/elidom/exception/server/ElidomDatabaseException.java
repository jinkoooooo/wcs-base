/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.exception.server;

import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import xyz.elidom.exception.ExceptionMessageConstants;

/**
 * IQueryManager에서 발생하는 데이터베이스 관련 예외
 * 
 * @author shortstop
 */
public class ElidomDatabaseException extends ElidomServerException {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = -6424847830980355555L;
	
	/**
	 * Error Code
	 */
	private static final int STATUS = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
	private static final String TITLE_CODE = ExceptionMessageConstants.TITLE_DATA_BASE_ERROR;
	private static final String ERROR_CODE = ExceptionMessageConstants.DATA_BASE_ERROR;
	
	public ElidomDatabaseException() {
		super(STATUS, TITLE_CODE, ERROR_CODE, ERROR_CODE , null, null);	
	}

	public ElidomDatabaseException(String value) {
		super(STATUS, TITLE_CODE, value, value, null);
	}
	
	public ElidomDatabaseException(String value, List<String> params) {
		super(STATUS, TITLE_CODE, value, value, params, null);
	}

	public ElidomDatabaseException(String code, String message) {
		super(STATUS, TITLE_CODE, code, message, null);
	}

	public ElidomDatabaseException(String code, String message, List<String> params) {
		super(STATUS, TITLE_CODE, code, message, params, null);
	}
	
	public ElidomDatabaseException(String code, String message, Throwable cause) {
		super(STATUS, TITLE_CODE, code, message, cause);
	}
	
	public ElidomDatabaseException(String code, String message, List<String> params, Throwable cause) {
		super(STATUS, TITLE_CODE, code, message, params, cause);
	}

	public ElidomDatabaseException(Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, cause.getMessage(), cause);
	}

	public ElidomDatabaseException(String message, Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, message, cause);
	}
	
	public ElidomDatabaseException(String message, List<String> params, Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, message, params, cause);
	}
}