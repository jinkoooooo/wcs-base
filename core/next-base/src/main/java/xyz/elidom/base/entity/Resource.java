/* Copyright © Nearsolution Inc. All rights reserved. */
package xyz.elidom.base.entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.ClassUtils;

import xyz.elidom.base.entity.relation.ResourceRef;
import xyz.elidom.core.CoreMessageConstants;
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
import xyz.elidom.exception.server.ElidomServiceException;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.entity.basic.ElidomStampHook;
import xyz.elidom.sys.config.ModuleConfigSet;
import xyz.elidom.sys.system.config.module.IModuleProperties;
import xyz.elidom.sys.util.MessageUtil;
import xyz.elidom.sys.util.SysValueUtil;
import xyz.elidom.util.BeanUtil;

@Table(name = "entities", idStrategy = GenerationRule.UUID, uniqueFields = "domainId,name", indexes = { 
	@Index(name = "ix_entity_0", columnList = "domain_id,name", unique = true),
	@Index(name = "ix_entity_1", columnList = "domain_id"),
	@Index(name = "ix_entity_2", columnList = "domain_id,bundle"),
	@Index(name = "ix_entity_3", columnList = "domain_id,master_id")
}, childEntities = {
	@ChildEntity(entityClass = ResourceColumn.class, type = MasterDetailType.ONE_TO_MANY, refFields = "entityId", dataProperty = "columns", deleteStrategy = DetailRemovalStrategy.EXCEPTION) 
})
public class Resource extends ElidomStampHook {

	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 4182272142848231616L;

	@PrimaryKey
	@Column(name = "id", nullable = false, length = OrmConstants.FIELD_SIZE_UUID)
	private String id;

	@Column(name = "name", nullable = false, length = OrmConstants.FIELD_SIZE_NAME)
	private String name;

	@Column(name = "description", length = OrmConstants.FIELD_SIZE_DESCRIPTION)
	private String description;

	@Column(name = "bundle", nullable = false, length = OrmConstants.FIELD_SIZE_CATEGORY)
	private String bundle;

	@Column(name = "table_name", nullable = false, length = OrmConstants.FIELD_SIZE_NAME)
	private String tableName;

	@Column(name = "search_url", length = 64)
	private String searchUrl;

	@Column(name = "multi_save_url", length = 64)
	private String multiSaveUrl;

	@Column(name = "id_type", length = 15)
	private String idType;
	
	@Column(name = "id_field", length = OrmConstants.FIELD_SIZE_NAME)
	private String idField;

	@Column(name = "title_field", length = OrmConstants.FIELD_SIZE_NAME)
	private String titleField;
	
	@Column(name = "desc_field", length = OrmConstants.FIELD_SIZE_NAME)
	private String descField;

	@Column(name = "master_id", length = OrmConstants.FIELD_SIZE_UUID)
	private String masterId;

	@Relation(field = "masterId")
	private ResourceRef master;

	@Column(name = "association", length = 15)
	private String association;

	@Column(name = "data_prop", length = OrmConstants.FIELD_SIZE_NAME)
	private String dataProp;

	@Column(name = "ref_field", length = OrmConstants.FIELD_SIZE_NAME)
	private String refField;

	@Column(name = "del_strategy", length = 20)
	private String delStrategy;

	@Column(name = "fixed_columns")
	private Integer fixedColumns;

	@Column(name = "active")
	private Boolean active;
	
	@Column(name = "ext_entity")
	private Boolean extEntity;

	@Ignore
	private String title;

	@Ignore
	private List<ResourceColumn> items;

	public Resource() {
	}

	public Resource(String id) {
		this.id = id;
	}
	
	public Resource(Long domainId, String name) {
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
	 * @return the bundle
	 */
	public String getBundle() {
		return bundle;
	}

	/**
	 * @param bundle the bundle to set
	 */
	public void setBundle(String bundle) {
		this.bundle = bundle;
	}

	/**
	 * @return the tableName
	 */
	public String getTableName() {
		return tableName;
	}

	/**
	 * @param tableName the tableName to set
	 */
	public void setTableName(String tableName) {
		this.tableName = tableName;
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
	 * @return the multiSaveUrl
	 */
	public String getMultiSaveUrl() {
		return multiSaveUrl;
	}

	/**
	 * @param multiSaveUrl the multiSaveUrl to set
	 */
	public void setMultiSaveUrl(String multiSaveUrl) {
		this.multiSaveUrl = multiSaveUrl;
	}

	/**
	 * @return the idField
	 */
	public String getIdField() {
		return idField;
	}

	/**
	 * @param idField the idField to set
	 */
	public void setIdField(String idField) {
		this.idField = idField;
	}

	/**
	 * @return the idType
	 */
	public String getIdType() {
		return idType;
	}

	/**
	 * @param idType the idType to set
	 */
	public void setIdType(String idType) {
		this.idType = idType;
	}

	/**
	 * @return the active
	 */
	public Boolean getActive() {
		return active;
	}

	/**
	 * @param active the active to set
	 */
	public void setActive(Boolean active) {
		this.active = active;
	}

	/**
	 * @return the titleField
	 */
	public String getTitleField() {
		return titleField;
	}

	/**
	 * @param titleField the titleField to set
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
	 * @param descField the descField to set
	 */
	public void setDescField(String descField) {
		this.descField = descField;
	}

	/**
	 * @return the masterId
	 */
	public String getMasterId() {
		return masterId;
	}

	/**
	 * @param masterId the masterId to set
	 */
	public void setMasterId(String masterId) {
		this.masterId = masterId;
	}

	/**
	 * @return the master
	 */
	public ResourceRef getMaster() {
		return master;
	}

	/**
	 * @param master the master to set
	 */
	public void setMaster(ResourceRef master) {
		this.master = master;
	}

	/**
	 * @return the refField
	 */
	public String getRefField() {
		return refField;
	}

	/**
	 * @param refField the refField to set
	 */
	public void setRefField(String refField) {
		this.refField = refField;
	}

	/**
	 * @return the delStrategy
	 */
	public String getDelStrategy() {
		return delStrategy;
	}

	/**
	 * @param delStrategy the delStrategy to set
	 */
	public void setDelStrategy(String delStrategy) {
		this.delStrategy = delStrategy;
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
	 * @return the fixedColumns
	 */
	public Integer getFixedColumns() {
		return fixedColumns;
	}

	/**
	 * @param fixedColumns the fixedColumns to set
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
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @return ResourceColumn List
	 */
	public List<ResourceColumn> getItems() {
		return this.items;
	}

	/**
	 * @param items ResourceColumn List
	 */
	public void setItems(List<ResourceColumn> items) {
		this.items = items;
	}

	/**
	 * ResourceColumn 추가
	 * 
	 * @param item
	 */
	public void addItem(ResourceColumn item) {
		if (items == null) {
			items = new ArrayList<ResourceColumn>();
		}

		items.add(item);
	}

	/**
	 * Ext Resource 여부
	 * 
	 * @return
	 */
	public Boolean getExtEntity() {
		return extEntity;
	}

	public void setExtEntity(Boolean extEntity) {
		this.extEntity = extEntity;
	}

	/**
	 * Resource에 대한 resource columns를 조회
	 * 
	 * @return
	 */
	public List<ResourceColumn> resourceColumns() {
		if (SysValueUtil.isEmpty(this.items)) {
			String sql = "SELECT * FROM entity_columns where entity_id = :resourceId order by rank";
			this.items = BeanUtil.get(IQueryManager.class).selectListBySql(sql, SysValueUtil.newMap("resourceId", this.id), ResourceColumn.class, 0, 0);
		}

		return this.items;
	}

	/**
	 * List Rank가 0보다 큰 컬럼들만 List Rank로 asc해서 리턴
	 * 
	 * @return
	 */
	public List<ResourceColumn> resourceColumnsByListRank() {
		String sql = "SELECT * FROM entity_columns where entity_id = :resourceId and grid_rank > 0 order by grid_rank";
		return BeanUtil.get(IQueryManager.class).selectListBySql(sql, SysValueUtil.newMap("resourceId", this.id), ResourceColumn.class, 0, 0);
	}

	/**
	 * entityName으로 부터 클래스를 찾아 리턴
	 * 
	 * @param name
	 * @return
	 */
	public static Class<?> findClassByEntityName(String name) {
		if(SysValueUtil.isEqual("Entity", name)) {
			name = "Resource";
		} else if(SysValueUtil.isEqual("EntityColumn", name)) {
			name = "ResourceColumn";
		}
		
		ModuleConfigSet configSet = BeanUtil.get(ModuleConfigSet.class);
		Iterator<IModuleProperties> modules = configSet.getModules().iterator();

		while (modules.hasNext()) {
			IModuleProperties mod = modules.next();
			String pkg = mod.getScanEntityPackage();
			String entityName = pkg + OrmConstants.DOT + name;
			try {
				return ClassUtils.getClass(entityName);
			} catch (ClassNotFoundException e) {
				continue;
			}
		}

		throw new ElidomServiceException(CoreMessageConstants.IS_NOT_ENTITY, "[{0}] is not a Entity.", MessageUtil.params(name));
	}
	
}