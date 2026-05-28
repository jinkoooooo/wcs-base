/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.exception;

import java.text.MessageFormat;
import java.util.List;

/**
 * Elidom 최상위 Exception
 * 기본적으로 Client에서는 title, code, message까지만 보여주고 상세보기시에 나머지 디테일 보여준다.
 * 기본 title은 다국어 처리가 안 되고 next-core 라이브러리를 사용하는 경우에 code에 따라 다국어 처리가 된다.
 * (title 다국어 처리는 language 설정값을 가져와야 하므로 Request가 필요할 듯 하여 ExceptionHandler에서 처리하도록 한다.)
 * message는 다국어 처리하지 않고 에러 내용을 간단히 알려주고 싶을 때 사용한다.
 * 
 * @author shortstop
 */
public class ElidomException extends RuntimeException {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = -3865890444633025221L;
	/**
	 * Response Status Code
	 */
	private int status;
	/**
	 * Error Title - For End User
	 */
	private String title;

	/**
	 * User message - To Display message for End User
	 */
	private String userMessage;
	/**
	 * Error Code
	 */
	private String code;

	/**
	 * Message Parameters
	 */
	private List<String> params;

	/**
	 * Exception 타입에 따라 에러 정보를 저장할 것인지 여부
	 * 이 옵션은 next-core 라이브러리 사용시 의미가 있다.
	 */
	private boolean writable = true;

	/**
	 * Constructor1
	 * 
	 * @param status
	 * @param code
	 * @param cause
	 */
	public ElidomException(int status, String title, String code, Throwable cause) {
		this(status, title, code, cause.getMessage(), cause);
	}

	/**
	 * Constructor2
	 * 
	 * @param status
	 * @param title
	 * @param code
	 * @param params
	 * @param cause
	 */
	public ElidomException(int status, String title, String code, List<String> params, Throwable cause) {
		this(status, title, code, cause.getMessage(), cause);
	}

	/**
	 * Constructor3
	 * 
	 * @param status
	 * @param title
	 * @param code
	 * @param message
	 * @param cause
	 */
	public ElidomException(int status, String title, String code, String message, Throwable cause) {
		this(status, title, code, message, null, cause);
	}

	/**
	 * Constructor4
	 * 
	 * @param status
	 * @param title
	 * @param code
	 * @param message
	 * @param params
	 * @param cause
	 */
	public ElidomException(int status, String title, String code, String message, List<String> params, Throwable cause) {
		this(status, title, code, message, params, cause, true);
	}

	/**
	 * Constructor5
	 * 
	 * @param status
	 * @param title
	 * @param code
	 * @param message
	 * @param params
	 * @param cause
	 * @param writableStackTrace
	 */
	public ElidomException(int status, String title, String code, String message, List<String> params, Throwable cause, boolean writableStackTrace) {
		super((message == null ? (cause != null ? cause.getMessage() : code) : parseMessge(message, params)), cause, false, writableStackTrace);

		this.setStatus(status);
		this.setCode(code);
		this.setTitle(title);

		if (params == null && cause instanceof ElidomException) {
			ElidomException elidomException = (ElidomException) cause;
			params = elidomException.getParams();
		}

		this.setParams(params);
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getUserMessage() {
		return userMessage;
	}

	public void setUserMessage(String message) {
		this.userMessage = message;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public List<String> getParams() {
		return params;
	}

	public void setParams(List<String> params) {
		this.params = params;
	}

	public boolean isWritable() {
		return writable;
	}

	public void setWritable(boolean writable) {
		this.writable = writable;
	}

	private static String parseMessge(String message, List<String> params) {
		return (params == null || params.isEmpty()) ? message : MessageFormat.format(message, params.toArray());
	}
}