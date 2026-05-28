package operato.logis.asrs.entity;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;

@Table(name = "tb_ac_item_master", idStrategy = GenerationRule.UUID)
public class TbAcItemMaster extends xyz.elidom.orm.entity.basic.ElidomStampHook {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 128665826269872503L;

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;

	@Column (name = "item_code", nullable = false, length = 50)
	private String itemCode;

	@Column (name = "item_name", nullable = false, length = 200)
	private String itemName;

	@Column (name = "item_category_id", nullable = false, length = 40)
	private String itemCategoryId;

	@Column (name = "operation_profile_id", length = 40)
	private String operationProfileId;

	@Column (name = "industry_type", nullable = false, length = 30)
	private String industryType;

	@Column (name = "base_uom", nullable = false, length = 20)
	private String baseUom;

	@Column (name = "handling_unit_type", nullable = false, length = 20)
	private String handlingUnitType;

	@Column (name = "outbound_unit_type", nullable = false, length = 20)
	private String outboundUnitType;

	@Column (name = "length_mm", nullable = false)
	private Integer lengthMm;

	@Column (name = "width_mm", nullable = false)
	private Integer widthMm;

	@Column (name = "height_mm", nullable = false)
	private Integer heightMm;

	@Column (name = "weight_g", nullable = false)
	private Integer weightG;

	@Column (name = "volume_mm3")
	private Integer volumeMm3;

	@Column (name = "storage_temp_type", nullable = false, length = 20)
	private String storageTempType;

	@Column (name = "lot_control_yn", nullable = false, length = 10)
	private String lotControlYn;

	@Column (name = "expiry_control_yn", nullable = false, length = 10)
	private String expiryControlYn;

	@Column (name = "serial_control_yn", nullable = false, length = 10)
	private String serialControlYn;

	@Column (name = "partial_pick_yn", nullable = false, length = 10)
	private String partialPickYn;

	@Column (name = "mixed_load_yn", nullable = false, length = 10)
	private String mixedLoadYn;

	@Column (name = "fragile_yn", nullable = false, length = 10)
	private String fragileYn;

	@Column (name = "heavy_yn", nullable = false, length = 10)
	private String heavyYn;

	@Column (name = "quarantine_required_yn", nullable = false, length = 10)
	private String quarantineRequiredYn;

	@Column (name = "allocation_rule_code", nullable = false, length = 30)
	private String allocationRuleCode;

	@Column (name = "rotation_profile_code", nullable = false, length = 30)
	private String rotationProfileCode;

	@Column (name = "storage_grade_seed", nullable = false, length = 10)
	private String storageGradeSeed;

	@Column (name = "ext_attr", length = 4000)
	private String extAttr;

	@Column (name = "active_yn", nullable = false, length = 10)
	private String activeYn;
  
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getItemCode() {
		return itemCode;
	}

	public void setItemCode(String itemCode) {
		this.itemCode = itemCode;
	}

	public String getItemName() {
		return itemName;
	}

	public void setItemName(String itemName) {
		this.itemName = itemName;
	}

	public String getItemCategoryId() {
		return itemCategoryId;
	}

	public void setItemCategoryId(String itemCategoryId) {
		this.itemCategoryId = itemCategoryId;
	}

	public String getOperationProfileId() {
		return operationProfileId;
	}

	public void setOperationProfileId(String operationProfileId) {
		this.operationProfileId = operationProfileId;
	}

	public String getIndustryType() {
		return industryType;
	}

	public void setIndustryType(String industryType) {
		this.industryType = industryType;
	}

	public String getBaseUom() {
		return baseUom;
	}

	public void setBaseUom(String baseUom) {
		this.baseUom = baseUom;
	}

	public String getHandlingUnitType() {
		return handlingUnitType;
	}

	public void setHandlingUnitType(String handlingUnitType) {
		this.handlingUnitType = handlingUnitType;
	}

	public String getOutboundUnitType() {
		return outboundUnitType;
	}

	public void setOutboundUnitType(String outboundUnitType) {
		this.outboundUnitType = outboundUnitType;
	}

	public Integer getLengthMm() {
		return lengthMm;
	}

	public void setLengthMm(Integer lengthMm) {
		this.lengthMm = lengthMm;
	}

	public Integer getWidthMm() {
		return widthMm;
	}

	public void setWidthMm(Integer widthMm) {
		this.widthMm = widthMm;
	}

	public Integer getHeightMm() {
		return heightMm;
	}

	public void setHeightMm(Integer heightMm) {
		this.heightMm = heightMm;
	}

	public Integer getWeightG() {
		return weightG;
	}

	public void setWeightG(Integer weightG) {
		this.weightG = weightG;
	}

	public Integer getVolumeMm3() {
		return volumeMm3;
	}

	public void setVolumeMm3(Integer volumeMm3) {
		this.volumeMm3 = volumeMm3;
	}

	public String getStorageTempType() {
		return storageTempType;
	}

	public void setStorageTempType(String storageTempType) {
		this.storageTempType = storageTempType;
	}

	public String getLotControlYn() {
		return lotControlYn;
	}

	public void setLotControlYn(String lotControlYn) {
		this.lotControlYn = lotControlYn;
	}

	public String getExpiryControlYn() {
		return expiryControlYn;
	}

	public void setExpiryControlYn(String expiryControlYn) {
		this.expiryControlYn = expiryControlYn;
	}

	public String getSerialControlYn() {
		return serialControlYn;
	}

	public void setSerialControlYn(String serialControlYn) {
		this.serialControlYn = serialControlYn;
	}

	public String getPartialPickYn() {
		return partialPickYn;
	}

	public void setPartialPickYn(String partialPickYn) {
		this.partialPickYn = partialPickYn;
	}

	public String getMixedLoadYn() {
		return mixedLoadYn;
	}

	public void setMixedLoadYn(String mixedLoadYn) {
		this.mixedLoadYn = mixedLoadYn;
	}

	public String getFragileYn() {
		return fragileYn;
	}

	public void setFragileYn(String fragileYn) {
		this.fragileYn = fragileYn;
	}

	public String getHeavyYn() {
		return heavyYn;
	}

	public void setHeavyYn(String heavyYn) {
		this.heavyYn = heavyYn;
	}

	public String getQuarantineRequiredYn() {
		return quarantineRequiredYn;
	}

	public void setQuarantineRequiredYn(String quarantineRequiredYn) {
		this.quarantineRequiredYn = quarantineRequiredYn;
	}

	public String getAllocationRuleCode() {
		return allocationRuleCode;
	}

	public void setAllocationRuleCode(String allocationRuleCode) {
		this.allocationRuleCode = allocationRuleCode;
	}

	public String getRotationProfileCode() {
		return rotationProfileCode;
	}

	public void setRotationProfileCode(String rotationProfileCode) {
		this.rotationProfileCode = rotationProfileCode;
	}

	public String getStorageGradeSeed() {
		return storageGradeSeed;
	}

	public void setStorageGradeSeed(String storageGradeSeed) {
		this.storageGradeSeed = storageGradeSeed;
	}

	public String getExtAttr() {
		return extAttr;
	}

	public void setExtAttr(String extAttr) {
		this.extAttr = extAttr;
	}

	public String getActiveYn() {
		return activeYn;
	}

	public void setActiveYn(String activeYn) {
		this.activeYn = activeYn;
	}	
}
