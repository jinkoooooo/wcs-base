/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.params;

import xyz.elidom.util.ValueUtil;

/**
 * 에러가 발생했을 경우에만 사용하는 Output. Exception Handler가 다룸.
 * 
 * @author shortstop
 */
public class ErrorOutput extends BasicOutput {

	/**
	 * Response Status
	 */
	private int status;
	/**
	 * Error ID
	 */
	private String errorId;
	/**
	 * Detail Message
	 */
	private String detail;
	
	public ErrorOutput() {
		this.setSuccess(false);
	}

	public ErrorOutput(int status, String code, String msg, String errorId) {
		this.status = status;
		this.errorId = errorId;
		this.setSuccess(false);
		this.setMsg(msg);
		this.setCode(code);
	}
	
	public ErrorOutput(int status, String errorId, String code, String msg, String detail) {
		this.status = status;
		this.errorId = errorId;
		this.setSuccess(false);
		this.setMsg(ValueUtil.isNotEmpty(msg) ? msg : detail);
		this.setCode(code);
		// this.setDetail(detail);
	}
	
	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getErrorId() {
		return errorId;
	}

	public void setErrorId(String errorId) {
		this.errorId = errorId;
	}
	
	public String getDetail() {
		return this.detail;
	}
	
	public void setDetail(String detail) {
		this.detail = detail;
	}

//	/**
//	 * Create Error Ouput For OI
//	 * 
//	 * @param errorId
//	 * @param msg
//	 * @param code
//	 * @param codeArgs
//	 * @return
//	 */
//	public static ErrorOutput createI18nError(String errorId, String msg, String code, String... codeArgs) {
//		ErrorOutput output = new ErrorOutput(400, code, msg, errorId);
//		String message = Message.getMessageByParamsForLocale(code, msg, codeArgs);
//		output.setMsg(message);
//		return output;
//	}
}
