/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.entity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Ignore;
import xyz.elidom.dbist.annotation.Index;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Relation;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.exception.server.ElidomServiceException;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.entity.basic.ElidomStampHook;
import xyz.elidom.sys.SysConfigConstants;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.SysMessageConstants;
import xyz.elidom.sys.entity.relation.DomainRef;
import xyz.elidom.sys.system.auth.AuthProviderFactory;
import xyz.elidom.sys.system.auth.IAuthProvider;
import xyz.elidom.sys.util.SettingUtil;
import xyz.elidom.sys.util.SysValueUtil;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.DateUtil;

@Table(name = "users", idStrategy = GenerationRule.NONE, meaningfulFields = "login", uniqueFields = "login", notnullFields = "login,name", indexes = { 
	@Index(name = "ix_user_1", columnList = "domain_id"),
	@Index(name = "ix_user_2", columnList = "login"),
	@Index(name = "ix_user_3", columnList = "email"),
	@Index(name = "ix_user_4", columnList = "name"),
	@Index(name = "ix_user_5", columnList = "active_flag"),
	@Index(name = "ix_user_6", columnList = "admin_flag")
})
public class User extends ElidomStampHook {

	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 1L;

	@PrimaryKey
	@Column(name = "id", nullable = false, length = OrmConstants.FIELD_SIZE_USER_ID)
	private String id;

	@Relation(field = "domainId")
	private DomainRef domain;

	@Column(name = "login", nullable = false, length = 25)
	private String login;

	@Column(name = "email", length = OrmConstants.FIELD_SIZE_USER_ID)
	private String email;

	@Column(name = "encrypted_password", nullable = false, length = 80)
	private String encryptedPassword;

	@Column(name = "reset_password_token", length = 80)
	private String resetPasswordToken;

	@Column(name = "reset_password_sent_at")
	private Date resetPasswordSentAt;

	@Column(name = "remember_created_at")
	private Date rememberCreatedAt;

	@Column(name = "sign_in_count")
	private Integer signInCount;

	@Column(name = "current_sign_in_at")
	private Date currentSignInAt;

	@Column(name = "last_sign_in_at")
	private Date lastSignInAt;

	@Column(name = "current_sign_in_ip", length = OrmConstants.FIELD_SIZE_IP)
	private String currentSignInIp;

	@Column(name = "last_sign_in_ip", length = OrmConstants.FIELD_SIZE_IP)
	private String lastSignInIp;

	@Column(name = "name", nullable = false, length = 30)
	private String name;
	
	@Column(name = "dept", length = OrmConstants.FIELD_SIZE_NAME)
	private String dept;
	
	@Column(name = "division", length = OrmConstants.FIELD_SIZE_NAME)
	private String division;

	@Column(name = "locale", length = OrmConstants.FIELD_SIZE_LOCALE)
	private String locale;

	@Column(name = "timezone", length = 64)
	private String timezone;

	@Column(name = "super_user")
	private Boolean superUser;
	
	@Column(name = "admin_flag")
	private Boolean adminFlag;

	@Column(name = "operator_flag")
	private Boolean operatorFlag;

	@Column(name = "active_flag")
	private Boolean activeFlag;
	
	@Column(name = "exclusive_role", length = 20)
	private String exclusiveRole;
	
	@Column(name = "account_type", length = 20)
	private String accountType;
	
	@Column(name = "password_expire_date", length = 20)
	private String passwordExpireDate;
	
	@Column(name = "account_expire_date", length = 20)
	private String accountExpireDate;
	
	@Ignore
	private List<Map<String, Object>> roles = null;
	
	@Ignore
	private String stompUrl;

	public User() {
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
	 * @return the login
	 */
	public String getLogin() {
		return login;
	}

	/**
	 * @param login the login to set
	 */
	public void setLogin(String login) {
		this.login = login;
	}

	/**
	 * @return the email
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * @param email the email to set
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * @return the encryptedPassword
	 */
	public String getEncryptedPassword() {
		return encryptedPassword;
	}

	/**
	 * @param encryptedPassword the encryptedPassword to set
	 */
	public void setEncryptedPassword(String encryptedPassword) {
		this.encryptedPassword = encryptedPassword;
	}

	/**
	 * @return the resetPasswordToken
	 */
	public String getResetPasswordToken() {
		return resetPasswordToken;
	}

	/**
	 * @param resetPasswordToken the resetPasswordToken to set
	 */
	public void setResetPasswordToken(String resetPasswordToken) {
		this.resetPasswordToken = resetPasswordToken;
	}

	/**
	 * @return the resetPasswordSentAt
	 */
	public Date getResetPasswordSentAt() {
		return resetPasswordSentAt;
	}

	/**
	 * @param resetPasswordSentAt the resetPasswordSentAt to set
	 */
	public void setResetPasswordSentAt(Date resetPasswordSentAt) {
		this.resetPasswordSentAt = resetPasswordSentAt;
	}

	/**
	 * @return the rememberCreatedAt
	 */
	public Date getRememberCreatedAt() {
		return rememberCreatedAt;
	}

	/**
	 * @param rememberCreatedAt the rememberCreatedAt to set
	 */
	public void setRememberCreatedAt(Date rememberCreatedAt) {
		this.rememberCreatedAt = rememberCreatedAt;
	}

	/**
	 * @return the signInCount
	 */
	public Integer getSignInCount() {
		return signInCount;
	}

	/**
	 * @param signInCount the signInCount to set
	 */
	public void setSignInCount(Integer signInCount) {
		this.signInCount = signInCount;
	}

	/**
	 * @return the currentSignInAt
	 */
	public Date getCurrentSignInAt() {
		return currentSignInAt;
	}

	/**
	 * @param currentSignInAt the currentSignInAt to set
	 */
	public void setCurrentSignInAt(Date currentSignInAt) {
		this.currentSignInAt = currentSignInAt;
	}

	/**
	 * @return the lastSignInAt
	 */
	public Date getLastSignInAt() {
		return lastSignInAt;
	}

	/**
	 * @param lastSignInAt the lastSignInAt to set
	 */
	public void setLastSignInAt(Date lastSignInAt) {
		this.lastSignInAt = lastSignInAt;
	}

	/**
	 * @return the currentSignInIp
	 */
	public String getCurrentSignInIp() {
		return currentSignInIp;
	}

	/**
	 * @param currentSignInIp the currentSignInIp to set
	 */
	public void setCurrentSignInIp(String currentSignInIp) {
		this.currentSignInIp = currentSignInIp;
	}

	/**
	 * @return the lastSignInIp
	 */
	public String getLastSignInIp() {
		return lastSignInIp;
	}

	/**
	 * @param lastSignInIp the lastSignInIp to set
	 */
	public void setLastSignInIp(String lastSignInIp) {
		this.lastSignInIp = lastSignInIp;
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
	 * @return the dept
	 */
	public String getDept() {
		return dept;
	}
	
	/**
	 * @param dept the dept to set
	 */
	public void setDept(String dept) {
		this.dept = dept;
	}

	/**
	 * @return the division
	 */
	public String getDivision() {
		return division;
	}

	/**
	 * @param division the division to set
	 */
	public void setDivision(String division) {
		this.division = division;
	}

	/**
	 * @return the locale
	 */
	public String getLocale() {
		return locale;
	}

	/**
	 * @param locale the locale to set
	 */
	public void setLocale(String locale) {
		this.locale = locale;
	}

	/**
	 * @return the timezone
	 */
	public String getTimezone() {
		return timezone;
	}

	/**
	 * @param timezone the timezone to set
	 */
	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	/**
	 * @return the adminFlag
	 */
	public Boolean getAdminFlag() {
		return adminFlag;
	}

	/**
	 * @param adminFlag the adminFlag to set
	 */
	public void setAdminFlag(Boolean adminFlag) {
		this.adminFlag = adminFlag;
	}

	/**
	 * @return the operatorFlag
	 */
	public Boolean getOperatorFlag() {
		return operatorFlag;
	}

	/**
	 * @param operatorFlag the operatorFlag to set
	 */
	public void setOperatorFlag(Boolean operatorFlag) {
		this.operatorFlag = operatorFlag;
	}

	/**
	 * @return the activeFlag
	 */
	public Boolean getActiveFlag() {
		return activeFlag;
	}

	/**
	 * @param activeFlag the activeFlag to set
	 */
	public void setActiveFlag(Boolean activeFlag) {
		this.activeFlag = activeFlag;
	}

	/**
	 * @return the superUser
	 */
	public Boolean getSuperUser() {
		return superUser;
	}

	/**
	 * @param superUser the superUser to set
	 */
	public void setSuperUser(Boolean superUser) {
		this.superUser = superUser;
	}
	
	/**
	 * @return the exclusiveRole
	 */
	public String getExclusiveRole() {
		return exclusiveRole;
	}

	/**
	 * @param exclusiveRole the exclusiveRole to set
	 */
	public void setExclusiveRole(String exclusiveRole) {
		this.exclusiveRole = exclusiveRole;
	}

	/**
	 * @return the domain
	 */
	public DomainRef getDomain() {
		return domain;
	}

	/**
	 * @param domain the domain to set
	 */
	public void setDomain(DomainRef domain) {
		this.domain = domain;
	}

	/**
	 * @return the roles of user
	 */
	public List<Map<String, Object>> getRoles() {
		return roles;
	}

	/**
	 * @param roles the roles to set
	 */
	public void setRoles(List<Map<String, Object>> roles) {
		this.roles = roles;
	}
	
	/**
	 * @param role the role to add
	 */
	public void addRole(Map<String, Object> role) {
		if(roles == null) {
			roles = new ArrayList<Map<String, Object>>(3);
		}
		
		this.roles.add(role);
	}
	
	/**
	 * @return the stompUrl
	 */	
	public String getStompUrl() {
		return stompUrl;
	}

	/**
	 * @param stompUrl the stompUrl to set
	 */
	public void setStompUrl(String stompUrl) {
		this.stompUrl = stompUrl;
	}
	
	/**
	 * Get Account Type (ex.user,token,json...)
	 * 
	 * @return
	 */
	public String getAccountType() {
		return accountType;
	}

	/**
	 * Set Account Type (ex.user,token,json...)
	 * 
	 * @param accountType
	 */
	public void setAccountType(String accountType) {
		this.accountType = accountType;
	}

	/**
	 * Get Password Expire Date
	 * 
	 * @return
	 */
	public String getPasswordExpireDate() {
		return passwordExpireDate;
	}

	/**
	 * Set Password Expire Date
	 * 
	 * @param passwordExpireDate
	 */
	public void setPasswordExpireDate(String passwordExpireDate) {
		this.passwordExpireDate = passwordExpireDate;
	}

	/**
	 * Get Account Expire Date
	 * 
	 * @return
	 */
	public String getAccountExpireDate() {
		return accountExpireDate;
	}

	/**
	 * Set Account Expire Date
	 * 
	 * @param accountExpireDate
	 */
	public void setAccountExpireDate(String accountExpireDate) {
		this.accountExpireDate = accountExpireDate;
	}
	
	/**
	 * 현재 로그인 된 사용자를 추출 - 중요 확장 포인트
	 * 
	 * @return
	 */
	public static User currentUser() {
		IAuthProvider authProvider = BeanUtil.get(AuthProviderFactory.class).getAuthProvider();
		return authProvider == null ? null : authProvider.currentUser();
	}

	/**
	 * ID를 통한 사용자 조회
	 * 
	 * @param id
	 * @return
	 */
	public static User getUserById(String id) {
		return BeanUtil.get(IQueryManager.class).select(true, User.class, id);
	}

	/**
	 * E-mail을 통한 사용자 조회
	 * 
	 * @param email
	 * @return
	 */
	public static User getUserByEmail(String email) {
		return BeanUtil.get(IQueryManager.class).selectBySql(SysConstants.USER_BY_EMAIL_QUERY, SysValueUtil.newMap(SysConstants.USER_FIELD_EMAIL, email), User.class);
	}
	
	/**
	 * LoginId를 통한 사용자 조회
	 * 
	 * @param login
	 * @return
	 */
	public static User getUserByLoginId(String login) {
		return BeanUtil.get(IQueryManager.class).selectBySql(SysConstants.USER_BY_LOGIN_QUERY, SysValueUtil.newMap(SysConstants.USER_FIELD_LOGIN, login), User.class);
	}
	
	/**
	 * 현재 사용자가 superuser 이거나 해당 도메인의 admin인지 체크 
	 * 
	 * @return
	 */
	public static boolean isCurrentUserAdmin() {
		User user = User.currentUser();
		
		// 1. 먼저 사용자의 권한이 superuser 이거나 
		if(user.getSuperUser()) {
			return true;
		}
		
		// 2. 해당 도메인의 admin이면 full 권한 
		if(user.getAdminFlag() && SysValueUtil.isEqual(Domain.currentDomain().getId(), user.getDomainId())) {
			return true;
		}
		
		return false;
	}

	@Override
	public void beforeCreate() {
		super.beforeCreate();

		// 1. ID 설정
		if (SysValueUtil.isEmpty(this.id)) {
			IAuthProvider provider = BeanUtil.get(AuthProviderFactory.class).getAuthProvider();
			this.id = provider != null ? provider.loginToUserId(login) : login;
		}

		// 2. Account Type이 비어있다면 User Type으로 설정
		if (SysValueUtil.isEmpty(this.getAccountType())) {
			this.setAccountType(SysConstants.ACCOUNT_TYPE_USER);
		}

		// 3. 암호가 비었다면 기본 암호로 설정
		String pass = this.getEncryptedPassword();
		if (SysValueUtil.isEmpty(pass)) {
			String defaultPass = SettingUtil.getValue(SysConfigConstants.SECURITY_INIT_PASS);
			if (SysValueUtil.isEmpty(defaultPass)) {
				throw new ElidomServiceException(SysMessageConstants.USER_EMPTY_INIT_PASS, "Initial password can not be empty.");
			}
			
			// 중요 - 확장 포인트
			IAuthProvider provider = BeanUtil.get(AuthProviderFactory.class).getAuthProvider();
			String encodedPass = provider != null ? provider.encodePassword(defaultPass) : defaultPass;
			this.setEncryptedPassword(encodedPass);
			this.setResetPasswordToken(encodedPass);

			// Accout Type이 user 이고, 비밀번호 변경 주기가 설정되어 있는 경우, 현재 날짜를 만료 날짜로 지정
			if (SysValueUtil.isEqual(this.getAccountType(), SysConstants.ACCOUNT_TYPE_USER)) {
				boolean isEnable = SysValueUtil.toBoolean(SettingUtil.getValue(SysConfigConstants.USER_PASSWORD_EXPIRE_ENABLE), false);
				if (isEnable) {
					this.setPasswordExpireDate(DateUtil.dateStr(new Date(), DateUtil.getDateFormat()));
				}
			}
		}
	}
}