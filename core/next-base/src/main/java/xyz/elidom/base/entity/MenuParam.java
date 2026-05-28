/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.base.entity;

import xyz.elidom.base.entity.relation.MenuRef;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Index;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Relation;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.entity.basic.DomainStampHook;

@Table(name = "menu_params", idStrategy = GenerationRule.UUID, uniqueFields = "menuId,name", indexes = { 
	@Index(name = "ix_menu_param_0", columnList = "menu_id,name", unique = true),
	@Index(name = "ix_menu_param_1", columnList = "menu_id")
})
public class MenuParam extends DomainStampHook {

	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = -6074871116380047706L;

	@PrimaryKey
	@Column(name = "id", nullable = false, length = OrmConstants.FIELD_SIZE_UUID)
	private String id;

	@Column(name = "menu_id", nullable = false, length = OrmConstants.FIELD_SIZE_UUID)
	private String menuId;
	
	@Relation(field = "menuId")
	private MenuRef menu;

	@Column(name = "name", nullable = false, length = OrmConstants.FIELD_SIZE_NAME)
	private String name;

	@Column(name = "description", length = OrmConstants.FIELD_SIZE_DESCRIPTION)
	private String description;

	@Column(name = "value", nullable = false, length = OrmConstants.FIELD_SIZE_MAX_TEXT)
	private String value;

	public MenuParam() {
	}

	public MenuParam(String menuId, String name, String description, String value) {
		this.menuId = menuId;
		this.name = name;
		this.description = description;
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
	 * @return the menuId
	 */
	public String getMenuId() {
		return menuId;
	}

	/**
	 * @param menuId the menuId to set
	 */
	public void setMenuId(String menuId) {
		this.menuId = menuId;
	}

	/**
	 * @return the menu
	 */
	public MenuRef getMenu() {
		return menu;
	}

	/**
	 * @param menu the menu to set
	 */
	public void setMenu(MenuRef menu) {
		this.menu = menu;
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
	
}