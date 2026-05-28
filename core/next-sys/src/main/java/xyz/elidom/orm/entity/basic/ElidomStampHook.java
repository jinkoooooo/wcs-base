/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.orm.entity.basic;

import java.util.Date;

import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.util.DateUtil;
import xyz.elidom.util.ValueUtil;

/**
 * Domain && User && Time Stamp && Entity Hook
 * 
 * @author shortstop
 */
public class ElidomStampHook extends ElidomStamp implements IEntityHook {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = -5587467454709213060L;

	@Override
	public void beforeCreate() {
		this._setDefault_(true, false);
		this._setId_();
		this.validationCheck(OrmConstants.CUD_FLAG_CREATE);
	}

	@Override
	public void afterCreate() {
		this.setComplexKey();
	}

	@Override
	public void beforeUpdate() {
		//this._setDefault_(false, true);
		
		// 수정 - 업데이트시에는 domainId 체크하지 않는다.
		Date now = DateUtil.getDate();
		this.setUpdatedAt(now);
		
		if (User.currentUser() != null) {
			this.setUpdaterId(User.currentUser().getId());
		}

		this.setPrimaryKeyByComplexKey();
		this.validationCheck(OrmConstants.CUD_FLAG_UPDATE);
	}

	@Override
	public void afterUpdate() {
		this.setComplexKey();
	}

	@Override
	public void beforeDelete() {
		this.setPrimaryKeyByComplexKey();
	}

	@Override
	public void afterDelete() {
		this.deleteDetailResource();
	}

	@Override
	public void beforeFind() {
		this._setDomainId_();
		this.setPrimaryKeyByComplexKey();
	}

	@Override
	public void afterFind() {
		this.setComplexKey();
	}

	@Override
	public void beforeSearch() {
		this._setDomainId_();
	}

	@Override
	public void afterSearch() {
		this.setComplexKey();
	}

	/**
	 * default setting
	 * 
	 * @param domainFlag
	 * @param createFlag
	 * @param updateFlag
	 */
	public void _setDefault_(boolean createFlag, boolean updateFlag) {
		this._setDomainId_();

		if (!(createFlag || updateFlag))
			return;

		Date now = DateUtil.getDate();

		if (createFlag && ValueUtil.isEmpty(this.createdAt)) {
			this.setCreatedAt(now);
			this.setUpdatedAt(now);
		}

		if (createFlag && ValueUtil.isEmpty(this.creatorId) && User.currentUser() != null) {
			this.setCreatorId(User.currentUser().getId());
			this.setUpdaterId(User.currentUser().getId());
		}

		if (updateFlag) {
			this.setUpdatedAt(now);
		}

		if (updateFlag && User.currentUser() != null) {
			this.setUpdaterId(User.currentUser().getId());
		}
	}

	/**
	 * Set Default Domain ID
	 */
	private void _setDomainId_() {
		if ((this.domainId == null || this.domainId <= 0) && (Domain.currentDomain() != null)) {
			this.setDomainId(Domain.currentDomain().getId());
		}
	}
}