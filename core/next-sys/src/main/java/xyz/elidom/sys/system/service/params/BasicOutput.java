/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.params;

import xyz.elidom.sys.system.service.params.conds.IBasicOutput;

/**
 * 서비스 실행 결과에 대한 단순 Output : 성공 여부와 메시지 정보만 리턴
 * 
 * @author shortstop
 */
public class BasicOutput implements IBasicOutput {

	/**
	 * 서비스 실행 성공 여부
	 */
	protected boolean success = true;
	/**
	 * 서비스 실행 메시지 코드
	 */
	protected String code;
	/**
	 * 서비스 실행 메시지 설명 - code로 부터 다국어 처리
	 */
	private String msg;

	public BasicOutput() {
	}

	public BasicOutput(String code) {
		this(true, code, null);
	}

	public BasicOutput(String code, String msg) {
		this(true, code, msg);
	}

	public BasicOutput(boolean success, String code) {
		this(success, code, null);
	}

	public BasicOutput(boolean success, String code, String msg) {
		this.success = success;
		this.code = code;
		this.msg = msg;
	}

	@Override
	public boolean isSuccess() {
		return this.success;
	}

	@Override
	public void setSuccess(boolean success) {
		this.success = success;
	}

	@Override
	public String getCode() {
		return this.code;
	}

	@Override
	public void setCode(String code) {
		this.code = code;
	}

	@Override
	public String getMsg() {
		return this.msg;
	}

	@Override
	public void setMsg(String msg) {
		this.msg = msg;
	}

}
