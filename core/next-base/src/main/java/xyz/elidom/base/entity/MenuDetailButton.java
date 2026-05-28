/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.base.entity;


import xyz.elidom.base.entity.relation.MenuDetailRef;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.ColumnType;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Index;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Relation;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.entity.basic.DomainStampHook;

@Table(name = "menu_detail_buttons", idStrategy = GenerationRule.UUID, uniqueFields = "menuDetailId,name", indexes = { 
	@Index(name = "ix_menu_detail_btn_0", columnList = "menu_detail_id,name", unique = true),
	@Index(name = "ix_menu_detail_btn_1", columnList = "menu_detail_id")
})
public class MenuDetailButton extends DomainStampHook {

	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = -1232581993821819341L;

	@PrimaryKey
	@Column (name = "id", nullable = false, length = OrmConstants.FIELD_SIZE_UUID)
	private String id;

	@Column (name = "menu_detail_id", nullable = false, length = OrmConstants.FIELD_SIZE_UUID)
	private String menuDetailId;
	
	@Relation(field = "menuDetailId")
	private MenuDetailRef menuDetail;

	@Column (name = "name", nullable = false, length = OrmConstants.FIELD_SIZE_NAME)
	private String name;

	@Column (name = "icon", length = 32)
	private String icon;
	
	@Column (name = "style", length = 32)
	private String style;
	
	@Column (name = "logic", type = ColumnType.TEXT)
	private String logic;
	  
	public MenuDetailButton() {
	}
	
	public MenuDetailButton(String menuDetailId) {
		this.menuDetailId = menuDetailId;
	}
	
	public MenuDetailButton(String menuDetailId, String name) {
		this.menuDetailId = menuDetailId;
		this.name = name;
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

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getMenuDetailId() {
		return menuDetailId;
	}

	public void setMenuDetailId(String menuDetailId) {
		this.menuDetailId = menuDetailId;
	}

	public MenuDetailRef getMenuDetail() {
		return menuDetail;
	}

	public void setMenuDetail(MenuDetailRef menuDetail) {
		this.menuDetail = menuDetail;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

}