/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.base.model;

import java.util.Map;

import xyz.elidom.base.entity.ResourceColumn;
import xyz.elidom.dbist.metadata.TableCol;
import xyz.elidom.sys.util.SysValueUtil;

/**
 * 엔티티 & 테이블 컬럼 비교를 위한 모델 
 * 
 * @author shortstop
 */
public class EntityColumn {
	
	/**
	 * 추가할 컬럼 
	 */
	public static String ALTER_TYPE_ADD = "A";
	/**
	 * 삭제할 컬럼 
	 */
	public static String ALTER_TYPE_DROP = "D";
	/**
	 * 변경할 컬럼 - 컬럼이름 변경 
	 */
	public static String ALTER_TYPE_RENAME = "RN";	
	/**
	 * 변경할 컬럼 - 타입 변경 
	 */
	public static String ALTER_TYPE_MODIFY_TYPE = "MF";
	/**
	 * 변경할 컬럼 - Nullable 변경 
	 */
	public static String ALTER_TYPE_MODIFY_NULLABLE = "MN";
	/**
	 * 변경할 컬럼 - Default 값 변경 
	 */
	public static String ALTER_TYPE_MODIFY_DEFAULT = "MD";
	
	/**
	 * 엔티티 컬럼명 
	 */
	private String entityColName;
	/**
	 * 테이블 컬럼명 
	 */
	private String tableColName;
	/**
	 * 엔티티 컬럼 설명 
	 */
	private String entityColDesc;
	/**
	 * 테이블 컬럼 설명 
	 */
	private String tableColDesc;
	/**
	 * 엔티티 컬럼 타입  
	 */
	private String entityColDataType;
	/**
	 * 테이블 컬럼 타입 
	 */
	private String tableColDataType;
	/**
	 * 엔티티 컬럼 사이즈 
	 */
	private Integer entityColLength;
	/**
	 * 테이블 컬럼 사이즈 
	 */
	private Integer tableColLength;
	/**
	 * 엔티티 컬럼 Null 허용 
	 */
	private Boolean entityColNullable;
	/**
	 * 테이블 컬럼 Null 허용 
	 */
	private Boolean tableColNullable;
	/**
	 * 엔티티 컬럼과 테이블 컬럼이 다른 지 여부  
	 */
	private Boolean typeDiff;
	/**
	 * 엔티티 컬럼 사이즈와 테이블 컬럼 사이즈가 다른 지 여부  
	 */
	private Boolean sizeDiff;
	/**
	 * Alter 문의 타입 
	 */
	private String alterType;
	
	public EntityColumn() {
	}
	
	public EntityColumn(ResourceColumn resourceCol, TableCol tableCol) {
		if(resourceCol != null) {
			this.applyResourceColumn(resourceCol);
		}
		
		if(tableCol != null) {
			this.applyTableColumn(tableCol);
		}
	}
	
	public void applyResourceColumn(ResourceColumn resourceCol) {
		this.entityColName = resourceCol.getName();
		this.entityColDesc = resourceCol.getDescription();
		this.entityColDataType = resourceCol.getColType();
		this.entityColLength = resourceCol.getColSize();
		this.entityColNullable = resourceCol.getNullable();
	}
	
	public void applyTableColumn(TableCol tableCol) {
		this.tableColName = tableCol.getName();
		this.tableColDesc = tableCol.getComment();
		this.tableColDataType = tableCol.getDataType();
		this.tableColLength = tableCol.getLength();
		this.tableColNullable = tableCol.getNullable();
	}

	/**
	 * @return the entityColName
	 */
	public String getEntityColName() {
		return entityColName;
	}

	/**
	 * @param entityColName the entityColName to set
	 */
	public void setEntityColName(String entityColName) {
		this.entityColName = entityColName;
	}

	/**
	 * @return the tableColName
	 */
	public String getTableColName() {
		return tableColName;
	}

	/**
	 * @param tableColName the tableColName to set
	 */
	public void setTableColName(String tableColName) {
		this.tableColName = tableColName;
	}

	/**
	 * @return the entityColDesc
	 */
	public String getEntityColDesc() {
		return entityColDesc;
	}

	/**
	 * @param entityColDesc the entityColDesc to set
	 */
	public void setEntityColDesc(String entityColDesc) {
		this.entityColDesc = entityColDesc;
	}

	/**
	 * @return the tableColDesc
	 */
	public String getTableColDesc() {
		return tableColDesc;
	}

	/**
	 * @param tableColDesc the tableColDesc to set
	 */
	public void setTableColDesc(String tableColDesc) {
		this.tableColDesc = tableColDesc;
	}

	/**
	 * @return the entityColDataType
	 */
	public String getEntityColDataType() {
		return entityColDataType;
	}

	/**
	 * @param entityColDataType the entityColDataType to set
	 */
	public void setEntityColDataType(String entityColDataType) {
		this.entityColDataType = entityColDataType;
	}

	/**
	 * @return the tableColDataType
	 */
	public String getTableColDataType() {
		return tableColDataType;
	}

	/**
	 * @param tableColDataType the tableColDataType to set
	 */
	public void setTableColDataType(String tableColDataType) {
		this.tableColDataType = tableColDataType;
	}

	/**
	 * @return the entityColLength
	 */
	public Integer getEntityColLength() {
		return entityColLength;
	}

	/**
	 * @param entityColLength the entityColLength to set
	 */
	public void setEntityColLength(Integer entityColLength) {
		this.entityColLength = entityColLength;
	}

	/**
	 * @return the tableColLength
	 */
	public Integer getTableColLength() {
		return tableColLength;
	}

	/**
	 * @param tableColLength the tableColLength to set
	 */
	public void setTableColLength(Integer tableColLength) {
		this.tableColLength = tableColLength;
	}

	/**
	 * @return the entityColNullable
	 */
	public Boolean getEntityColNullable() {
		return entityColNullable;
	}

	/**
	 * @param entityColNullable the entityColNullable to set
	 */
	public void setEntityColNullable(Boolean entityColNullable) {
		this.entityColNullable = entityColNullable;
	}

	/**
	 * @return the tableColNullable
	 */
	public Boolean getTableColNullable() {
		return tableColNullable;
	}

	/**
	 * @param tableColNullable the tableColNullable to set
	 */
	public void setTableColNullable(Boolean tableColNullable) {
		this.tableColNullable = tableColNullable;
	}

	/**
	 * @return the typeDiff
	 */
	public Boolean getTypeDiff() {
		return this.typeDiff;
	}

	/**
	 * @param isDifferent the typeDiff to set
	 */
	public void setTypeDiff(Boolean typeDiff) {
		this.typeDiff = typeDiff;
	}
	
	/**
	 * @return the alterType
	 */
	public String getAlterType() {
		return alterType;
	}

	/**
	 * @param alterType the alterType to set
	 */
	public void setAlterType(String alterType) {
		this.alterType = alterType;
	}

	/**
	 * Table column과 Entity column을 비교하여 다른 점이 있는지 체크 
	 * 
	 * @param tableMappingColName
	 */
	public Boolean checkTypeDiff(String tableMappingColName) {
		if(SysValueUtil.isEmpty(this.entityColDataType) || SysValueUtil.isEmpty(this.tableColDataType)) {
			this.typeDiff = true;
			return this.typeDiff;
		}
		
		if(SysValueUtil.isEqual(this.tableColDataType, "bigint") && SysValueUtil.isEqual(tableMappingColName, "integer")) {
			this.typeDiff = false;
			return this.typeDiff;
		}
		
		this.typeDiff = SysValueUtil.isNotEqual(this.tableColDataType, tableMappingColName);
		return this.typeDiff;
	}
	
	/**
	 * Table column size와 Entity column size를 비교하여 다른 점이 있는지 체크 
	 * 
	 * @return
	 */
	public Boolean checkSizeDiff() {
		if(SysValueUtil.isEmpty(this.entityColDataType) || SysValueUtil.isEmpty(this.tableColDataType)) {
			this.sizeDiff = true;
			return this.sizeDiff;
		}
		
		if(SysValueUtil.isEqual(this.entityColDataType, "string")) {
			this.sizeDiff = (this.entityColLength != this.tableColLength);
		} else {
			this.sizeDiff = false;
		}
		
		return this.sizeDiff;
	}
	
	/**
	 * 컬럼을 추가할 것인지 삭제할 것인지, 변경할 것인지를 판단 
	 * 
	 * @param tableMappingColType
	 */
	public String checkAlterType(String tableMappingColType) {
		if(this.checkTypeDiff(tableMappingColType)) {
			this.alterType = EntityColumn.ALTER_TYPE_MODIFY_TYPE;
			
		} else {
			if(SysValueUtil.isEmpty(this.tableColName) && SysValueUtil.isNotEmpty(this.entityColName)) {
				this.alterType = EntityColumn.ALTER_TYPE_ADD;
				
			} else if(SysValueUtil.isNotEmpty(this.tableColName) && SysValueUtil.isEmpty(this.entityColName)) {
				this.alterType = EntityColumn.ALTER_TYPE_DROP;
				
			} else {
				if(SysValueUtil.isNotEqual(this.tableColLength, this.entityColLength)) {
					this.alterType = EntityColumn.ALTER_TYPE_MODIFY_TYPE;
					
				} else if(SysValueUtil.isNotEqual(this.tableColNullable, this.entityColNullable)) {
					this.alterType = EntityColumn.ALTER_TYPE_MODIFY_NULLABLE;
				}
			}
		}
		
		return this.alterType;
	}
	
	public Map<String, Object> toAlterMap() {
		if(this.alterType == EntityColumn.ALTER_TYPE_ADD) {
			return SysValueUtil.newMap("name,type,nullable", this.entityColName, this.entityColDataType, this.entityColNullable ? "YES" : "NO");
			
		} else if(this.alterType == EntityColumn.ALTER_TYPE_DROP) {
			return SysValueUtil.newMap("name", this.entityColName);
			
		} else if(this.alterType == EntityColumn.ALTER_TYPE_RENAME) {
			return SysValueUtil.newMap("nameFrom,nameTo", this.tableColName, this.entityColName);
			
		} else if(this.alterType == EntityColumn.ALTER_TYPE_MODIFY_TYPE) {
			return SysValueUtil.newMap("name,type", this.entityColName, this.entityColDataType);
			
		} else if(this.alterType == EntityColumn.ALTER_TYPE_MODIFY_NULLABLE) {
			return SysValueUtil.newMap("name,nullable", this.entityColName, this.entityColNullable ? "YES" : "NO");
			
		} else if(this.alterType == EntityColumn.ALTER_TYPE_MODIFY_DEFAULT) {
			//return ValueUtil.newMap("name,defaultValue", this.entityColName, "");
			return null;
			
		} else {
			return null;
		}
	}
}
