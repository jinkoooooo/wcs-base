package operato.logis.asrs.entity;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;

@Table(name = "tb_ac_item_category", idStrategy = GenerationRule.UUID)
public class TbAcItemCategory extends xyz.elidom.orm.entity.basic.ElidomStampHook {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 655283505995516308L;

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;

	@Column (name = "category_code", nullable = false, length = 30)
	private String categoryCode;

	@Column (name = "category_name", nullable = false, length = 100)
	private String categoryName;

	@Column (name = "parent_category_id", length = 40)
	private String parentCategoryId;

	@Column (name = "default_operation_profile_id", nullable = false, length = 40)
	private String defaultOperationProfileId;

	@Column (name = "description", length = 500)
	private String description;

	@Column (name = "active_yn", nullable = false, length = 10)
	private String activeYn;
  
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCategoryCode() {
		return categoryCode;
	}

	public void setCategoryCode(String categoryCode) {
		this.categoryCode = categoryCode;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

	public String getParentCategoryId() {
		return parentCategoryId;
	}

	public void setParentCategoryId(String parentCategoryId) {
		this.parentCategoryId = parentCategoryId;
	}

	public String getDefaultOperationProfileId() {
		return defaultOperationProfileId;
	}

	public void setDefaultOperationProfileId(String defaultOperationProfileId) {
		this.defaultOperationProfileId = defaultOperationProfileId;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getActiveYn() {
		return activeYn;
	}

	public void setActiveYn(String activeYn) {
		this.activeYn = activeYn;
	}	
}
