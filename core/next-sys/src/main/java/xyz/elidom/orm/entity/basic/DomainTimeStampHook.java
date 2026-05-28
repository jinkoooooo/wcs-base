package xyz.elidom.orm.entity.basic;

import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.util.DateUtil;

/**
 * DomainId && Time Stamp && Entity Hook
 * 
 * @author shortstop
 */
public class DomainTimeStampHook extends DomainTimeStamp implements IEntityHook {

	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 92738618306781754L;

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
		//this._setDefault_();
		this.setUpdatedAt(DateUtil.getDate());				
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
		this.setUpdatedAt(DateUtil.getDate());
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
