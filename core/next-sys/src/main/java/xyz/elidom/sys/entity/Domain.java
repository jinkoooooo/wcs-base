/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.entity;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Index;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.exception.client.ElidomBadRequestException;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.entity.basic.UserTimeStampHook;
import xyz.elidom.sys.SysConstants;
import xyz.elidom.sys.rest.DomainController;
import xyz.elidom.sys.system.context.DomainContext;
import xyz.elidom.sys.util.SessionUtil;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.FormatUtil;
import xyz.elidom.util.ValueUtil;

@Table(name = "domains", idStrategy = GenerationRule.NONE, uniqueFields = "name", indexes = {
	@Index(name = "ix_domain_0", columnList = "name", unique = true),
	@Index(name = "ix_domain_1", columnList = "subdomain"),
	@Index(name = "ix_domain_2", columnList = "system_flag")
})
public class Domain extends UserTimeStampHook {

	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = -8199132822942901927L;
	/**
	 * 시스템 도메인 
	 */
	private static Domain systemDomain;

	@PrimaryKey
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(name = "name", nullable = false, length = OrmConstants.FIELD_SIZE_NAME)
	private String name;

	@Column(name = "description", length = OrmConstants.FIELD_SIZE_DESCRIPTION)
	private String description;

	@Column(name = "timezone", length = 64)
	private String timezone;

	@Column(name = "system_flag")
	private Boolean systemFlag;

	@Column(name = "site_port", length = 5)
	private Integer sitePort;
	
	@Column(name = "subdomain", nullable = false, length = 64)
	private String subdomain;

	@Column(name = "brand_name", length = OrmConstants.FIELD_SIZE_NAME)
	private String brandName;

	@Column(name = "brand_image", length = OrmConstants.FIELD_SIZE_UUID)
	private String brandImage;

	@Column(name = "content_image", length = OrmConstants.FIELD_SIZE_UUID)
	private String contentImage;
	
	@Column(name = "theme", length = OrmConstants.FIELD_SIZE_NAME)
	private String theme;
	
	@Column(name = "mw_site_cd", length = 20)
	private String mwSiteCd;
	
	@Column(name = "dev_subdomain", length = 64)
	private String devSubdomain;


	@Column(name = "owner", length = 40)
	private String owner;

	@Column(name = "provision_id", length = 100)
	private String provisionId;

	public Domain() {
	}

	public Domain(Long id) {
		this.id = id;
	}

	public Domain(String name) {
		this.name = name;
	}

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
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
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
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
	 * @return the systemFlag
	 */
	public Boolean getSystemFlag() {
		return systemFlag;
	}

	/**
	 * @param systemFlag the systemFlag to set
	 */
	public void setSystemFlag(Boolean systemFlag) {
		this.systemFlag = systemFlag;
	}

	/**
	 * @param sitePort the sitePort
	 */
	public Integer getSitePort() {
		return sitePort;
	}

	/**
	 * @param sitePort the sitePort to set
	 */
	public void setSitePort(Integer sitePort) {
		this.sitePort = sitePort;
	}

	/**
	 * @return the subdomain
	 */
	public String getSubdomain() {
		return subdomain;
	}

	/**
	 * @param subdomain the subdomain to set
	 */
	public void setSubdomain(String subdomain) {
		this.subdomain = subdomain;
	}

	/**
	 * @return the brandName
	 */
	public String getBrandName() {
		return brandName;
	}

	/**
	 * @param brandName the brandName to set
	 */
	public void setBrandName(String brandName) {
		this.brandName = brandName;
	}

	/**
	 * @return the brandImage
	 */
	public String getBrandImage() {
		return brandImage;
	}

	/**
	 * @param brandImage the brandImage to set
	 */
	public void setBrandImage(String brandImage) {
		this.brandImage = brandImage;
	}

	/**
	 * @return the contentImage
	 */
	public String getContentImage() {
		return contentImage;
	}

	/**
	 * @param contentImage the contentImage to set
	 */
	public void setContentImage(String contentImage) {
		this.contentImage = contentImage;
	}

	/**
	 * @return the theme
	 */
	public String getTheme() {
		return theme;
	}

	/**
	 * @param theme the theme to set
	 */
	public void setTheme(String theme) {
		this.theme = theme;
	}

	/**
	 * @return the mwSiteCd
	 */
	public String getMwSiteCd() {
		return mwSiteCd;
	}

	/**
	 * @param mwSiteCd the mwSiteCd to set
	 */
	public void setMwSiteCd(String mwSiteCd) {
		this.mwSiteCd = mwSiteCd;
	}

	/**
	 * @return the devSubdomain
	 */
	public String getDevSubdomain() {
		return devSubdomain;
	}

	/**
	 * @param devSubdomain the devSubdomain to set
	 */
	public void setDevSubdomain(String devSubdomain) {
		this.devSubdomain = devSubdomain;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	/**
	 * domainId로 도메인 조회
	 * 
	 * @param domainId
	 * @return
	 */
	public static Domain find(Long domainId) {
		return BeanUtil.get(IQueryManager.class).select(new Domain(domainId));
	}

	/**
	 * System Domain을 찾아 리턴
	 * 
	 * @return
	 */
	public static Domain systemDomain() {
		if (Domain.systemDomain == null) {
			Domain.resetSystemDomain();
		}

		return Domain.systemDomain;
	}
	
	/**
	 * System Domain을 찾아 리턴
	 * 
	 * @return
	 */	
	public static Domain resetSystemDomain() {
		Domain condition = new Domain();
		condition.setSystemFlag(true);
		Domain.systemDomain = BeanUtil.get(IQueryManager.class).selectByCondition(Domain.class, condition);
		return Domain.systemDomain;
	}

	/**
	 * 현재 세션의 도메인을 찾아 리턴 
	 * 
	 * @return
	 */
	public static Domain currentDomain() {
		Domain domain = (Domain) SessionUtil.getAttribute(SysConstants.CURRENT_DOMAIN);
		
		// 그래도 없다면 스레드 로컬 변수에서 조회
		if(domain == null) {
			domain = DomainContext.getCurrentDomain();
		}

        if(domain == null) {
            Domain temp = new  Domain();
            temp.setId(7L);
            domain = temp;
        }
		
		// 그래도 없다면 에러
		if (domain == null) {
			throw new RuntimeException("Current Domain Not Exist!");
		}
		
		return domain;
	}
	
	public static Domain currentDomain(String subdomain) {
		Domain domain = null;
		
		if(ValueUtil.isNotEmpty(subdomain)) {
			domain = BeanUtil.get(DomainController.class).findBySubdomain(subdomain);
		} else {
			domain = (Domain) SessionUtil.getAttribute(SysConstants.CURRENT_DOMAIN);
		}
		
		if(domain == null) {
			domain = DomainContext.getCurrentDomain();
		}
		
		if (ValueUtil.isEmpty(domain)) {
			domain = Domain.systemDomain();
		}
		// 그래도 없다면 에러
		if (domain == null) {
			throw new RuntimeException("Current Domain Not Exist!");
		}
		return domain;
	}

	/**
	 * 현재 스레드에 currentDomain을 설정
	 * 
	 * @param currentDomain
	 * @return
	 */
	public static void setCurrentDomain(Domain currentDomain) {
		DomainContext.setCurrentDomain(currentDomain);
	}
	
	/**
	 * 현재 세션의 도메인 ID를 찾아 리턴
	 * 
	 * @return
	 */
	public static Long currentDomainId() {
		return Domain.currentDomain().getId();
	}
	
	/**
	 * 도메인 이름으로 도메인을 찾아 리턴
	 * 
	 * @param name
	 * @return
	 */
	public static Domain findByName(String name) {
		return BeanUtil.get(DomainController.class).findByName(name);
	}

	/**
	 * subdomain으로 도메인을 찾아 리턴
	 * 
	 * @param subdomain
	 * @return
	 */
	public static Domain findBySubdomain(String subdomain) {
		return BeanUtil.get(DomainController.class).findBySubdomain(subdomain);
	}
	
	/**
	 * 미들웨어 사이트 코드로 도메인 조회
	 * 
	 * @param mwSiteCd
	 * @return
	 */
	public static Domain findByMwSiteCd(String mwSiteCd) {
		// TODO 도메인 컨트롤러 API 이용하여 캐쉬 기능을 사용하도록 수정
		Query condition = new Query();
		condition.addFilter("mwSiteCd", mwSiteCd);
		return BeanUtil.get(IQueryManager.class).selectByCondition(Domain.class, condition);
	}

	@Override
	public void beforeCreate() {
		super.beforeCreate();

		if (Domain.systemDomain() != null && this.systemFlag) {
			throw new ElidomBadRequestException("System domain already exist! System domain must be one!");
		}
	}

	@Override
	public void afterCreate() {
		super.afterCreate();

		// 시스템 도메인이 없는 상태에서 시스템 도메인이 생성되었다면 생성된 도메인이 시스템 도메인이 되어야 한다.
		if (Domain.systemDomain() == null && this.systemFlag) {
			Domain.systemDomain = this;
		}
	}

	@Override
	public void beforeUpdate() {
		super.beforeUpdate();

		if (Domain.systemDomain() != null && !this.systemFlag) {
			// 시스템 도메인은 일반 도메인으로 변경되지 못한다.
			if (Domain.systemDomain().getId() == this.getId())
				throw new ElidomBadRequestException("System domain must not be updated to general domain! System domain must be one!");
		}
	}

	@Override
	public void beforeDelete() {
		super.beforeDelete();

		if (this.systemFlag) {
			throw new ElidomBadRequestException("System domain can not be deleted! System domain must be one!");
		}
	}
	
	@Override
	protected <T> void _setId_() {
		if(this.id == null || this.id == 0) {
			Long maxId = BeanUtil.get(IQueryManager.class).selectBySql("select max(id) as id from domains", null, Long.class);
			
			if(maxId == null || maxId == 0) {
				this.id = ValueUtil.toLong(1);
			} else {
				this.id = maxId + 1;
			}
		}
	}
	
	/**
	 * To String을 JSON 형식으로 ...
	 */
	@Override
	public String toString() {
		return FormatUtil.toJsonString(this);
	}

	public String getProvisionId() {
		return provisionId;
	}

	public void setProvisionId(String provisionId) {
		this.provisionId = provisionId;
	}
}