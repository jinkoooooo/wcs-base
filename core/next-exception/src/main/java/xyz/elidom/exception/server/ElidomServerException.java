/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.exception.server;

import java.util.List;

import xyz.elidom.exception.ElidomException;

/**
 * Server Side Fault - HTTP 500번대 에러에 대응 
 * 
 * @author shortstop
 */
public class ElidomServerException extends ElidomException {
	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 8954591201513783533L;

	public ElidomServerException(int status, String title, String code, Throwable cause) {
		this(status, title, code, (cause == null ? title : cause.getMessage()), cause);
	}
	
	public ElidomServerException(int status, String title, String code, List<String> params, Throwable cause) {
		this(status, title, code, (cause == null ? title : cause.getMessage()), params, cause);
	}
	
	public ElidomServerException(int status, String title, String code, String message, Throwable cause) {
		super(status, title, code, message, cause);
	}
	
	public ElidomServerException(int status, String title, String code, String message, List<String> params, Throwable cause) {
		super(status, title, code, message, params, cause);
	}
}