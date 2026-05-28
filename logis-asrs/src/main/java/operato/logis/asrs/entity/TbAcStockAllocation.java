package operato.logis.asrs.entity;

import java.util.Date; 
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;

@Table(name = "tb_ac_stock_allocation", idStrategy = GenerationRule.UUID)
public class TbAcStockAllocation extends xyz.elidom.orm.entity.basic.ElidomStampHook {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 742597770158869210L;

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;

	@Column (name = "stock_unit_id", nullable = false, length = 40)
	private String stockUnitId;

	@Column (name = "item_id", nullable = false, length = 40)
	private String itemId;

	@Column (name = "allocated_qty", nullable = false)
	private Integer allocatedQty;

	@Column (name = "alloc_status_code", nullable = false, length = 20)
	private String allocStatusCode;

	@Column (name = "ref_doc_type", nullable = false, length = 30)
	private String refDocType;

	@Column (name = "ref_doc_no", nullable = false, length = 50)
	private String refDocNo;

	@Column (name = "ref_line_no", length = 30)
	private String refLineNo;

	@Column (name = "due_date")
	private Date dueDate;

	@Column (name = "allocated_at", nullable = false, type = xyz.elidom.dbist.annotation.ColumnType.DATETIME)
	private Date allocatedAt;
  
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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

	public Integer getAllocatedQty() {
		return allocatedQty;
	}

	public void setAllocatedQty(Integer allocatedQty) {
		this.allocatedQty = allocatedQty;
	}

	public String getAllocStatusCode() {
		return allocStatusCode;
	}

	public void setAllocStatusCode(String allocStatusCode) {
		this.allocStatusCode = allocStatusCode;
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

	public Date getDueDate() {
		return dueDate;
	}

	public void setDueDate(Date dueDate) {
		this.dueDate = dueDate;
	}

	public Date getAllocatedAt() {
		return allocatedAt;
	}

	public void setAllocatedAt(Date allocatedAt) {
		this.allocatedAt = allocatedAt;
	}	
}
