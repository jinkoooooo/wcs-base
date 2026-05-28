package operato.logis.asrs.entity;

import java.util.Date; 
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;

@Table(name = "tb_ac_demand_plan", idStrategy = GenerationRule.UUID)
public class TbAcDemandPlan extends xyz.elidom.orm.entity.basic.ElidomStampHook {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 815383129315865622L;

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;

	@Column (name = "area_id", nullable = false, length = 40)
	private String areaId;

	@Column (name = "item_id", nullable = false, length = 40)
	private String itemId;

	@Column (name = "demand_date", nullable = false)
	private Date demandDate;

	@Column (name = "demand_qty", nullable = false)
	private Integer demandQty;

	@Column (name = "demand_type", nullable = false, length = 20)
	private String demandType;

	@Column (name = "priority", nullable = false)
	private Integer priority;

	@Column (name = "source_system", length = 30)
	private String sourceSystem;

	@Column (name = "source_doc_no", length = 50)
	private String sourceDocNo;

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

	public String getItemId() {
		return itemId;
	}

	public void setItemId(String itemId) {
		this.itemId = itemId;
	}

	public Date getDemandDate() {
		return demandDate;
	}

	public void setDemandDate(Date demandDate) {
		this.demandDate = demandDate;
	}

	public Integer getDemandQty() {
		return demandQty;
	}

	public void setDemandQty(Integer demandQty) {
		this.demandQty = demandQty;
	}

	public String getDemandType() {
		return demandType;
	}

	public void setDemandType(String demandType) {
		this.demandType = demandType;
	}

	public Integer getPriority() {
		return priority;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	public String getSourceSystem() {
		return sourceSystem;
	}

	public void setSourceSystem(String sourceSystem) {
		this.sourceSystem = sourceSystem;
	}

	public String getSourceDocNo() {
		return sourceDocNo;
	}

	public void setSourceDocNo(String sourceDocNo) {
		this.sourceDocNo = sourceDocNo;
	}

	public String getActiveYn() {
		return activeYn;
	}

	public void setActiveYn(String activeYn) {
		this.activeYn = activeYn;
	}	
}
