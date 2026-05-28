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
 * Creator, Updater 및 시간 및 Multiple Update 필드를 관리
 * 
 * @author shortstop
 */
public class UserTimeStamp extends AbstractStamp {

	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 4339839808095212737L;

	@Column(name = OrmConstants.TABLE_FIELD_CREATOR_ID, length = OrmConstants.FIELD_SIZE_USER_ID)
	protected String creatorId;

	@Column(name = OrmConstants.TABLE_FIELD_UPDATER_ID, length = OrmConstants.FIELD_SIZE_USER_ID)
	protected String updaterId;
	
	@Relation(field = OrmConstants.ENTITY_FIELD_CREATOR_ID)
	protected UserRef creator;

	@Relation(field = OrmConstants.ENTITY_FIELD_UPDATER_ID)
	protected UserRef updater;

	@Column(name = OrmConstants.TABLE_FIELD_CREATED_AT, type = ColumnType.DATETIME)
	protected Date createdAt;

	@Column(name = OrmConstants.TABLE_FIELD_UPDATED_AT, type = ColumnType.DATETIME)
	protected Date updatedAt;

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
	 * @return the createdAt
	 */
	public Date getCreatedAt() {
		return createdAt;
	}

	/**
	 * @return the creator
	 */
	public UserRef getCreator() {
		return this.creator;
	}

	/**
	 * @param creator the creator to set
	 */
	public void setCreator(UserRef creator) {
		this.creator = creator;
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
	 * @param createdAt the createdAt to set
	 */
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
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