package operato.logis.asrs.entity;

import java.util.Date; 
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;

@Table(name = "tb_ac_lot", idStrategy = GenerationRule.UUID)
public class TbAcLot extends xyz.elidom.orm.entity.basic.ElidomStampHook {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 710058930692228499L;

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;

	@Column (name = "item_id", nullable = false, length = 40)
	private String itemId;

	@Column (name = "lot_no", nullable = false, length = 50)
	private String lotNo;

	@Column (name = "vendor_lot_no", length = 50)
	private String vendorLotNo;

	@Column (name = "mfg_date")
	private Date mfgDate;

	@Column (name = "expiry_date")
	private Date expiryDate;

	@Column (name = "qa_status_code", nullable = false, length = 20)
	private String qaStatusCode;

	@Column (name = "active_yn", nullable = false, length = 10)
	private String activeYn;
  
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getItemId() {
		return itemId;
	}

	public void setItemId(String itemId) {
		this.itemId = itemId;
	}

	public String getLotNo() {
		return lotNo;
	}

	public void setLotNo(String lotNo) {
		this.lotNo = lotNo;
	}

	public String getVendorLotNo() {
		return vendorLotNo;
	}

	public void setVendorLotNo(String vendorLotNo) {
		this.vendorLotNo = vendorLotNo;
	}

	public Date getMfgDate() {
		return mfgDate;
	}

	public void setMfgDate(Date mfgDate) {
		this.mfgDate = mfgDate;
	}

	public Date getExpiryDate() {
		return expiryDate;
	}

	public void setExpiryDate(Date expiryDate) {
		this.expiryDate = expiryDate;
	}

	public String getQaStatusCode() {
		return qaStatusCode;
	}

	public void setQaStatusCode(String qaStatusCode) {
		this.qaStatusCode = qaStatusCode;
	}

	public String getActiveYn() {
		return activeYn;
	}

	public void setActiveYn(String activeYn) {
		this.activeYn = activeYn;
	}	
}
