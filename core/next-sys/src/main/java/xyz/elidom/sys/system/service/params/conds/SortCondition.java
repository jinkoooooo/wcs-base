/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.system.service.params.conds;

/**
 * 소팅 조건
 * 
 * @author shortstop
 */
public class SortCondition {

	/**
	 * 조인 쿼리의 경우 조인 테이블 명
	 */
	private String refName;
	/**
	 * 소팅 필드 명
	 */
	private String name;
	/**
	 * ASC / DESC 여부 - 'ASC' or 'DESC'
	 */
	private String dir;
	
	public SortCondition() {
	}
	
	public SortCondition(String name, String dir) {
		this.name = name;
		this.dir = dir;
	}
	
	public SortCondition(String refName, String name, String dir) {
		this.refName = refName;
		this.name = name;
		this.dir = dir;
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

	/**
	 * @return the dir
	 */
	public String getDir() {
		return dir;
	}

	/**
	 * @param dir the dir to set
	 */
	public void setDir(String dir) {
		this.dir = dir;
	}

}
