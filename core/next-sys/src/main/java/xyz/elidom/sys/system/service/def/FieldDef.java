/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.def;

import java.util.List;

/**
 * Data Field 정의
 * 
 * @author shortstop
 */
public class FieldDef {

	/**
	 * 데이터 필드 명
	 */
	private String name;
	/**
	 * 데이터 타입 명
	 */
	private String type;
	/**
	 * 데이터 타입이 오브젝트 형식일 경우 즉 Primitive 타입이 아닐 경우 서브 데이터 필드 리스트를 정의
	 * Primitive Type인 경우 subFieldList는 존재하지 않음
	 */
	private List<FieldDef> subFieldList;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public List<FieldDef> getSubFieldList() {
		return subFieldList;
	}

	public void setSubFieldList(List<FieldDef> subFieldList) {
		this.subFieldList = subFieldList;
	}

}
