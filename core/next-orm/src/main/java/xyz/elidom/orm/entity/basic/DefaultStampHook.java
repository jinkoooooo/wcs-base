/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.orm.entity.basic;

import xyz.elidom.orm.OrmConstants;

/**
 * Time Stamp && Entity Hook
 * 
 * @author shortstop
 */
public class DefaultStampHook extends AbstractStamp implements IEntityHook {

	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 381282050706590716L;

	@Override
	public void beforeCreate() {
		this._setId_();
		this.validationCheck(OrmConstants.CUD_FLAG_CREATE);
	}

	@Override
	public void afterCreate() {
	}

	@Override
	public void beforeUpdate() {
		this.validationCheck(OrmConstants.CUD_FLAG_UPDATE);
	}

	@Override
	public void afterUpdate() {
	}

	@Override
	public void beforeDelete() {

	}

	@Override
	public void afterDelete() {
	}

	@Override
	public void beforeFind() {
	}

	@Override
	public void afterFind() {
	}
	
	@Override
	public void beforeSearch() {
	}

	@Override
	public void afterSearch() {
	}
}