/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sec.entity;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Ignore;
import xyz.elidom.dbist.annotation.Index;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

@Table(name = "permissions", idStrategy = GenerationRule.UUID, notnullFields="roleId,resourceType,resourceId", indexes = { 
	@Index(name = "ix_pmss_0", columnList = "role_id,resource_type,resource_id"),
	@Index(name = "ix_pmss_1", columnList = "role_id"),
	@Index(name = "ix_pmss_2", columnList = "domain_id")
})
public class Permission extends ElidomStampHook {
	
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = -7040645010425174955L;

	@PrimaryKey
	@Column(name = "id", nullable = false, length = OrmConstants.FIELD_SIZE_UUID)
	private String id;

	@Column(name = "role_id", nullable = false, length = OrmConstants.FIELD_SIZE_UUID)
	private String roleId;

	@Column(name = "resource_id", nullable = false, length = OrmConstants.FIELD_SIZE_MEANINGFUL_ID)
	private String resourceId;

	@Column(name = "resource_type", nullable = false, length = OrmConstants.FIELD_SIZE_NAME)
	private String resourceType;

	@Column(name = "action_name", length = OrmConstants.FIELD_SIZE_NAME)
	private String actionName;

	@Column(name = "method_name", length = OrmConstants.FIELD_SIZE_NAME)
	private String methodName;

	@Ignore
	private String name;

	@Ignore
	private String parentId;
	
	public Permission() {
	}
	
	public Permission(String name) {
		this.name = name;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the roleId
	 */
	public String getRoleId() {
		return roleId;
	}

	/**
	 * @param roleId the roleId to set
	 */
	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}

	/**
	 * @return the resourceId
	 */
	public String getResourceId() {
		return resourceId;
	}

	/**
	 * @param resourceId the resourceId to set
	 */
	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}

	/**
	 * @return the resourceType
	 */
	public String getResourceType() {
		return resourceType;
	}

	/**
	 * @param resourceType the resourceType to set
	 */
	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

	/**
	 * @return the actionName
	 */
	public String getActionName() {
		return actionName;
	}

	/**
	 * @param actionName the actionName to set
	 */
	public void setActionName(String actionName) {
		this.actionName = actionName;
	}

	/**
	 * @return the methodName
	 */
	public String getMethodName() {
		return methodName;
	}

	/**
	 * @param methodName the methodName to set
	 */
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the parentId
	 */
	public String getParentId() {
		return parentId;
	}

	/**
	 * @param parentId the parentId to set
	 */
	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

}