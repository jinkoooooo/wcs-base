package operato.logis.asrs.entity;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;

@Table(name = "tb_ac_access_point", idStrategy = GenerationRule.UUID)
public class TbAcAccessPoint extends xyz.elidom.orm.entity.basic.ElidomStampHook {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 161148258632726928L;

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;

	@Column (name = "area_id", nullable = false, length = 40)
	private String areaId;

	@Column (name = "point_code", nullable = false, length = 30)
	private String pointCode;

	@Column (name = "point_name", nullable = false, length = 100)
	private String pointName;

	@Column (name = "point_type", nullable = false, length = 30)
	private String pointType;

	@Column (name = "aisle_no", nullable = false)
	private Integer aisleNo;

	@Column (name = "side_code", nullable = false, length = 10)
	private String sideCode;

	@Column (name = "bay_no", nullable = false)
	private Integer bayNo;

	@Column (name = "level_no", nullable = false)
	private Integer levelNo;

	@Column (name = "depth_no", nullable = false)
	private Integer depthNo;

	@Column (name = "use_for_sort_yn", nullable = false, length = 1)
	private String useForSortYn;

	@Column (name = "active_yn", nullable = false, length = 1)
	private String activeYn;

	@Column (name = "description", length = 500)
	private String description;
  
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

	public String getPointCode() {
		return pointCode;
	}

	public void setPointCode(String pointCode) {
		this.pointCode = pointCode;
	}

	public String getPointName() {
		return pointName;
	}

	public void setPointName(String pointName) {
		this.pointName = pointName;
	}

	public String getPointType() {
		return pointType;
	}

	public void setPointType(String pointType) {
		this.pointType = pointType;
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

	public String getUseForSortYn() {
		return useForSortYn;
	}

	public void setUseForSortYn(String useForSortYn) {
		this.useForSortYn = useForSortYn;
	}

	public String getActiveYn() {
		return activeYn;
	}

	public void setActiveYn(String activeYn) {
		this.activeYn = activeYn;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}	
}
