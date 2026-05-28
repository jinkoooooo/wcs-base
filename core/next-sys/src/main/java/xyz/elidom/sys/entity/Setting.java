/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.sys.entity;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Index;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

@Table(name = "settings", idStrategy = GenerationRule.UUID, uniqueFields = "domainId,name", indexes = { 
	@Index(name = "ix_setting_0", columnList = "domain_id,name", unique = true),
	@Index(name = "ix_setting_1", columnList = "domain_id,category")
})
public class Setting extends ElidomStampHook {

	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 1448754347791659956L;

	@PrimaryKey
	@Column(name = "id", nullable = false, length = OrmConstants.FIELD_SIZE_UUID)
	private String id;	

	@Column(name = "name", nullable = false, length = OrmConstants.FIELD_SIZE_LONG_NAME)
	private String name;

	@Column(name = "description", length = OrmConstants.FIELD_SIZE_DESCRIPTION)
	private String description;

	@Column(name = "value", length = OrmConstants.FIELD_SIZE_VALUE_2000)
	private String value;
	
	@Column(name = "category", length = OrmConstants.FIELD_SIZE_CATEGORY)
	private String category;	

	public Setting() {
	}

	public Setting(String name) {
		this(Domain.currentDomain().getId(), name);
	}
	
	public Setting(String name, String value) {
		this(Domain.currentDomain().getId(), name, value);
	}
	
	public Setting(Long domainId, String name) {
		this(domainId, name, null);
	}
	
	public Setting(Long domainId, String name, String value) {
		this.domainId = domainId;
		this.name = name;
		this.value = value;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * @return the category
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * @param category the category to set
	 */
	public void setCategory(String category) {
		this.category = category;
	}
}