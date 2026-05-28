/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.exception.client;

import java.util.List;

import xyz.elidom.exception.ElidomException;

/**
 * Client Client Fault - HTTP 400번대 에러에 대응 
 * 
 * @author shortstop
 */
public class ElidomClientException extends ElidomException {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = -5184363332678706081L;
	
	public ElidomClientException(int status, String title, String code, Throwable cause) {
		this(status, title, code, (cause != null ? cause.getMessage() : title), cause);
	}
	
	public ElidomClientException(int status, String title, String code, List<String> params, Throwable cause) {
		this(status, title, code, (cause != null ? cause.getMessage() : title), params, cause);
	}

	public ElidomClientException(int status, String title, String code, String message, Throwable cause) {
		super(status, title, code, message, cause);
	}
	
	public ElidomClientException(int status, String title, String code, String message, List<String> params, Throwable cause) {
		super(status, title, code, message, params, cause);
	}
}