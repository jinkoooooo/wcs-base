/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.orm.entity.basic;

import java.util.Date;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.ColumnType;
import xyz.elidom.dbist.annotation.Relation;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.entity.relation.UserRef;
import xyz.elidom.util.FormatUtil;

/**
 * DomainId, UpdatedAt, Updator 필드를 관리
 * 
 * @author shortstop
 */
public class DomainUpdateStamp extends AbstractStamp {

	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 4745859509140396613L;

	@Column(name = OrmConstants.TABLE_FIELD_DOMAIN_ID)
	protected Long domainId;
	
	@Column(name = OrmConstants.TABLE_FIELD_UPDATED_AT, type = ColumnType.DATETIME)
	protected Date updatedAt;
	
	@Column(name = OrmConstants.TABLE_FIELD_UPDATER_ID, length = OrmConstants.FIELD_SIZE_USER_ID)
	protected String updaterId;

	@Relation(field = OrmConstants.ENTITY_FIELD_UPDATER_ID)
	protected UserRef updater;

	/**
	 * @return the domainId
	 */
	public Long getDomainId() {
		return domainId;
	}

	/**
	 * @param domainId the domainId to set
	 */
	public void setDomainId(Long domainId) {
		this.domainId = domainId;
	}
	
	/**
	 * @return the updaterId
	 */
	public String getUpdaterId() {
		return updaterId;
	}

	/**
	 * @param updaterId the updaterId to set
	 */
	public void setUpdaterId(String updaterId) {
		this.updaterId = updaterId;
	}

	/**
	 * @return the updater
	 */
	public UserRef getUpdater() {
		return updater;
	}

	/**
	 * @param updater the updater to set
	 */
	public void setUpdater(UserRef updater) {
		this.updater = updater;
	}

	/**
	 * @return the updatedAt
	 */
	public Date getUpdatedAt() {
		return updatedAt;
	}

	/**
	 * @param updatedAt the updatedAt to set
	 */
	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}

	/**
	 * To String을 JSON 형식으로 ...
	 */
	@Override
	public String toString() {
		return FormatUtil.toJsonString(this);
	}
}