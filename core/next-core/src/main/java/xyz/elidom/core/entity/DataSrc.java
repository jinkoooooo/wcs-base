/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.core.entity;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Index;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

@Table(name = "data_srcs", idStrategy = GenerationRule.UUID, uniqueFields="domainId,name", indexes = { 
	@Index(name = "ix_data_srcs_0", columnList = "domain_id,name", unique = true)
})
public class DataSrc extends ElidomStampHook {

	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = -1712949966209327889L;
	
	/**
	 * 데이터소스 연결 상태 - CONNECTED
	 */
	public static final String STATUS_CONNECTED = "CONNECTED";
	/**
	 * 데이터소스 연결 해제 상태 - CLOSED
	 */
	public static final String STATUS_CLOSED = "CLOSED";
	/**
	 * 데이터소스 연결 중 에러 상태 - ERROR
	 */
	public static final String STATUS_ERROR = "ERROR";

	@PrimaryKey
	@Column (name = "id", nullable = false, length = OrmConstants.FIELD_SIZE_UUID)
	private String id;

	@Column (name = "name", nullable = false, length = OrmConstants.FIELD_SIZE_NAME)
	private String name;

	@Column (name = "description", length = OrmConstants.FIELD_SIZE_DESCRIPTION)
	private String description;

	@Column (name = "src_type", nullable = false, length = 15)
	private String srcType;

	@Column (name = "url", nullable = false, length = OrmConstants.FIELD_SIZE_URL)
	private String url;

	@Column (name = "class_name", length = OrmConstants.FIELD_SIZE_LONG_NAME)
	private String className;

	@Column (name = "domain", length = OrmConstants.FIELD_SIZE_NAME)
	private String domain;
	
	@Column (name = "userid", length = OrmConstants.FIELD_SIZE_USER_ID)
	private String userid;

	@Column (name = "password", length = 64)
	private String password;

	@Column (name = "max_active")
	private Integer maxActive;

	@Column (name = "max_idle")
	private Integer maxIdle;

	@Column (name = "min_idle")
	private Integer minIdle;

	@Column (name = "max_wait")
	private Integer maxWait;

	@Column (name = "evict_time")
	private Integer evictTime;

	@Column (name = "status", length = 10)
	private String status;
	
	public DataSrc() {
	}
	
	public DataSrc(String id) {
		this.id = id;
	}
	
	public DataSrc(Long domainId, String name) {
		this.domainId = domainId;
		this.name = name;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSrcType() {
		return srcType;
	}

	public void setSrcType(String srcType) {
		this.srcType = srcType;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}
	
	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getUserid() {
		return userid;
	}

	public void setUserid(String userid) {
		this.userid = userid;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Integer getMaxActive() {
		return maxActive;
	}

	public void setMaxActive(Integer maxActive) {
		this.maxActive = maxActive;
	}

	public Integer getMaxIdle() {
		return maxIdle;
	}

	public void setMaxIdle(Integer maxIdle) {
		this.maxIdle = maxIdle;
	}

	public Integer getMinIdle() {
		return minIdle;
	}

	public void setMinIdle(Integer minIdle) {
		this.minIdle = minIdle;
	}

	public Integer getMaxWait() {
		return maxWait;
	}

	public void setMaxWait(Integer maxWait) {
		this.maxWait = maxWait;
	}

	public Integer getEvictTime() {
		return evictTime;
	}

	public void setEvictTime(Integer evictTime) {
		this.evictTime = evictTime;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}