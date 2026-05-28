/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.orm.entity.basic;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.orm.OrmConstants;

/**
 * Domain 필드를 관리
 * 
 * @author shortstop
 */
public class DomainStamp extends AbstractStamp {

	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = -4824882682642483567L;
	
	@Column(name = OrmConstants.TABLE_FIELD_DOMAIN_ID)
	protected Long domainId;

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
}