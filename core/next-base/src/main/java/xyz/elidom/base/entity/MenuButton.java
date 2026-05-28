/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.base.entity;

import xyz.elidom.base.entity.relation.MenuRef;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.ColumnType;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Index;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Relation;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.entity.basic.DomainStampHook;

@Table(name = "menu_buttons", idStrategy = GenerationRule.UUID, uniqueFields = "menuId,text", indexes = { 
	@Index(name = "ix_menu_btn_0", columnList = "menu_id,text", unique = true),
	@Index(name = "ix_menu_btn_1", columnList = "menu_id")
})
public class MenuButton extends DomainStampHook {

	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = -1644213038805706903L;

	@PrimaryKey
	@Column(name = "id", nullable = false, length = OrmConstants.FIELD_SIZE_UUID)
	private String id;

	@Column(name = "menu_id", nullable = false, length = OrmConstants.FIELD_SIZE_UUID)
	private String menuId;
	
	@Relation(field = "menuId")
	private MenuRef menu;

	@Column(name = "rank", length = 64)
	private Integer rank;

	@Column(name = "style", length = 64)
	private String style;

	@Column(name = "icon", length = 32)
	private String icon;

	@Column(name = "text", nullable = false, length = OrmConstants.FIELD_SIZE_NAME)
	private String text;

	@Column(name = "auth", length = 10)
	private String auth;
	
	@Column(name = "logic", type = ColumnType.TEXT)
	private String logic;
	
	@Column(name = "button_type", length = 10)
	private String buttonType;

	public MenuButton() {
	}

	public MenuButton(String menuId) {
		this.menuId = menuId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getMenuId() {
		return menuId;
	}

	public void setMenuId(String menuId) {
		this.menuId = menuId;
	}

	public MenuRef getMenu() {
		return menu;
	}

	public void setMenu(MenuRef menu) {
		this.menu = menu;
	}

	public Integer getRank() {
		return rank;
	}

	public void setRank(Integer rank) {
		this.rank = rank;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getAuth() {
		return auth;
	}

	public void setAuth(String auth) {
		this.auth = auth;
	}
	
	public String getLogic() {
		return logic;
	}

	public void setLogic(String logic) {
		this.logic = logic;
	}

	public String getStyle() {
		return style;
	}

	public void setStyle(String style) {
		this.style = style;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public String getButtonType() {
		return buttonType;
	}

	public void setButtonType(String buttonType) {
		this.buttonType = buttonType;
	}

}