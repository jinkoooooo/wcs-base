package operato.logis.asrs.entity;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;

@Table(name = "tb_ac_location_profile", idStrategy = GenerationRule.UUID)
public class TbAcLocationProfile extends xyz.elidom.orm.entity.basic.ElidomStampHook {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 363454864902634309L;

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;

	@Column (name = "area_id", nullable = false, length = 40)
	private String areaId;

	@Column (name = "profile_code", nullable = false, length = 30)
	private String profileCode;

	@Column (name = "profile_name", nullable = false, length = 100)
	private String profileName;

	@Column (name = "aisle_start", nullable = false)
	private Integer aisleStart;

	@Column (name = "aisle_end", nullable = false)
	private Integer aisleEnd;

	@Column (name = "side_codes", nullable = false, length = 20)
	private String sideCodes;

	@Column (name = "bay_start", nullable = false)
	private Integer bayStart;

	@Column (name = "bay_end", nullable = false)
	private Integer bayEnd;

	@Column (name = "level_start", nullable = false)
	private Integer levelStart;

	@Column (name = "level_end", nullable = false)
	private Integer levelEnd;

	@Column (name = "depth_start", nullable = false)
	private Integer depthStart;

	@Column (name = "depth_end", nullable = false)
	private Integer depthEnd;

	@Column (name = "location_type", nullable = false, length = 30)
	private String locationType;

	@Column (name = "code_pattern", nullable = false, length = 200)
	private String codePattern;

	@Column (name = "mixed_load_yn", nullable = false, length = 10)
	private String mixedLoadYn;

	@Column (name = "inbound_allowed_yn", nullable = false, length = 10)
	private String inboundAllowedYn;

	@Column (name = "outbound_allowed_yn", nullable = false, length = 10)
	private String outboundAllowedYn;

	@Column (name = "active_yn", nullable = false, length = 10)
	private String activeYn;
  
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

	public String getProfileCode() {
		return profileCode;
	}

	public void setProfileCode(String profileCode) {
		this.profileCode = profileCode;
	}

	public String getProfileName() {
		return profileName;
	}

	public void setProfileName(String profileName) {
		this.profileName = profileName;
	}

	public Integer getAisleStart() {
		return aisleStart;
	}

	public void setAisleStart(Integer aisleStart) {
		this.aisleStart = aisleStart;
	}

	public Integer getAisleEnd() {
		return aisleEnd;
	}

	public void setAisleEnd(Integer aisleEnd) {
		this.aisleEnd = aisleEnd;
	}

	public String getSideCodes() {
		return sideCodes;
	}

	public void setSideCodes(String sideCodes) {
		this.sideCodes = sideCodes;
	}

	public Integer getBayStart() {
		return bayStart;
	}

	public void setBayStart(Integer bayStart) {
		this.bayStart = bayStart;
	}

	public Integer getBayEnd() {
		return bayEnd;
	}

	public void setBayEnd(Integer bayEnd) {
		this.bayEnd = bayEnd;
	}

	public Integer getLevelStart() {
		return levelStart;
	}

	public void setLevelStart(Integer levelStart) {
		this.levelStart = levelStart;
	}

	public Integer getLevelEnd() {
		return levelEnd;
	}

	public void setLevelEnd(Integer levelEnd) {
		this.levelEnd = levelEnd;
	}

	public Integer getDepthStart() {
		return depthStart;
	}

	public void setDepthStart(Integer depthStart) {
		this.depthStart = depthStart;
	}

	public Integer getDepthEnd() {
		return depthEnd;
	}

	public void setDepthEnd(Integer depthEnd) {
		this.depthEnd = depthEnd;
	}

	public String getLocationType() {
		return locationType;
	}

	public void setLocationType(String locationType) {
		this.locationType = locationType;
	}

	public String getCodePattern() {
		return codePattern;
	}

	public void setCodePattern(String codePattern) {
		this.codePattern = codePattern;
	}

	public String getMixedLoadYn() {
		return mixedLoadYn;
	}

	public void setMixedLoadYn(String mixedLoadYn) {
		this.mixedLoadYn = mixedLoadYn;
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

	public String getActiveYn() {
		return activeYn;
	}

	public void setActiveYn(String activeYn) {
		this.activeYn = activeYn;
	}	
}
