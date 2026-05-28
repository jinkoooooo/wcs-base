package operato.logis.asrs.entity;

import java.util.Date; 
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;

@Table(name = "tb_ac_stock_txn", idStrategy = GenerationRule.UUID)
public class TbAcStockTxn extends xyz.elidom.orm.entity.basic.ElidomStampHook {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 890574199124117164L;

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;

	@Column (name = "txn_no", nullable = false, length = 50)
	private String txnNo;

	@Column (name = "txn_type", nullable = false, length = 30)
	private String txnType;

	@Column (name = "stock_unit_id", nullable = false, length = 40)
	private String stockUnitId;

	@Column (name = "item_id", nullable = false, length = 40)
	private String itemId;

	@Column (name = "lot_id", length = 40)
	private String lotId;

	@Column (name = "from_location_id", length = 40)
	private String fromLocationId;

	@Column (name = "to_location_id", length = 40)
	private String toLocationId;

	@Column (name = "qty", nullable = false)
	private Integer qty;

	@Column (name = "ref_doc_type", length = 30)
	private String refDocType;

	@Column (name = "ref_doc_no", length = 50)
	private String refDocNo;

	@Column (name = "ref_line_no", length = 30)
	private String refLineNo;

	@Column (name = "reason_code", length = 30)
	private String reasonCode;

	@Column (name = "remark", length = 500)
	private String remark;

	@Column (name = "txn_at", nullable = false, type = xyz.elidom.dbist.annotation.ColumnType.DATETIME)
	private Date txnAt;
  
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTxnNo() {
		return txnNo;
	}

	public void setTxnNo(String txnNo) {
		this.txnNo = txnNo;
	}

	public String getTxnType() {
		return txnType;
	}

	public void setTxnType(String txnType) {
		this.txnType = txnType;
	}

	public String getStockUnitId() {
		return stockUnitId;
	}

	public void setStockUnitId(String stockUnitId) {
		this.stockUnitId = stockUnitId;
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

	public String getFromLocationId() {
		return fromLocationId;
	}

	public void setFromLocationId(String fromLocationId) {
		this.fromLocationId = fromLocationId;
	}

	public String getToLocationId() {
		return toLocationId;
	}

	public void setToLocationId(String toLocationId) {
		this.toLocationId = toLocationId;
	}

	public Integer getQty() {
		return qty;
	}

	public void setQty(Integer qty) {
		this.qty = qty;
	}

	public String getRefDocType() {
		return refDocType;
	}

	public void setRefDocType(String refDocType) {
		this.refDocType = refDocType;
	}

	public String getRefDocNo() {
		return refDocNo;
	}

	public void setRefDocNo(String refDocNo) {
		this.refDocNo = refDocNo;
	}

	public String getRefLineNo() {
		return refLineNo;
	}

	public void setRefLineNo(String refLineNo) {
		this.refLineNo = refLineNo;
	}

	public String getReasonCode() {
		return reasonCode;
	}

	public void setReasonCode(String reasonCode) {
		this.reasonCode = reasonCode;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public Date getTxnAt() {
		return txnAt;
	}

	public void setTxnAt(Date txnAt) {
		this.txnAt = txnAt;
	}	
}
