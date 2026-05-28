/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.orm.entity.basic;

import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.entity.User;
import xyz.elidom.sys.util.DateUtil;
import xyz.elidom.util.ValueUtil;

/**
 * Update User && Update Time Stamp && Entity Hook
 * 
 * @author shortstop
 */
public class UpdateStampHook extends UpdateStamp implements IEntityHook {

	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 1494016238331457428L;

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
		this._setDefault_();
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
		this.setPrimaryKeyByComplexKey();
	}

	@Override
	public void afterFind() {
		this.setComplexKey();
	}
	
	@Override
	public void beforeSearch() {
	}

	@Override
	public void afterSearch() {
		this.setComplexKey();
	}

	/**
	 * default setting
	 */
	private void _setDefault_() {
		this.setUpdatedAt(DateUtil.getDate());
		
		if(ValueUtil.isEmpty(this.updaterId) && User.currentUser() != null) {
			this.setUpdaterId(User.currentUser().getId());
		}
	}
}