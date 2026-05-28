/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.params;

import xyz.elidom.sys.system.service.params.conds.IBasicInput;

/**
 * ID 하나만 받아서 조회나 트랜잭션을 일으키는 서비스의 Input / Output - ex) Exist, Find One, Delete
 * 등의 API 서비스 실행 결과에 대한 단순 Output : 성공 여부와 결과 존재 혹은 실행 여부(Exist, Delete), 메시지
 * 정보만 리턴
 * 
 * @author shortstop
 */
public class BasicInOut extends BasicOutput implements IBasicInput {

	/**
	 * ID - Input
	 */
	private String id;
	/**
	 * 실행 결과 - Output : Exist 메소드의 경우 존재 여부, Delete 메소드의 경우 삭제 여부 등
	 */
	private boolean result;

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	public boolean isResult() {
		return result;
	}

	public void setResult(boolean result) {
		this.result = result;
	}

}
