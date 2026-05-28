/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.msg.entity;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Index;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

@Table(name = "terminologies", idStrategy = GenerationRule.UUID, uniqueFields = "domainId,locale,category,name", indexes = {
	@Index(name = "ix_term_0", columnList = "domain_id,locale,category,name", unique = true),
	@Index(name = "ix_term_1", columnList = "domain_id"),
	@Index(name = "ix_term_2", columnList = "domain_id,locale"),
	@Index(name = "ix_term_3", columnList = "domain_id,locale,name")
})
public class Terminology extends ElidomStampHook {

	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 7491007906053521209L;
	
	@PrimaryKey
	@Column(name = "id", nullable = false, length = OrmConstants.FIELD_SIZE_UUID)
	private String id;

	@Column(name = "name", nullable = false, length = OrmConstants.FIELD_SIZE_LONG_NAME)
	private String name;

	@Column(name = "description", length = OrmConstants.FIELD_SIZE_DESCRIPTION)
	private String description;

	@Column(name = "locale", length = OrmConstants.FIELD_SIZE_LOCALE, nullable = false)
	private String locale;

	@Column(name = "category", length = OrmConstants.FIELD_SIZE_CATEGORY, nullable = false)
	private String category;

	@Column(name = "display", length = OrmConstants.FIELD_SIZE_VALUE_1000)
	private String display;

	public Terminology() {
	}
	
	public Terminology(Long domainId, String name) {
		this.domainId = domainId;
		this.name = name;
	}
	
	public Terminology(Long domainId, String name, String locale, String category, String display) {
		this(domainId, name);
		this.locale = locale;
		this.category = category;
		this.display = display;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
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
	 * @param name
	 *            the name to set
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
	 * @param description
	 *            the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the locale
	 */
	public String getLocale() {
		return locale;
	}

	/**
	 * @param locale
	 *            the locale to set
	 */
	public void setLocale(String locale) {
		this.locale = locale;
	}

	/**
	 * @return the category
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * @param category
	 *            the category to set
	 */
	public void setCategory(String category) {
		this.category = category;
	}

	/**
	 * @return the display
	 */
	public String getDisplay() {
		return display;
	}

	/**
	 * @param display
	 *            the display to set
	 */
	public void setDisplay(String display) {
		this.display = display;
	}
}