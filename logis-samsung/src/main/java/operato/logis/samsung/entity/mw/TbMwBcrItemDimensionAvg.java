package operato.logis.samsung.entity.mw;

import java.util.Date; 
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;

@Table(name = "tb_mw_bcr_item_dimension_avg", idStrategy = GenerationRule.UUID)
public class TbMwBcrItemDimensionAvg extends xyz.elidom.orm.entity.basic.ElidomStampHook {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 420418427932849451L;

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;

	@Column (name = "inner_item_code", nullable = false, length = 100)
	private String innerItemCode;

	@Column (name = "item_code", nullable = false, length = 100)
	private String itemCode;

	@Column (name = "avg_length_mm", nullable = false)
	private Integer avgLengthMm;

	@Column (name = "avg_width_mm", nullable = false)
	private Integer avgWidthMm;

	@Column (name = "avg_height_mm", nullable = false)
	private Integer avgHeightMm;

	@Column (name = "sample_cnt")
	private Integer sampleCnt;

	@Column (name = "last_scan_dt")
	private Date lastScanDt;

	@Column (name = "last_calc_dt")
	private Date lastCalcDt;
  
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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

	public Integer getAvgLengthMm() {
		return avgLengthMm;
	}

	public void setAvgLengthMm(Integer avgLengthMm) {
		this.avgLengthMm = avgLengthMm;
	}

	public Integer getAvgWidthMm() {
		return avgWidthMm;
	}

	public void setAvgWidthMm(Integer avgWidthMm) {
		this.avgWidthMm = avgWidthMm;
	}

	public Integer getAvgHeightMm() {
		return avgHeightMm;
	}

	public void setAvgHeightMm(Integer avgHeightMm) {
		this.avgHeightMm = avgHeightMm;
	}

	public Integer getSampleCnt() {
		return sampleCnt;
	}

	public void setSampleCnt(Integer sampleCnt) {
		this.sampleCnt = sampleCnt;
	}

	public Date getLastScanDt() {
		return lastScanDt;
	}

	public void setLastScanDt(Date lastScanDt) {
		this.lastScanDt = lastScanDt;
	}

	public Date getLastCalcDt() {
		return lastCalcDt;
	}

	public void setLastCalcDt(Date lastCalcDt) {
		this.lastCalcDt = lastCalcDt;
	}	
}
