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
 * DomainId, CreatedAt, Creator 필드를 관리
 * 
 * @author shortstop
 */
public class DomainCreateStamp extends AbstractStamp {

	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = -2021474865003504977L;

	@Column(name = OrmConstants.TABLE_FIELD_DOMAIN_ID)
	protected Long domainId;
	
	@Column(name = OrmConstants.TABLE_FIELD_CREATED_AT, type = ColumnType.DATETIME)
	protected Date createdAt;
	
	@Column(name = OrmConstants.TABLE_FIELD_CREATOR_ID, length = OrmConstants.FIELD_SIZE_USER_ID)
	protected String creatorId;

	@Relation(field = OrmConstants.ENTITY_FIELD_CREATOR_ID)
	protected UserRef creator;

	/**
	 * @return the domainId
	 */
	public Long getDomainId() {
		return domainId;
	}

	/**
	 * @param domainId
	 *            the domainId to set
	 */
	public void setDomainId(Long domainId) {
		this.domainId = domainId;
	}
	
	/**
	 * @return the createdAt
	 */
	public Date getCreatedAt() {
		return createdAt;
	}

	/**
	 * @param createdAt the createdAt to set
	 */
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}
	
	/**
	 * @return the creatorId
	 */
	public String getCreatorId() {
		return creatorId;
	}

	/**
	 * @param creatorId the creatorId to set
	 */
	public void setCreatorId(String creatorId) {
		this.creatorId = creatorId;
	}

	/**
	 * @return the creator
	 */
	public UserRef getCreator() {
		return creator;
	}

	/**
	 * @param creator the creator to set
	 */
	public void setCreator(UserRef creator) {
		this.creator = creator;
	}

	/**
	 * To String을 JSON 형식으로 ...
	 */
	@Override
	public String toString() {
		return FormatUtil.toJsonString(this);
	}
}