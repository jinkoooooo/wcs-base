/* Copyright © HatioLab Inc. All rights reserved. */
package xyz.elidom.sys.entity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.ColumnType;
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
import xyz.elidom.sec.SecConfigConstants;
import xyz.elidom.sec.SecConstants;
import xyz.elidom.sec.entity.LoginHistory;
import xyz.elidom.sec.entity.Role;
import xyz.elidom.sec.entity.UserHistory;
import xyz.elidom.sec.entity.UserRoleHistory;
import xyz.elidom.sec.entity.UsersRole;
import xyz.elidom.sec.util.SecurityUtil;
import xyz.elidom.sys.SysConfigConstants;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.SysMessageConstants;
import xyz.elidom.sys.entity.relation.DomainRef;
import xyz.elidom.sys.system.auth.AuthProviderFactory;
import xyz.elidom.sys.system.auth.IAuthProvider;
import xyz.elidom.sys.util.MessageUtil;
import xyz.elidom.sys.util.SettingUtil;
import xyz.elidom.sys.util.SysValueUtil;
import xyz.elidom.sys.util.ThrowUtil;
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
	
	// 아래 필드들은 node client 호환을 위함
	@Column(name = "description", length = 100)
	private String description;
	
	
	@Column(name = "user_type", length = 20)
	private String userType;
	
	@Column(name = "reference", length = 40)
	private String reference;
	
	// sha-256 hmec key (uuid)
	@Column(name = "salt", length = 40)
	private String salt;
	
	// sha-256 hmec base 64	
	@Column(name = "password", length = 1000)
	private String password;

	
	@Column(name = "status", length = 20)
	private String status;
	
	@Column(name = "fail_count")
	private Integer failCount;
	
	@Column(name = "password_updated_at", type = ColumnType.DATETIME)
	protected Date passwordUpdatedAt;

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
		if (roles == null) {
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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getUserType() {
		return userType;
	}

	public void setUserType(String userType) {
		this.userType = userType;
	}

	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

	public String getSalt() {
		return salt;
	}

	public void setSalt(String salt) {
		this.salt = salt;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Integer getFailCount() {
		return failCount;
	}

	public void setFailCount(Integer failCount) {
		this.failCount = failCount;
	}

	public Date getPasswordUpdatedAt() {
		return passwordUpdatedAt;
	}

	public void setPasswordUpdatedAt(Date passwordUpdatedAt) {
		this.passwordUpdatedAt = passwordUpdatedAt;
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
		if (user.getSuperUser()) {
			return true;
		}

		// 2. 해당 도메인의 admin이면 full 권한
		if (user.getAdminFlag() && SysValueUtil.isEqual(Domain.currentDomain().getId(), user.getDomainId())) {
			return true;
		}

		return false;
	}

	@Override
	public void beforeCreate() {		
		// 1. 기본 설정
		this._setDefault_(true, false);

		// 2. ID 설정
		if (SysValueUtil.isEmpty(this.id)) {
			IAuthProvider provider = BeanUtil.get(AuthProviderFactory.class).getAuthProvider();
			this.id = provider != null ? provider.loginToUserId(login) : login;
		}
		
		// 3. 사용자가 이미 존재하는지 체크
		IQueryManager queryMgr = BeanUtil.get(IQueryManager.class);
		if(queryMgr.select(User.class, this.id) != null) {
			throw ThrowUtil.newValidationErrorWithNoLog(MessageUtil.getMessage("USER_ALREADY_EXIST"));
		}

		// 4. Account Type이 비어있다면 User Type으로 설정
		if (SysValueUtil.isEmpty(this.getAccountType())) {
			this.setAccountType(SysConstants.ACCOUNT_TYPE_USER);
		}

		// 5. 암호가 비었다면 기본 암호로 설정
		String pass = this.getEncryptedPassword();
		if (SysValueUtil.isEmpty(pass)) {
			String defaultPass = SecurityUtil.newPass();
			if (SysValueUtil.isEmpty(defaultPass)) {
				throw new ElidomServiceException(SysMessageConstants.USER_EMPTY_INIT_PASS, "Initial password can not be empty.");
			}
			
			// 중요 - 확장 포인트
			IAuthProvider provider = BeanUtil.get(AuthProviderFactory.class).getAuthProvider();
			String encodedPass = provider != null ? provider.encodePassword(defaultPass) : defaultPass;
			this.setEncryptedPassword(defaultPass);
			this.setResetPasswordToken(encodedPass);
		}
		
		// 6. Accout Type이 토큰이 아닌 사용자이라면
		if (SysValueUtil.isEqual(this.accountType, SysConstants.ACCOUNT_TYPE_USER)) {
			// 6.1 비밀번호 변경 주기가 설정되어 있는 경우, 현재 날짜를 만료 날짜로 지정
			boolean isEnable = SysValueUtil.toBoolean(SettingUtil.getValue(SysConfigConstants.USER_PASSWORD_EXPIRE_ENABLE), false);
			if (isEnable) {
				String period = SettingUtil.getValue(SysConfigConstants.USER_PASSWORD_CHANGE_PERIOD_DAY, "90");
				String parseDate = DateUtil.addDateToStr(new Date(), SysValueUtil.toInteger(period));

				this.setPasswordExpireDate(parseDate);
			}
		}
		
		// 7. 기본값 설정
		if(this.adminFlag == null) {
			this.setAdminFlag(false);
		}
		
		if(this.superUser == null) {
			this.setSuperUser(false);
		}
		
		if(this.operatorFlag == null) {
			this.setOperatorFlag(false);
		}
		
		if(this.activeFlag == null) {
			this.setActiveFlag(false);
		}
		
		// 8. Operato 관련 field
		try {
			// 8.1 sha-256 키 생성 
			this.setSalt(UUID.randomUUID().toString().replaceAll("-", ""));
			
			// 8.2. SecretKeySpec 클래스를 사용한 키 생성 
			SecretKeySpec secretKey = new SecretKeySpec(this.getSalt().getBytes("utf-8"), "HmacSHA256");

			// 8.3. 지정된  MAC 알고리즘을 구현하는 Mac 객체를 작성합니다.
			Mac hasher = Mac.getInstance("HmacSHA256");
					
			// 8.4. 키를 사용해 이 Mac 객체를 초기화
			hasher.init(secretKey);
					
			// 8.5. 암호화 하려는 데이터의 바이트의 배열을 처리해 MAC 조작을 종료
			byte[] hash = hasher.doFinal(SecurityUtil.newPass().getBytes());
					
			// 8.6. Base 64 Encode to String
			this.setPassword(Base64.encodeBase64String(hash));
			
			// 8.7 기타 필드.. 
			this.setEmail(this.getId());
			this.setStatus(SysValueUtil.toBoolean(this.getActiveFlag(), false) == true ? "activated" : "inactive");
			this.setFailCount(0);
			this.setUserType(SysValueUtil.toBoolean(this.getAdminFlag(), false) == true ? "admin" : "user");
			
		} catch(Exception e) {
			throw new ElidomServiceException(e);
		}
	}

	@Override
	public void afterCreate() {
		super.afterCreate();

		// 동일한 계정의 이전 정보가 존재한다면 삭제.
		this.deleteOldUserInfo();
		
		// 사용자 계정 생성/삭제 이력 생성.
		this.createUserHistory(SecConstants.USER_CREATED);

		// 사용자 권한 데이터 생성.
		this.createUsersRole();
	}

	@Override
	public void afterDelete() {
		super.afterDelete();

		// 사용자 계정 생성/삭제 이력 생성.
		this.createUserHistory(SecConstants.USER_DELETED);

		// 사용자 권한 삭제.
		this.deleteUsersRole();
		
		// 도메인 사용자 관계 정보 삭제
		BeanUtil.get(IQueryManager.class).deleteByCondition(DomainUser.class, SysValueUtil.newMap("userId", this.id));
	}

	@Override
	public void beforeUpdate() {
		super.beforeUpdate();

		// 사용자 ActiveFlag 변경에 대한 이력 생성.
		if (SysValueUtil.isNotEmpty(this.getActiveFlag())) {
			IQueryManager queryManager = BeanUtil.get(IQueryManager.class);
			User user = queryManager.select(User.class, this.getId());

			if (SysValueUtil.isNotEqual(user.getActiveFlag(), this.getActiveFlag())) {
				this.createUserHistory(this.getActiveFlag() ? SecConstants.USER_ACTIVE : SecConstants.USER_INACTIVE);
			}
		}
	}

	/**
	 * 사용자 계정 이력 생성.
	 */
	private void createUserHistory(String status) {
		BeanUtil.get(IQueryManager.class).insert(new UserHistory(this.getId(), status));
	}

	/**
	 * 사용자 권한 데이터 생성.
	 */
	private void createUsersRole() {
		// Role 설정 여부.
		boolean isSuperUser = SysValueUtil.toBoolean(this.getSuperUser(), false);
		boolean isAdminUser = SysValueUtil.toBoolean(this.getAdminFlag(), false);
		boolean isOperatorUser = SysValueUtil.toBoolean(this.getOperatorFlag(), false);

		// Role 설정값 조회.
		List<String> roleNameList = new ArrayList<String>();
		if (isSuperUser) {
			roleNameList.add(SettingUtil.getValue(SecConfigConstants.ROLE_SUPER, "super"));
		}
		if (isAdminUser) {
			roleNameList.add(SettingUtil.getValue(SecConfigConstants.ROLE_ADMIN, "admin"));
		}
		if (isOperatorUser) {
			roleNameList.add(SettingUtil.getValue(SecConfigConstants.ROLE_OPERATOR, "operator"));
		}

		// super, admin, operator가 아니라면 기본 권한(guest) 부여.
		if (!(isSuperUser || isAdminUser || isOperatorUser)) {
			roleNameList.add(SettingUtil.getValue(SecConfigConstants.ROLE_GUEST, "guest"));
		}

		// 이전 데이터가 있을 경우 삭제.
		this.deleteUsersRole();

		/**
		 * UsersRole 데이터 생성.
		 */
		IQueryManager queryManager = BeanUtil.get(IQueryManager.class);
		for (String roleName : roleNameList) {
			Role role = queryManager.selectByCondition(Role.class, SysValueUtil.newMap("domain_id,name", Domain.currentDomainId(), roleName));
			if (SysValueUtil.isNotEmpty(role)) {
				queryManager.insert(new UsersRole(this.getId(), role.getId()));
			}
		}
	}
	
	/**
	 * 이전 사용자에 대한 접속 정보 삭제.
	 */
	private void deleteOldUserInfo() {
		IQueryManager queryManager = BeanUtil.get(IQueryManager.class);

		/*
		 * 로그인 이력 삭제.
		 */
		LoginHistory loginHistory = new LoginHistory();
		loginHistory.setAccessUserId(this.getId());

		List<LoginHistory> loginHistories = queryManager.selectList(LoginHistory.class, loginHistory);
		queryManager.deleteBatch(loginHistories);

		/*
		 * 권한 생성 이력 삭제.
		 */
		UserRoleHistory userRoleHistory = new UserRoleHistory();
		userRoleHistory.setUserAccountId(this.getId());

		List<UserRoleHistory> userRoleHistories = queryManager.selectList(UserRoleHistory.class, userRoleHistory);
		queryManager.deleteBatch(userRoleHistories);
	}

	/**
	 * 사용자 삭제 시, 권한 삭제.
	 */
	private void deleteUsersRole() {
		IQueryManager queryManager = BeanUtil.get(IQueryManager.class);
		UsersRole usersRole = new UsersRole();
		usersRole.setDomainId(Domain.currentDomainId());
		usersRole.setUserId(this.getId());
		
		List<UsersRole> roles = queryManager.selectList(UsersRole.class, usersRole);
		queryManager.deleteBatch(roles);
	}
}