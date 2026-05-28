/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.orm.entity.basic;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.Relation;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.entity.relation.UserRef;

/**
 * Creator, Updater 사용자 필드를 관리
 * 
 * @author shortstop
 */
public class UserStamp extends AbstractStamp {

	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 4830622656058784517L;

	@Column(name = OrmConstants.TABLE_FIELD_CREATOR_ID, length = OrmConstants.FIELD_SIZE_USER_ID)
	protected String creatorId;

	@Column(name = OrmConstants.TABLE_FIELD_UPDATER_ID, length = OrmConstants.FIELD_SIZE_USER_ID)
	protected String updaterId;

	@Relation(field = OrmConstants.ENTITY_FIELD_CREATOR_ID)
	protected UserRef creator;

	@Relation(field = OrmConstants.ENTITY_FIELD_UPDATER_ID)
	protected UserRef updater;

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
}