package operato.logis.asrs.entity;

import java.util.Date; 
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;

@Table(name = "tb_ac_item_activity_daily", idStrategy = GenerationRule.UUID)
public class TbAcItemActivityDaily extends xyz.elidom.orm.entity.basic.ElidomStampHook {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 231022841318179637L;

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;

	@Column (name = "center_id", nullable = false, length = 40)
	private String centerId;

	@Column (name = "area_id", nullable = false, length = 40)
	private String areaId;

	@Column (name = "item_id", nullable = false, length = 40)
	private String itemId;

	@Column (name = "activity_date", nullable = false)
	private Date activityDate;

	@Column (name = "inbound_count", nullable = false)
	private Integer inboundCount;

	@Column (name = "outbound_count", nullable = false)
	private Integer outboundCount;

	@Column (name = "outbound_qty", nullable = false)
	private Integer outboundQty;

	@Column (name = "partial_out_count", nullable = false)
	private Integer partialOutCount;

	@Column (name = "return_in_count", nullable = false)
	private Integer returnInCount;

	@Column (name = "move_count", nullable = false)
	private Integer moveCount;

	@Column (name = "avg_dwell_days", nullable = false)
	private Integer avgDwellDays;

	@Column (name = "demand_tomorrow_qty", nullable = false)
	private Integer demandTomorrowQty;

	@Column (name = "score_raw", nullable = false)
	private Integer scoreRaw;
  
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCenterId() {
		return centerId;
	}

	public void setCenterId(String centerId) {
		this.centerId = centerId;
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

	public Date getActivityDate() {
		return activityDate;
	}

	public void setActivityDate(Date activityDate) {
		this.activityDate = activityDate;
	}

	public Integer getInboundCount() {
		return inboundCount;
	}

	public void setInboundCount(Integer inboundCount) {
		this.inboundCount = inboundCount;
	}

	public Integer getOutboundCount() {
		return outboundCount;
	}

	public void setOutboundCount(Integer outboundCount) {
		this.outboundCount = outboundCount;
	}

	public Integer getOutboundQty() {
		return outboundQty;
	}

	public void setOutboundQty(Integer outboundQty) {
		this.outboundQty = outboundQty;
	}

	public Integer getPartialOutCount() {
		return partialOutCount;
	}

	public void setPartialOutCount(Integer partialOutCount) {
		this.partialOutCount = partialOutCount;
	}

	public Integer getReturnInCount() {
		return returnInCount;
	}

	public void setReturnInCount(Integer returnInCount) {
		this.returnInCount = returnInCount;
	}

	public Integer getMoveCount() {
		return moveCount;
	}

	public void setMoveCount(Integer moveCount) {
		this.moveCount = moveCount;
	}

	public Integer getAvgDwellDays() {
		return avgDwellDays;
	}

	public void setAvgDwellDays(Integer avgDwellDays) {
		this.avgDwellDays = avgDwellDays;
	}

	public Integer getDemandTomorrowQty() {
		return demandTomorrowQty;
	}

	public void setDemandTomorrowQty(Integer demandTomorrowQty) {
		this.demandTomorrowQty = demandTomorrowQty;
	}

	public Integer getScoreRaw() {
		return scoreRaw;
	}

	public void setScoreRaw(Integer scoreRaw) {
		this.scoreRaw = scoreRaw;
	}	
}
