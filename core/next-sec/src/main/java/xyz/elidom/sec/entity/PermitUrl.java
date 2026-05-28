package xyz.elidom.sec.entity;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Index;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

@Table(name = "permit_urls", idStrategy = GenerationRule.UUID, uniqueFields = "domainId,name", indexes = {
		@Index(name = "ix_permit_urls_0", columnList = "domain_id,name", unique = true) })
public class PermitUrl extends ElidomStampHook {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 179111763060557588L;

	@PrimaryKey
	@Column(name = "id", nullable = false, length = OrmConstants.FIELD_SIZE_UUID)
	private String id;

	@Column(name = "name", nullable = false, length = OrmConstants.FIELD_SIZE_LONG_NAME)
	private String name;

	@Column(name = "description", length = OrmConstants.FIELD_SIZE_DESCRIPTION)
	private String description;

	@Column(name = "type", nullable = false, length = OrmConstants.FIELD_SIZE_CATEGORY)
	private String type;

	@Column(name = "active")
	private Boolean active;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}
}
