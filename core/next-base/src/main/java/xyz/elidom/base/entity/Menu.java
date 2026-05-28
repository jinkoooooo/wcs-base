/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.base.entity;

import xyz.elidom.base.entity.relation.MenuRef;
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
import xyz.elidom.sys.SysConstants;

@Table(name = "menus", idStrategy = GenerationRule.UUID, uniqueFields = "domainId,name", indexes = {
	@Index(name = "ix_menu_0", columnList = "domain_id,name", unique = true),
	@Index(name = "ix_menu_1", columnList = "domain_id,parent_id"),
	@Index(name = "ix_menu_2", columnList = "domain_id,category,menu_type,rank") 
}, childEntities = {
	@ChildEntity(entityClass = Menu.class, type = MasterDetailType.ONE_TO_MANY, refFields = "parentId", dataProperty = "submenus", deleteStrategy = DetailRemovalStrategy.EXCEPTION),
	@ChildEntity(entityClass = MenuColumn.class, type = MasterDetailType.ONE_TO_MANY, refFields = "menuId", dataProperty = "columns", deleteStrategy = DetailRemovalStrategy.EXCEPTION),
	@ChildEntity(entityClass = MenuButton.class, type = MasterDetailType.ONE_TO_MANY, refFields = "menuId", dataProperty = "buttons", deleteStrategy = DetailRemovalStrategy.EXCEPTION),
	@ChildEntity(entityClass = MenuParam.class, type = MasterDetailType.ONE_TO_MANY, refFields = "menuId", dataProperty = "params", deleteStrategy = DetailRemovalStrategy.EXCEPTION) 
})
public class Menu extends ElidomStampHook {

	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 6243144277279053281L;

	@PrimaryKey
	@Column(name = "id", nullable = false, length = OrmConstants.FIELD_SIZE_UUID)
	private String id;

	@Column(name = "name", nullable = false, length = OrmConstants.FIELD_SIZE_NAME)
	private String name;

	@Column(name = "description", length = OrmConstants.FIELD_SIZE_DESCRIPTION)
	private String description;

	@Column(name = "parent_id", length = OrmConstants.FIELD_SIZE_UUID)
	private String parentId;

	@Relation(field = "parentId", selfReference = true)
	private MenuRef parent;

	@Column(name = "template", length = 128)
	private String template;

	@Column(name = "menu_type", length = OrmConstants.FIELD_SIZE_CATEGORY)
	private String menuType;

	@Column(name = "category", length = OrmConstants.FIELD_SIZE_CATEGORY)
	private String category;

	@Column(name = "rank")
	private Integer rank;

	@Column(name = "icon_path", length = OrmConstants.FIELD_SIZE_FILE_PATH)
	private String iconPath;

	@Column(name = "hidden_flag")
	private Boolean hiddenFlag;

	@Column(name = "routing", length = OrmConstants.FIELD_SIZE_NAME)
	private String routing;

	@Column(name = "routing_type", length = 20)
	private String routingType;

	@Column(name = "detail_form_id", length = OrmConstants.FIELD_SIZE_NAME)
	private String detailFormId;

	@Column(name = "detail_layout", length = OrmConstants.FIELD_SIZE_NAME)
	private String detailLayout;

	@Column(name = "resource_type", length = 15)
	private String resourceType;

	@Column(name = "resource_name", length = OrmConstants.FIELD_SIZE_NAME)
	private String resourceName;

	@Column(name = "resource_url", length = OrmConstants.FIELD_SIZE_URL)
	private String resourceUrl;

	@Column(name = "grid_save_url", length = OrmConstants.FIELD_SIZE_URL)
	private String gridSaveUrl;

	@Column(name = "id_field", length = OrmConstants.FIELD_SIZE_NAME)
	private String idField;

	@Column(name = "title_field", length = OrmConstants.FIELD_SIZE_NAME)
	private String titleField;
	
	@Column(name = "desc_field", length = OrmConstants.FIELD_SIZE_NAME)
	private String descField;

	@Column(name = "pagination")
	private Boolean pagination;

	@Column(name = "pagination_value" ,length = 100)
	private String paginationValue;

	@Column(name = "items_prop", length = OrmConstants.FIELD_SIZE_NAME)
	private String itemsProp;

	@Column(name = "total_prop", length = OrmConstants.FIELD_SIZE_NAME)
	private String totalProp;

	@Column(name = "fixed_columns")
	private Integer fixedColumns;

	@Column(name = "provision_type" ,length = 100)
	private String provisionType;

	/**
	 * Menu index 권한 반영을 위한 모델
	 */
	@Ignore
	private String Auth;

	@Ignore
	private String title;

	/**
	 * Default Constructor
	 */
	public Menu() {
	}
	
	public Menu(String id) {
		this.id = id;
	}

	public Menu(Long domainId, String name) {
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
	 * @return the parentId
	 */
	public String getParentId() {
		return parentId;
	}

	/**
	 * @param parentId
	 *            the parentId to set
	 */
	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	/**
	 * @return the parent
	 */
	public MenuRef getParent() {
		return parent;
	}

	/**
	 * @param parent
	 *            the parent to set
	 */
	public void setParent(MenuRef parent) {
		this.parent = parent;
	}

	/**
	 * @return the template
	 */
	public String getTemplate() {
		return template;
	}

	/**
	 * @param template
	 *            the template to set
	 */
	public void setTemplate(String template) {
		this.template = template;
	}

	/**
	 * @return the menuType
	 */
	public String getMenuType() {
		return menuType;
	}

	/**
	 * @param menuType
	 *            the menuType to set
	 */
	public void setMenuType(String menuType) {
		this.menuType = menuType;
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
	 * @return the rank
	 */
	public Integer getRank() {
		return rank;
	}

	/**
	 * @param rank
	 *            the rank to set
	 */
	public void setRank(Integer rank) {
		this.rank = rank;
	}

	/**
	 * @return the iconPath
	 */
	public String getIconPath() {
		return iconPath;
	}

	/**
	 * @param iconPath
	 *            the iconPath to set
	 */
	public void setIconPath(String iconPath) {
		this.iconPath = iconPath;
	}

	/**
	 * @return the hiddenFlag
	 */
	public Boolean getHiddenFlag() {
		return hiddenFlag;
	}

	/**
	 * @param hiddenFlag
	 *            the hiddenFlag to set
	 */
	public void setHiddenFlag(Boolean hiddenFlag) {
		this.hiddenFlag = hiddenFlag;
	}

	/**
	 * @return the routing
	 */
	public String getRouting() {
		return routing;
	}

	/**
	 * @param routing
	 *            the routing to set
	 */
	public void setRouting(String routing) {
		this.routing = routing;
	}

	/**
	 * @return the routingType
	 */
	public String getRoutingType() {
		return routingType;
	}

	/**
	 * @param routingType
	 *            the routingType to set
	 */
	public void setRoutingType(String routingType) {
		this.routingType = routingType;
	}

	/**
	 * @return the detailFormId
	 */
	public String getDetailFormId() {
		return detailFormId;
	}

	/**
	 * @param detailFormId
	 *            the detailFormId to set
	 */
	public void setDetailFormId(String detailFormId) {
		this.detailFormId = detailFormId;
	}

	/**
	 * @return the detailLayout
	 */
	public String getDetailLayout() {
		return detailLayout;
	}

	/**
	 * @param detailLayout
	 *            the detailLayout to set
	 */
	public void setDetailLayout(String detailLayout) {
		this.detailLayout = detailLayout;
	}

	/**
	 * @return the resourceType
	 */
	public String getResourceType() {
		return resourceType;
	}

	/**
	 * @param resourceType
	 *            the resourceType to set
	 */
	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

	/**
	 * @return the resourceName
	 */
	public String getResourceName() {
		return resourceName;
	}

	/**
	 * @param resourceName
	 *            the resourceName to set
	 */
	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}

	/**
	 * @return the resourceUrl
	 */
	public String getResourceUrl() {
		return resourceUrl;
	}

	/**
	 * @param resourceUrl
	 *            the resourceUrl to set
	 */
	public void setResourceUrl(String resourceUrl) {
		this.resourceUrl = resourceUrl;
	}

	/**
	 * @return the gridSaveUrl
	 */
	public String getGridSaveUrl() {
		return gridSaveUrl;
	}

	/**
	 * @param gridSaveUrl
	 *            the gridSaveUrl to set
	 */
	public void setGridSaveUrl(String gridSaveUrl) {
		this.gridSaveUrl = gridSaveUrl;
	}

	/**
	 * @return the idField
	 */
	public String getIdField() {
		return idField;
	}

	/**
	 * @param idField
	 *            the idField to set
	 */
	public void setIdField(String idField) {
		this.idField = idField;
	}

	/**
	 * @return the titleField
	 */
	public String getTitleField() {
		return titleField;
	}

	/**
	 * @param titleField
	 *            the titleField to set
	 */
	public void setTitleField(String titleField) {
		this.titleField = titleField;
	}
	
	/**
	 * @return the descField
	 */
	public String getDescField() {
		return descField;
	}

	/**
	 * @param descField
	 *            the descField to set
	 */
	public void setDescField(String descField) {
		this.descField = descField;
	}

	/**
	 * @return the pagination
	 */
	public Boolean getPagination() {
		return pagination;
	}

	/**
	 * @param pagination
	 *            the pagination to set
	 */
	public void setPagination(Boolean pagination) {
		this.pagination = pagination;
	}

	/**
	 * @return the itemsProp
	 */
	public String getItemsProp() {
		return itemsProp;
	}

	/**
	 * @param itemsProp
	 *            the itemsProp to set
	 */
	public void setItemsProp(String itemsProp) {
		this.itemsProp = itemsProp;
	}

	/**
	 * @return the totalProp
	 */
	public String getTotalProp() {
		return totalProp;
	}

	/**
	 * @param totalProp
	 *            the totalProp to set
	 */
	public void setTotalProp(String totalProp) {
		this.totalProp = totalProp;
	}

	/**
	 * @return the fixedColumns
	 */
	public Integer getFixedColumns() {
		return fixedColumns;
	}

	/**
	 * @param fixedColumns
	 *            the fixedColumns to set
	 */
	public void setFixedColumns(Integer fixedColumns) {
		this.fixedColumns = fixedColumns;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @param title
	 *            the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @return the auth
	 */
	public String getAuth() {
		return Auth;
	}

	/**
	 * @param auth
	 *            the auth to set
	 */
	public void setAuth(String auth) {
		Auth = auth;
	}

	@Override
	public void beforeCreate() {
		super.beforeCreate();

		if (this.hiddenFlag == null) {
			this.hiddenFlag = false;
		}

		if (this.parentId != null && this.parentId.equals(SysConstants.EMPTY_STRING)) {
			this.parentId = null;
		}
	}

	@Override
	public void beforeUpdate() {
		super.beforeUpdate();

		if (this.hiddenFlag == null) {
			this.hiddenFlag = false;
		}

		if (this.parentId != null && this.parentId.equals(SysConstants.EMPTY_STRING)) {
			this.parentId = null;
		}
	}

	public String getPaginationValue() {
		return paginationValue;
	}

	public void setPaginationValue(String paginationValue) {
		this.paginationValue = paginationValue;
	}

	public String getProvisionType() {
		return provisionType;
	}

	public void setProvisionType(String provisionType) {
		this.provisionType = provisionType;
	}
}