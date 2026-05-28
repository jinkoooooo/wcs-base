/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.orm.entity.basic;

import java.util.Date;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.ColumnType;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.util.FormatUtil;

/**
 * 생성, 업데이트 시간 필드를 관리
 * 
 * @author shortstop
 */
public class UpdateTimeStamp extends AbstractStamp {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = -8422589345106706905L;

	@Column(name = OrmConstants.TABLE_FIELD_UPDATED_AT, type = ColumnType.DATETIME)
	protected Date updatedAt;

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