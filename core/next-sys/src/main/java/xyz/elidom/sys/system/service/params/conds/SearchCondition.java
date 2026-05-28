/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.params.conds;

/**
 * 조회 조건
 * 
 * @author shortstop
 */
public class SearchCondition {

	/**
	 * 조인 쿼리의 경우 조인 테이블 명
	 */
	private String refName;
	/**
	 * 쿼리 필드 명
	 */
	private String name;
	/**
	 * 쿼리 필드 타입
	 */
	private String type;
	/**
	 * 쿼리 Operator
	 */
	private String operator = "eq";
	/**
	 * 조건 값
	 */
	private Object value;
	
	public SearchCondition() {
	}
	
	public SearchCondition(String name, String operator, Object value) {
		this.name = name;
		this.operator = operator;
		this.value = value;
	}
	
	public SearchCondition(String refName, String name, String type, String operator, Object value) {
		this.refName = refName;
		this.name = name;
		this.type = type;
		this.operator = operator;
		this.value = value;
	}
	
	public String getRefName() {
		return refName;
	}

	public void setRefName(String refName) {
		this.refName = refName;
	}

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

	public String getOperator() {
		return operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

}
