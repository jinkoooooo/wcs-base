/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.orm.entity.basic;

import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.entity.User;
import xyz.elidom.util.ValueUtil;

/**
 * User && Entity Hook
 * 
 * @author shortstop
 */
public class UserStampHook extends UserStamp implements IEntityHook {

	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 6251199356421176362L;

	@Override
	public void beforeCreate() {
		this._setDefault_(true, true);
		this._setId_();
		this.validationCheck(OrmConstants.CUD_FLAG_CREATE);
	}

	@Override
	public void afterCreate() {
		this.setComplexKey();
	}

	@Override
	public void beforeUpdate() {
		this._setDefault_(false, true);
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
	 * 
	 * @param createFlag
	 * @param updateFlag
	 */
	private void _setDefault_(boolean createFlag, boolean updateFlag) {
		if (createFlag && ValueUtil.isEmpty(this.creatorId) && User.currentUser() != null) {
			this.setCreatorId(User.currentUser().getId());
		}

		if (updateFlag && User.currentUser() != null) {
			this.setUpdaterId(User.currentUser().getId());
		}
	}
}