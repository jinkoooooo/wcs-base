/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.exception.client;

import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import xyz.elidom.exception.ExceptionMessageConstants;

/**
 * 클라이언트에서 요청한 아이디로 레코드를 조회한 결과 존재하지 않는 경우
 * 
 * @author shortstop
 */
public class ElidomRecordNotFoundException extends ElidomClientException {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = -2093947144289514633L;

	/**
	 * Error Code
	 */
	private static final int STATUS = HttpServletResponse.SC_NOT_FOUND;
	private static final String TITLE_CODE = ExceptionMessageConstants.TITLE_RECORD_NOT_FOUND;
	private static final String ERROR_CODE = ExceptionMessageConstants.RECORD_NOT_FOUND;
	
	public ElidomRecordNotFoundException() {
		super(STATUS, TITLE_CODE, ERROR_CODE, ERROR_CODE , null, null);	
	}
	
	public ElidomRecordNotFoundException(String value) {
		super(STATUS, TITLE_CODE, value, value, null);
	}
	
	public ElidomRecordNotFoundException(String value, List<String> params) {
		super(STATUS, TITLE_CODE, value, value, params, null);
	}

	public ElidomRecordNotFoundException(String code, String message) {
		super(STATUS, TITLE_CODE, code, message, null);
	}

	public ElidomRecordNotFoundException(String code, String message, List<String> params) {
		super(STATUS, TITLE_CODE, code, message, params, null);
	}
	
	public ElidomRecordNotFoundException(String code, String message, Throwable cause) {
		super(STATUS, TITLE_CODE, code, message, cause);
	}
	
	public ElidomRecordNotFoundException(String code, String message, List<String> params, Throwable cause) {
		super(STATUS, TITLE_CODE, code, message, params, cause);
	}

	public ElidomRecordNotFoundException(Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, cause.getMessage(), cause);
	}

	public ElidomRecordNotFoundException(String message, Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, message, cause);
	}
	
	public ElidomRecordNotFoundException(String message, List<String> params, Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, message, params, cause);
	}
}