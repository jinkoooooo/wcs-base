package xyz.elidom.exception.server;

import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import xyz.elidom.exception.ElidomException;
import xyz.elidom.exception.ExceptionMessageConstants;

public class ElidomLicenseException extends ElidomException {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 8827357876670462164L;
	
	private static final int STATUS = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
	private static final String TITLE_CODE = ExceptionMessageConstants.TITLE_SERVICE_ERROR;
	private static final String ERROR_CODE = ExceptionMessageConstants.SERVICE_ERROR;

	public ElidomLicenseException() {
		super(STATUS, TITLE_CODE, ERROR_CODE, ERROR_CODE , null, null);	
	}

	public ElidomLicenseException(String value) {
		super(STATUS, TITLE_CODE, value, value, null);
	}

	public ElidomLicenseException(String value, List<String> params) {
		super(STATUS, TITLE_CODE, value, value, params, null);
	}
	
	public ElidomLicenseException(String value, boolean writableStackTrace) {
		super(STATUS, TITLE_CODE, ERROR_CODE, value, null, null, writableStackTrace);
	}
	
	public ElidomLicenseException(String code, String value, boolean writableStackTrace) {
		super(STATUS, TITLE_CODE, code, value, null, null, writableStackTrace);
	}

	public ElidomLicenseException(String code, String message) {
		super(STATUS, TITLE_CODE, code, message, null);
	}

	public ElidomLicenseException(String code, String message, List<String> params) {
		super(STATUS, TITLE_CODE, code, message, params, null);
	}
	
	public ElidomLicenseException(String code, String message, Throwable cause) {
		super(STATUS, TITLE_CODE, code, message, cause);
	}
	
	public ElidomLicenseException(String code, String message, List<String> params, Throwable cause) {
		super(STATUS, TITLE_CODE, code, message, params, cause);
	}

	public ElidomLicenseException(Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, cause.getMessage(), cause);
	}

	public ElidomLicenseException(String message, Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, message, cause);
	}
	
	public ElidomLicenseException(String message, List<String> params, Throwable cause) {
		super(STATUS, TITLE_CODE, ERROR_CODE, message, params, cause);
	}
}
