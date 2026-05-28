package xyz.elidom.sec.entity;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Index;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Relation;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;
import xyz.elidom.sec.entity.relation.RoleRef;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.entity.relation.DomainRef;
import xyz.elidom.sys.entity.relation.UserRef;

@Table(name = "user_role_histories", idStrategy = GenerationRule.UUID, uniqueFields = "id,domainId", indexes = {
	@Index(name = "ix_user_role_histories_0", columnList = "id,domain_id", unique = true) 
})
public class UserRoleHistory extends ElidomStampHook {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 634847605512962143L;

	@PrimaryKey
	@Column(name = "id", nullable = false, length = 40)
	private String id;

	@Relation(field = "domainId")
	private DomainRef domain;

	@Column(name = "user_account_id", length = 40)
	private String userAccountId;

	@Relation(field = "userAccountId")
	private UserRef userAccount;

	@Column(name = "role_id", length = 40)
	private String roleId;

	@Relation(field = "roleId")
	private RoleRef role;

	@Column(name = "status", length = 20)
	private String status;

	public UserRoleHistory() {
	}

	public UserRoleHistory(String userId, String roleId, String status) {
		this.userAccountId = userId;
		this.roleId = roleId;
		this.status = status;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public DomainRef getDomain() {
		return domain;
	}

	public void setDomain(DomainRef domain) {
		this.domain = domain;
	}

	public String getUserAccountId() {
		return userAccountId;
	}

	public void setUserAccountId(String userId) {
		this.userAccountId = userId;
	}

	public UserRef getUserAccount() {
		return userAccount;
	}

	public void setUserAccount(UserRef user) {
		this.userAccount = user;

		if (this.userAccount != null) {
			String refId = this.userAccount.getId();
			if (refId != null) {
				this.userAccountId = refId;
			}
		}

		if (this.userAccountId == null) {
			this.userAccountId = SysConstants.EMPTY_STRING;
		}
	}

	public String getRoleId() {
		return roleId;
	}

	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}

	public RoleRef getRole() {
		return role;
	}

	public void setRole(RoleRef role) {
		this.role = role;

		if (this.role != null) {
			String refId = this.role.getId();
			if (refId != null) {
				this.roleId = refId;
			}
		}

		if (this.roleId == null) {
			this.roleId = SysConstants.EMPTY_STRING;
		}
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
