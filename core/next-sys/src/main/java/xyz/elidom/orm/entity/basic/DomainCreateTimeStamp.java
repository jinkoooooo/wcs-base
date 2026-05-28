package xyz.elidom.orm.entity.basic;

import java.util.Date;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.ColumnType;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.util.FormatUtil;

/**
 * DomainId, CreatedAt 필드를 관리
 * 
 * @author shortstop
 */
public class DomainCreateTimeStamp extends DomainStamp {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -8388072606219702056L;

	@Column(name = OrmConstants.TABLE_FIELD_CREATED_AT, type = ColumnType.DATETIME)
	protected Date createdAt;
	
	/**
	 * @return the createdAt
	 */
	public Date getCreatedAt() {
		return createdAt;
	}

	/**
	 * @param createdAt the createdAt to set
	 */
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	/**
	 * To String을 JSON 형식으로 ...
	 */
	@Override
	public String toString() {
		return FormatUtil.toJsonString(this);
	}
	
}
