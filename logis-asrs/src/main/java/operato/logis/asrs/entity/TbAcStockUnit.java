package operato.logis.asrs.entity;

import java.util.Date; 
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;

@Table(name = "tb_ac_stock_unit", idStrategy = GenerationRule.UUID)
public class TbAcStockUnit extends xyz.elidom.orm.entity.basic.ElidomStampHook {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 964826312196201853L;

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;

	@Column (name = "stock_unit_no", nullable = false, length = 50)
	private String stockUnitNo;

	@Column (name = "parent_stock_unit_id", length = 40)
	private String parentStockUnitId;

	@Column (name = "item_id", nullable = false, length = 40)
	private String itemId;

	@Column (name = "lot_id", length = 40)
	private String lotId;

	@Column (name = "current_location_id", nullable = false, length = 40)
	private String currentLocationId;

	@Column (name = "stock_unit_type", nullable = false, length = 20)
	private String stockUnitType;

	@Column (name = "qty", nullable = false)
	private Integer qty;

	@Column (name = "reserved_qty", nullable = false)
	private Integer reservedQty;

	@Column (name = "stock_status_code", nullable = false, length = 20)
	private String stockStatusCode;

	@Column (name = "inbound_at", nullable = false, type = xyz.elidom.dbist.annotation.ColumnType.DATETIME)
	private Date inboundAt;

	@Column (name = "last_moved_at", nullable = false, type = xyz.elidom.dbist.annotation.ColumnType.DATETIME)
	private Date lastMovedAt;

	@Column (name = "hold_yn", nullable = false, length = 10)
	private String holdYn;

	@Column (name = "active_yn", nullable = false, length = 10)
	private String activeYn;
  
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getStockUnitNo() {
		return stockUnitNo;
	}

	public void setStockUnitNo(String stockUnitNo) {
		this.stockUnitNo = stockUnitNo;
	}

	public String getParentStockUnitId() {
		return parentStockUnitId;
	}

	public void setParentStockUnitId(String parentStockUnitId) {
		this.parentStockUnitId = parentStockUnitId;
	}

	public String getItemId() {
		return itemId;
	}

	public void setItemId(String itemId) {
		this.itemId = itemId;
	}

	public String getLotId() {
		return lotId;
	}

	public void setLotId(String lotId) {
		this.lotId = lotId;
	}

	public String getCurrentLocationId() {
		return currentLocationId;
	}

	public void setCurrentLocationId(String currentLocationId) {
		this.currentLocationId = currentLocationId;
	}

	public String getStockUnitType() {
		return stockUnitType;
	}

	public void setStockUnitType(String stockUnitType) {
		this.stockUnitType = stockUnitType;
	}

	public Integer getQty() {
		return qty;
	}

	public void setQty(Integer qty) {
		this.qty = qty;
	}

	public Integer getReservedQty() {
		return reservedQty;
	}

	public void setReservedQty(Integer reservedQty) {
		this.reservedQty = reservedQty;
	}

	public String getStockStatusCode() {
		return stockStatusCode;
	}

	public void setStockStatusCode(String stockStatusCode) {
		this.stockStatusCode = stockStatusCode;
	}

	public Date getInboundAt() {
		return inboundAt;
	}

	public void setInboundAt(Date inboundAt) {
		this.inboundAt = inboundAt;
	}

	public Date getLastMovedAt() {
		return lastMovedAt;
	}

	public void setLastMovedAt(Date lastMovedAt) {
		this.lastMovedAt = lastMovedAt;
	}

	public String getHoldYn() {
		return holdYn;
	}

	public void setHoldYn(String holdYn) {
		this.holdYn = holdYn;
	}

	public String getActiveYn() {
		return activeYn;
	}

	public void setActiveYn(String activeYn) {
		this.activeYn = activeYn;
	}	
}
