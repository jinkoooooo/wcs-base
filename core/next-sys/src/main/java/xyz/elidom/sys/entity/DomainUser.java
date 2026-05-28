package xyz.elidom.sys.entity;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Ignore;
import xyz.elidom.dbist.annotation.Index;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;

@Table(name = "domain_users", idStrategy = GenerationRule.UUID, indexes = {
	@Index(name = "ix_domain_users_0", columnList = "user_id,domain_id", unique = true)
})
public class DomainUser extends xyz.elidom.orm.entity.basic.ElidomStampHook {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 302630612891121413L;

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;

	@Column (name = "user_id", nullable = false, length = 32)
	private String userId;
	/**
	 * 센터 코드
	 */
	@Ignore
	private String center;
	/**
	 * 사이트 코드 
	 */
	@Ignore
	private String siteCd;
	/**
	 * 사이트 명
	 */
	@Ignore
	private String siteNm;
	/**
	 * 사이트 권한 여부
	 */
	@Ignore
	private Boolean hasPermission;
  
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getCenter() {
		return center;
	}

	public void setCenter(String center) {
		this.center = center;
	}

	public String getSiteCd() {
		return siteCd;
	}

	public void setSiteCd(String siteCd) {
		this.siteCd = siteCd;
	}

	public String getSiteNm() {
		return siteNm;
	}

	public void setSiteNm(String siteNm) {
		this.siteNm = siteNm;
	}

	public Boolean getHasPermission() {
		return hasPermission;
	}

	public void setHasPermission(Boolean hasPermission) {
		this.hasPermission = hasPermission;
	}

}
