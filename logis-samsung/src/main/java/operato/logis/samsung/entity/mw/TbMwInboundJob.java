package operato.logis.samsung.entity.mw;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import operato.logis.samsung.config.jackson.NullableDateDeserializer;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;

import java.util.Date;

@Table(name = "tb_mw_inbound_job", idStrategy = GenerationRule.UUID)
public class TbMwInboundJob extends xyz.elidom.orm.entity.basic.ElidomStampHook {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 468460085507398958L;

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;

	@Column (name = "job_no", nullable = false, length = 100)
	private String jobNo;

	@Column (name = "bl_no", nullable = false, length = 20)
	private String blNo;

	@Column (name = "invoice", length = 50)
	private String invoice;

	@Column (name = "cntr_no", nullable = false, length = 50)
	private String cntrNo;

	@JsonDeserialize(using = NullableDateDeserializer.class)
	@Column (name = "inbound_date", nullable = false)
	private Date inboundDate;

	@Column (name = "job_status_desc", length = 50)
	private String jobStatusDesc;

	@Column (name = "job_status", nullable = false)
	private Integer jobStatus;

	@JsonDeserialize(using = NullableDateDeserializer.class)
	@Column (name = "job_start_dt")
	private Date jobStartDt;

	@JsonDeserialize(using = NullableDateDeserializer.class)
	@Column (name = "job_end_dt")
	private Date jobEndDt;

	@Column (name = "sku_qty")
	private Integer skuQty;

	@Column (name = "total_item_qty")
	private Integer totalItemQty;

	@Column (name = "completed_item_qty")
	private Integer completedItemQty;

	@Column (name = "ng_item_qty")
	private Integer ngItemQty;

	@Column (name = "attribute_1", length = 200)
	private String attribute1;

	@Column (name = "attribute_2", length = 200)
	private String attribute2;

	@Column (name = "attribute_3", length = 200)
	private String attribute3;

	@Column (name = "attribute_4", length = 200)
	private String attribute4;
  
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getJobNo() {
		return jobNo;
	}

	public void setJobNo(String jobNo) {
		this.jobNo = jobNo;
	}

	public String getBlNo() {
		return blNo;
	}

	public void setBlNo(String blNo) {
		this.blNo = blNo;
	}

	public String getInvoice() {
		return invoice;
	}

	public void setInvoice(String invoice) {
		this.invoice = invoice;
	}

	public String getCntrNo() {
		return cntrNo;
	}

	public void setCntrNo(String cntrNo) {
		this.cntrNo = cntrNo;
	}

	public Date getInboundDate() {
		return inboundDate;
	}

	public void setInboundDate(Date inboundDate) {
		this.inboundDate = inboundDate;
	}

	public String getJobStatusDesc() {
		return jobStatusDesc;
	}

	public void setJobStatusDesc(String jobStatusDesc) {
		this.jobStatusDesc = jobStatusDesc;
	}

	public Integer getJobStatus() {
		return jobStatus;
	}

	public void setJobStatus(Integer jobStatus) {
		this.jobStatus = jobStatus;
	}

	public Date getJobStartDt() {
		return jobStartDt;
	}

	public void setJobStartDt(Date jobStartDt) {
		this.jobStartDt = jobStartDt;
	}

	public Date getJobEndDt() {
		return jobEndDt;
	}

	public void setJobEndDt(Date jobEndDt) {
		this.jobEndDt = jobEndDt;
	}

	public Integer getSkuQty() {
		return skuQty;
	}

	public void setSkuQty(Integer skuQty) {
		this.skuQty = skuQty;
	}

	public Integer getTotalItemQty() {
		return totalItemQty;
	}

	public void setTotalItemQty(Integer totalItemQty) {
		this.totalItemQty = totalItemQty;
	}

	public Integer getcompletedItemQty() {
		return completedItemQty;
	}

	public void setcompletedItemQty(Integer completedItemQty) {
		this.completedItemQty = completedItemQty;
	}

	public Integer getNgItemQty() {
		return ngItemQty;
	}

	public void setNgItemQty(Integer ngItemQty) {
		this.ngItemQty = ngItemQty;
	}

	public String getAttribute1() {
		return attribute1;
	}

	public void setAttribute1(String attribute1) {
		this.attribute1 = attribute1;
	}

	public String getAttribute2() {
		return attribute2;
	}

	public void setAttribute2(String attribute2) {
		this.attribute2 = attribute2;
	}

	public String getAttribute3() {
		return attribute3;
	}

	public void setAttribute3(String attribute3) {
		this.attribute3 = attribute3;
	}

	public String getAttribute4() {
		return attribute4;
	}

	public void setAttribute4(String attribute4) {
		this.attribute4 = attribute4;
	}	
}
