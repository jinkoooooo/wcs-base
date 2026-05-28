/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.orm.entity.basic;

import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.util.DateUtil;
import xyz.elidom.util.ValueUtil;

/**
 * DomainId && Create User && Create Time Stamp && Entity Hook
 * 
 * @author shortstop
 */
public class DomainCreateStampHook extends DomainCreateStamp implements IEntityHook {

	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = -8525575471649526683L;

	@Override
	public void beforeCreate() {
		this._setDefault_();
		this._setId_();
		this.validationCheck(OrmConstants.CUD_FLAG_CREATE);
	}

	@Override
	public void afterCreate() {
		this.setComplexKey();
	}

	@Override
	public void beforeUpdate() {
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
	 */
	private void _setDefault_() {
		this._setDomainId_();

		this.setCreatedAt(DateUtil.getDate());

		if (ValueUtil.isEmpty(this.creatorId) && User.currentUser() != null) {
			this.setCreatorId(User.currentUser().getId());
		}
	}

	/**
	 * Set Default Domain ID
	 */
	private void _setDomainId_() {
		if (this.domainId == null || this.domainId <= 0) {
			this.setDomainId(Domain.currentDomain().getId());
		}
	}
}