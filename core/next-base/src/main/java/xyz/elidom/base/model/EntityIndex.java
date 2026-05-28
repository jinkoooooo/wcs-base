/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.base.model;

import java.util.Map;

import xyz.elidom.sys.util.SysValueUtil;

/**
 * Table Index 모델 
 * 
 * @author shortstop
 */
public class EntityIndex {
	/**
	 * 테이블 명 
	 */
	private String tableName;
	/**
	 * 테이블 인덱스 명 
	 */
	private String tableIdxName;
	/**
	 * 테이블 Unique Index 여부 
	 */
	private boolean tableIdxUnique;
	/**
	 * 테이블 인덱스 필드 리스트 : ','로 구분되는 필드 리스트 
	 */
	private String tableIdxFields;
	/**
	 * 엔티티 명 
	 */
	private String entityName;
	/**
	 * 엔티티 인덱스 명 
	 */
	private String entityIdxName;
	/**
	 * 엔티티 인덱스 Unique 여부 
	 */
	private boolean entityIdxUnique;
	/**
	 * 엔티티 인덱스 필드 리스트 : ','로 구분되는 필드 리스트 
	 */
	private String entityIdxFields;	
	/**
	 * 테이블 - 엔티티 인덱스가 다른 지 비교 
	 */
	private boolean different;
	
	/**
	 * 생성자 
	 */
	public EntityIndex() {
	}

	/**
	 * @return the tableName
	 */
	public String getTableName() {
		return tableName;
	}

	/**
	 * @param tableName the tableName to set
	 */
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	/**
	 * @return the tableIdxName
	 */
	public String getTableIdxName() {
		return tableIdxName;
	}

	/**
	 * @param tableIdxName the tableIdxName to set
	 */
	public void setTableIdxName(String tableIdxName) {
		this.tableIdxName = tableIdxName;
	}

	/**
	 * @return the tableIdxUnique
	 */
	public boolean getTableIdxUnique() {
		return tableIdxUnique;
	}

	/**
	 * @param tableIdxUnique the tableIdxUnique to set
	 */
	public void setTableIdxUnique(boolean tableIdxUnique) {
		this.tableIdxUnique = tableIdxUnique;
	}

	/**
	 * @return the tableIdxFields
	 */
	public String getTableIdxFields() {
		return tableIdxFields;
	}

	/**
	 * @param tableIdxFields the tableIdxFields to set
	 */
	public void setTableIdxFields(String tableIdxFields) {
		this.tableIdxFields = tableIdxFields;
	}

	/**
	 * @return the entityName
	 */
	public String getEntityName() {
		return entityName;
	}

	/**
	 * @param entityName the entityName to set
	 */
	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}

	/**
	 * @return the entityIdxName
	 */
	public String getEntityIdxName() {
		return entityIdxName;
	}

	/**
	 * @param entityIdxName the entityIdxName to set
	 */
	public void setEntityIdxName(String entityIdxName) {
		this.entityIdxName = entityIdxName;
	}

	/**
	 * @return the entityIdxUnique
	 */
	public boolean getEntityIdxUnique() {
		return entityIdxUnique;
	}

	/**
	 * @param entityIdxUnique the entityIdxUnique to set
	 */
	public void setEntityIdxUnique(boolean entityIdxUnique) {
		this.entityIdxUnique = entityIdxUnique;
	}

	/**
	 * @return the entityIdxFields
	 */
	public String getEntityIdxFields() {
		return entityIdxFields;
	}

	/**
	 * @param entityIdxFields the entityIdxFields to set
	 */
	public void setEntityIdxFields(String entityIdxFields) {
		this.entityIdxFields = entityIdxFields;
	}

	/**
	 * @return the different
	 */
	public boolean getDifferent() {
		return different;
	}

	/**
	 * @param different the different to set
	 */
	public void setDifferent(boolean different) {
		this.different = different;
	}
	
	/**
	 * 테이블 인덱스 내용과 엔티티 인덱스 내용이 다른 점이 있는지 비교 
	 */
	public void checkDifference() {
		if(SysValueUtil.isEqualIgnoreCase(this.tableIdxName, this.entityIdxName)) {
			if(SysValueUtil.isEqual(this.tableIdxUnique,  this.entityIdxUnique)) {
				if(SysValueUtil.isEqualIgnoreCase(this.tableIdxFields, this.entityIdxFields)) {
					this.different = false;
					return;
				}
			}
		}
		
		this.different = false;
	}
	
	public Map<String, Object> toMapByEntity() {
		return SysValueUtil.newMap("name,unique,columnList", this.entityIdxName, this.entityIdxUnique, this.entityIdxFields);
	}
	
	public Map<String, Object> toMapByTable() {
		return SysValueUtil.newMap("name,unique,columnList", this.tableIdxName, this.tableIdxUnique, this.tableIdxFields);
	}
}
