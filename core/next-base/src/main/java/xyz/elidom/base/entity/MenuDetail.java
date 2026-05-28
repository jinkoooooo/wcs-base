/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.base.entity;

import java.util.List;

//import xyz.elidom.base.entity.relation.MenuRef;
import xyz.elidom.base.entity.relation.ResourceRef;
import xyz.elidom.dbist.annotation.ChildEntity;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.DetailRemovalStrategy;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Ignore;
import xyz.elidom.dbist.annotation.Index;
import xyz.elidom.dbist.annotation.MasterDetailType;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Relation;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

@Table(name = "menu_details", idStrategy = GenerationRule.UUID, uniqueFields = "menuId,name", indexes = { 
	@Index(name = "ix_menu_detail_0", columnList = "menu_id,name", unique = true),
	@Index(name = "ix_menu_detail_1", columnList = "menu_id")
}, childEntities = {
	@ChildEntity(entityClass = MenuDetailColumn.class, type = MasterDetailType.ONE_TO_MANY, refFields = "menuDetailId", dataProperty = "columns", deleteStrategy = DetailRemovalStrategy.EXCEPTION),
	@ChildEntity(entityClass = MenuDetailButton.class, type = MasterDetailType.ONE_TO_MANY, refFields = "menuDetailId", dataProperty = "buttons", deleteStrategy = DetailRemovalStrategy.EXCEPTION),
})
public class MenuDetail extends ElidomStampHook {

	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = -6384928607807593077L;

	@PrimaryKey
	@Column(name = "id", nullable = false, length = OrmConstants.FIELD_SIZE_UUID)
	private String id;

	@Column(name = "menu_id", nullable = false, length = OrmConstants.FIELD_SIZE_UUID)
	private String menuId;
	
//	@Relation(field = "menuId")
//	private MenuRef menu;

	@Column(name = "name", nullable = false, length = OrmConstants.FIELD_SIZE_NAME)
	private String name;

	@Column(name = "view_section", nullable = false, length = OrmConstants.FIELD_SIZE_NAME)
	private String viewSection;

	@Column(name = "entity_id", length = OrmConstants.FIELD_SIZE_UUID)
	private String entityId;

	@Relation(field = "entityId")
	private ResourceRef entity;

	@Column(name = "data_prop", length = OrmConstants.FIELD_SIZE_NAME)
	private String dataProp;

	@Column(name = "association", length = 15)
	private String association;

	@Column (name = "search_url", length = OrmConstants.FIELD_SIZE_URL)
	private String searchUrl;

	@Column(name = "save_url", length = OrmConstants.FIELD_SIZE_URL)
	private String saveUrl;
	
	@Column (name = "master_field", length = OrmConstants.FIELD_SIZE_NAME)
	private String masterField;
	
	@Column (name = "custom_view", length = OrmConstants.FIELD_SIZE_NAME)
	private String customView;	
	
	@Ignore
	private MenuDetail menu;
	
	@Ignore
	private List<MenuDetailColumn> columns;
	
	@Ignore
	private List<MenuDetailButton> buttons;
	
	public MenuDetail() {
	}
	
	public MenuDetail(String id) {
		this.id = id;
	}

	public MenuDetail(Long domainId, String name) {
		this.domainId = domainId;
		this.name = name;
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
	
//	public MenuRef getMenu() {
//		return menu;
//	}

//	public void setMenu(MenuRef menu) {
//		this.menu = menu;
//	}

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
	 * @return the viewSection
	 */
	public String getViewSection() {
		return viewSection;
	}

	/**
	 * @param viewSection the viewSection to set
	 */
	public void setViewSection(String viewSection) {
		this.viewSection = viewSection;
	}

	/**
	 * @return the entityId
	 */
	public String getEntityId() {
		return entityId;
	}

	/**
	 * @param entityId the entityId to set
	 */
	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}

	/**
	 * @return the entity
	 */
	public ResourceRef getEntity() {
		return entity;
	}

	/**
	 * @param entity
	 *            the entity to set
	 */
	public void setEntity(ResourceRef entity) {
		this.entity = entity;
	}

	/**
	 * @return the dataProp
	 */
	public String getDataProp() {
		return dataProp;
	}

	/**
	 * @param dataProp the dataProp to set
	 */
	public void setDataProp(String dataProp) {
		this.dataProp = dataProp;
	}

	/**
	 * @return the association
	 */
	public String getAssociation() {
		return association;
	}

	/**
	 * @param association the association to set
	 */
	public void setAssociation(String association) {
		this.association = association;
	}

	/**
	 * @return the searchUrl
	 */
	public String getSearchUrl() {
		return searchUrl;
	}

	/**
	 * @param searchUrl the searchUrl to set
	 */
	public void setSearchUrl(String searchUrl) {
		this.searchUrl = searchUrl;
	}

	/**
	 * @return the saveUrl
	 */
	public String getSaveUrl() {
		return saveUrl;
	}

	/**
	 * @param saveUrl the saveUrl to set
	 */
	public void setSaveUrl(String saveUrl) {
		this.saveUrl = saveUrl;
	}

	/**
	 * @return the masterField
	 */
	public String getMasterField() {
		return masterField;
	}

	/**
	 * @param masterField the masterField to set
	 */
	public void setMasterField(String masterField) {
		this.masterField = masterField;
	}

	/**
	 * @return the customView
	 */
	public String getCustomView() {
		return customView;
	}

	/**
	 * @param customView the customView to set
	 */
	public void setCustomView(String customView) {
		this.customView = customView;
	}

	public MenuDetail getMenu() {
		return menu;
	}

	public void setMenu(MenuDetail menu) {
		this.menu = menu;
	}

	/**
	 * @return the menuDetailColumns
	 */
	public List<MenuDetailColumn> getColumns() {
		return columns;
	}

	/**
	 * @param menuDetailColumns the menuDetailColumns to set
	 */
	public void setColumns(List<MenuDetailColumn> menuDetailColumns) {
		this.columns = menuDetailColumns;
	}

	/**
	 * @return the menuDetailButtons
	 */
	public List<MenuDetailButton> getButtons() {
		return buttons;
	}

	/**
	 * @param menuDetailButtons the menuDetailButtons to set
	 */
	public void setButtons(List<MenuDetailButton> menuDetailButtons) {
		this.buttons = menuDetailButtons;
	}
	
}