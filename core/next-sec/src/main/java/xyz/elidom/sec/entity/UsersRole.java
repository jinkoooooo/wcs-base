/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sec.entity;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Index;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.entity.basic.DomainStampHook;
import xyz.elidom.sec.SecConstants;
import xyz.elidom.util.BeanUtil;

@Table(name = "users_roles", idStrategy = GenerationRule.UUID, uniqueFields = "userId,roleId", indexes = { 
	@Index(name = "ix_user_role_0", columnList = "user_id,role_id", unique = true),
	@Index(name = "ix_user_role_1", columnList = "role_id,user_id"),
	@Index(name = "ix_user_role_2", columnList = "domain_id")
})
public class UsersRole extends DomainStampHook {
	
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 1L;

	@PrimaryKey
	@Column(name = "id", nullable = false, length = OrmConstants.FIELD_SIZE_UUID)
	private String id;

	@Column(name = "user_id", nullable = false, length = OrmConstants.FIELD_SIZE_USER_ID)
	private String userId;

	@Column(name = "role_id", nullable = false, length = OrmConstants.FIELD_SIZE_UUID)
	private String roleId;
	
	@Column(name = "users_id", length = OrmConstants.FIELD_SIZE_USER_ID)
	private String usersId;

	@Column(name = "roles_id", length = OrmConstants.FIELD_SIZE_UUID)
	private String rolesId;

	public UsersRole() {
	}

	public UsersRole(String userId, String roleId) {
		this.userId = userId;
		this.usersId = userId;
		this.roleId = roleId;
		this.rolesId = roleId;
	}
	
	public UsersRole(String id, String userId, String roleId) {
		this.id = id;
		this.userId = userId;
		this.usersId = userId;
		this.roleId = roleId;
		this.rolesId = roleId;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param userId the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the userId
	 */
	public String getUserId() {
		return userId;
	}

	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
		this.usersId = userId;
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
		this.rolesId = roleId;
	}
	
	public String getUsersId() {
		return usersId;
	}

	public void setUsersId(String usersId) {
		this.usersId = usersId;
	}

	public String getRolesId() {
		return rolesId;
	}

	public void setRolesId(String rolesId) {
		this.rolesId = rolesId;
	}

	@Override
	public void afterCreate() {
		super.afterCreate();
		this.createUserRoleHistory(SecConstants.USER_ROLE_CREATED);
	}
	
	@Override
	public void afterDelete() {
		super.afterDelete();
		this.createUserRoleHistory(SecConstants.USER_ROLE_DELETED);
	}
	
	private void createUserRoleHistory(String status) {
		BeanUtil.get(IQueryManager.class).insert(new UserRoleHistory(this.getUserId(), this.getRoleId(), status));
	}
}