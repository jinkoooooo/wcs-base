package operato.logis.asrs.entity;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;

@Table(name = "tb_ac_location", idStrategy = GenerationRule.UUID)
public class TbAcLocation extends xyz.elidom.orm.entity.basic.ElidomStampHook {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 277681522239572336L;

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;

	@Column (name = "area_id", nullable = false, length = 40)
	private String areaId;

	@Column (name = "location_code", nullable = false, length = 50)
	private String locationCode;

	@Column (name = "aisle_no", nullable = false)
	private Integer aisleNo;

	@Column (name = "side_code", nullable = false, length = 1)
	private String sideCode;

	@Column (name = "bay_no", nullable = false)
	private Integer bayNo;

	@Column (name = "level_no", nullable = false)
	private Integer levelNo;

	@Column (name = "depth_no", nullable = false)
	private Integer depthNo;

	@Column (name = "location_type", nullable = false, length = 30)
	private String locationType;

	@Column (name = "usage_status_code", nullable = false, length = 30)
	private String usageStatusCode;

	@Column (name = "inbound_allowed_yn", nullable = false, length = 10)
	private String inboundAllowedYn;

	@Column (name = "outbound_allowed_yn", nullable = false, length = 10)
	private String outboundAllowedYn;

	@Column (name = "mixed_load_yn", nullable = false, length = 10)
	private String mixedLoadYn;

	@Column (name = "front_priority_yn", nullable = false, length = 10)
	private String frontPriorityYn;

	@Column (name = "dedicated_item_category_id", length = 40)
	private String dedicatedItemCategoryId;

	@Column (name = "max_weight_g")
	private Integer maxWeightG;

	@Column (name = "max_volume_mm3")
	private Integer maxVolumeMm3;

	@Column (name = "sort_seq", nullable = false)
	private Integer sortSeq;

	@Column (name = "active_yn", nullable = false, length = 10)
	private String activeYn;

	@Column (name = "location_grade", length = 10)
	private String locationGrade;

	@Column (name = "access_score")
	private Integer accessScore;

	@Column (name = "primary_access_point_id", length = 40)
	private String primaryAccessPointId;
  
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getAreaId() {
		return areaId;
	}

	public void setAreaId(String areaId) {
		this.areaId = areaId;
	}

	public String getLocationCode() {
		return locationCode;
	}

	public void setLocationCode(String locationCode) {
		this.locationCode = locationCode;
	}

	public Integer getAisleNo() {
		return aisleNo;
	}

	public void setAisleNo(Integer aisleNo) {
		this.aisleNo = aisleNo;
	}

	public String getSideCode() {
		return sideCode;
	}

	public void setSideCode(String sideCode) {
		this.sideCode = sideCode;
	}

	public Integer getBayNo() {
		return bayNo;
	}

	public void setBayNo(Integer bayNo) {
		this.bayNo = bayNo;
	}

	public Integer getLevelNo() {
		return levelNo;
	}

	public void setLevelNo(Integer levelNo) {
		this.levelNo = levelNo;
	}

	public Integer getDepthNo() {
		return depthNo;
	}

	public void setDepthNo(Integer depthNo) {
		this.depthNo = depthNo;
	}

	public String getLocationType() {
		return locationType;
	}

	public void setLocationType(String locationType) {
		this.locationType = locationType;
	}

	public String getUsageStatusCode() {
		return usageStatusCode;
	}

	public void setUsageStatusCode(String usageStatusCode) {
		this.usageStatusCode = usageStatusCode;
	}

	public String getInboundAllowedYn() {
		return inboundAllowedYn;
	}

	public void setInboundAllowedYn(String inboundAllowedYn) {
		this.inboundAllowedYn = inboundAllowedYn;
	}

	public String getOutboundAllowedYn() {
		return outboundAllowedYn;
	}

	public void setOutboundAllowedYn(String outboundAllowedYn) {
		this.outboundAllowedYn = outboundAllowedYn;
	}

	public String getMixedLoadYn() {
		return mixedLoadYn;
	}

	public void setMixedLoadYn(String mixedLoadYn) {
		this.mixedLoadYn = mixedLoadYn;
	}

	public String getFrontPriorityYn() {
		return frontPriorityYn;
	}

	public void setFrontPriorityYn(String frontPriorityYn) {
		this.frontPriorityYn = frontPriorityYn;
	}

	public String getDedicatedItemCategoryId() {
		return dedicatedItemCategoryId;
	}

	public void setDedicatedItemCategoryId(String dedicatedItemCategoryId) {
		this.dedicatedItemCategoryId = dedicatedItemCategoryId;
	}

	public Integer getMaxWeightG() {
		return maxWeightG;
	}

	public void setMaxWeightG(Integer maxWeightG) {
		this.maxWeightG = maxWeightG;
	}

	public Integer getMaxVolumeMm3() {
		return maxVolumeMm3;
	}

	public void setMaxVolumeMm3(Integer maxVolumeMm3) {
		this.maxVolumeMm3 = maxVolumeMm3;
	}

	public Integer getSortSeq() {
		return sortSeq;
	}

	public void setSortSeq(Integer sortSeq) {
		this.sortSeq = sortSeq;
	}

	public String getActiveYn() {
		return activeYn;
	}

	public void setActiveYn(String activeYn) {
		this.activeYn = activeYn;
	}

	public String getLocationGrade() {
		return locationGrade;
	}

	public void setLocationGrade(String locationGrade) {
		this.locationGrade = locationGrade;
	}

	public Integer getAccessScore() {
		return accessScore;
	}

	public void setAccessScore(Integer accessScore) {
		this.accessScore = accessScore;
	}

	public String getPrimaryAccessPointId() {
		return primaryAccessPointId;
	}

	public void setPrimaryAccessPointId(String primaryAccessPointId) {
		this.primaryAccessPointId = primaryAccessPointId;
	}	
}
