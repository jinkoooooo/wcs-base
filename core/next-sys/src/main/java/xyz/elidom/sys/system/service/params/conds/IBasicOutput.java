/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.params.conds;

/**
 * 서비스 실행 결과에 대한 단순 Output : 성공 여부와 메시지 정보만 리턴
 * 
 * @author shortstop
 */
public interface IBasicOutput {

	public boolean isSuccess();

	public void setSuccess(boolean success);

	public String getCode();

	public void setCode(String code);

	public String getMsg();

	public void setMsg(String msg);
	
}
