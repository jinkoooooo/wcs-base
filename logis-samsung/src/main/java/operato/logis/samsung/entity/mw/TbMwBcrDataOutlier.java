package operato.logis.samsung.entity.mw;

import java.util.Date; 
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;

@Table(name = "tb_mw_bcr_data_outlier", idStrategy = GenerationRule.UUID)
public class TbMwBcrDataOutlier extends xyz.elidom.orm.entity.basic.ElidomStampHook {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 208830522424172250L;

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;

	@Column (name = "bcr_data_id", nullable = false, length = 40)
	private String bcrDataId;

	@Column (name = "inner_item_code", nullable = false, length = 100)
	private String innerItemCode;

	@Column (name = "item_code", nullable = false, length = 100)
	private String itemCode;

	@Column (name = "barcodedata", nullable = false, length = 100)
	private String barcodedata;

	@Column (name = "length_mm")
	private Integer lengthMm;

	@Column (name = "width_mm")
	private Integer widthMm;

	@Column (name = "height_mm")
	private Integer heightMm;

	@Column (name = "master_length_mm")
	private Integer masterLengthMm;

	@Column (name = "master_width_mm")
	private Integer masterWidthMm;

	@Column (name = "master_height_mm")
	private Integer masterHeightMm;

	@Column (name = "diff_length_mm")
	private Integer diffLengthMm;

	@Column (name = "diff_width_mm")
	private Integer diffWidthMm;

	@Column (name = "diff_height_mm")
	private Integer diffHeightMm;

	@Column (name = "reg_dt")
	private Date regDt;

	@Column (name = "detected_at")
	private Date detectedAt;

	@Column (name = "resolved_yn", length = 10)
	private String resolvedYn;

	@Column (name = "resolved_at")
	private Date resolvedAt;

	@Column (name = "resolved_by", length = 100)
	private String resolvedBy;
  
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getBcrDataId() {
		return bcrDataId;
	}

	public void setBcrDataId(String bcrDataId) {
		this.bcrDataId = bcrDataId;
	}

	public String getInnerItemCode() {
		return innerItemCode;
	}

	public void setInnerItemCode(String innerItemCode) {
		this.innerItemCode = innerItemCode;
	}

	public String getItemCode() {
		return itemCode;
	}

	public void setItemCode(String itemCode) {
		this.itemCode = itemCode;
	}

	public String getBarcodedata() {
		return barcodedata;
	}

	public void setBarcodedata(String barcodedata) {
		this.barcodedata = barcodedata;
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

	public Integer getMasterLengthMm() {
		return masterLengthMm;
	}

	public void setMasterLengthMm(Integer masterLengthMm) {
		this.masterLengthMm = masterLengthMm;
	}

	public Integer getMasterWidthMm() {
		return masterWidthMm;
	}

	public void setMasterWidthMm(Integer masterWidthMm) {
		this.masterWidthMm = masterWidthMm;
	}

	public Integer getMasterHeightMm() {
		return masterHeightMm;
	}

	public void setMasterHeightMm(Integer masterHeightMm) {
		this.masterHeightMm = masterHeightMm;
	}

	public Integer getDiffLengthMm() {
		return diffLengthMm;
	}

	public void setDiffLengthMm(Integer diffLengthMm) {
		this.diffLengthMm = diffLengthMm;
	}

	public Integer getDiffWidthMm() {
		return diffWidthMm;
	}

	public void setDiffWidthMm(Integer diffWidthMm) {
		this.diffWidthMm = diffWidthMm;
	}

	public Integer getDiffHeightMm() {
		return diffHeightMm;
	}

	public void setDiffHeightMm(Integer diffHeightMm) {
		this.diffHeightMm = diffHeightMm;
	}

	public Date getRegDt() {
		return regDt;
	}

	public void setRegDt(Date regDt) {
		this.regDt = regDt;
	}

	public Date getDetectedAt() {
		return detectedAt;
	}

	public void setDetectedAt(Date detectedAt) {
		this.detectedAt = detectedAt;
	}

	public String getResolvedYn() {
		return resolvedYn;
	}

	public void setResolvedYn(String resolvedYn) {
		this.resolvedYn = resolvedYn;
	}

	public Date getResolvedAt() {
		return resolvedAt;
	}

	public void setResolvedAt(Date resolvedAt) {
		this.resolvedAt = resolvedAt;
	}

	public String getResolvedBy() {
		return resolvedBy;
	}

	public void setResolvedBy(String resolvedBy) {
		this.resolvedBy = resolvedBy;
	}	
}
