package operato.logis.lms.entity.pm;

import java.util.Date; 
import xyz.elidom.dbist.annotation.Index;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;

@Table(name = "tb_pm_project_main", idStrategy = GenerationRule.UUID)
public class TbPmProjectMain extends xyz.elidom.orm.entity.basic.ElidomStampHook {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 157832677484364708L;

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;

	@Column (name = "project_name", nullable = false, length = 200)
	private String projectName;

	@Column (name = "owner_team_name", length = 200)
	private String ownerTeamName;

	@Column (name = "owner_part_name", length = 200)
	private String ownerPartName;

	@Column (name = "project_status_cd", nullable = false)
	private Integer projectStatusCd;

	@Column (name = "supply_site", length = 200)
	private String supplySite;

	@Column (name = "contract_dt")
	private Date contractDt;

	@Column (name = "project_end_dt")
	private Date projectEndDt;

	@Column (name = "start_year")
	private Integer startYear;

	@Column (name = "end_year")
	private Integer endYear;

	@Column (name = "final_customer_name", length = 200)
	private String finalCustomerName;

	@Column (name = "final_customer_contact", length = 200)
	private String finalCustomerContact;

	@Column (name = "mid_customer_name", length = 200)
	private String midCustomerName;

	@Column (name = "mid_customer_contact", length = 200)
	private String midCustomerContact;

	@Column (name = "contract_company_name", length = 200)
	private String contractCompanyName;

	@Column (name = "pl_name", length = 200)
	private String plName;

	@Column (name = "sub_pl_name", length = 200)
	private String subPlName;

	@Column (name = "sales_total_amt")
	private Integer salesTotalAmt;

	@Column (name = "sales_contract_amt")
	private Integer salesContractAmt;

	@Column (name = "equip_purchase_amt")
	private Integer equipPurchaseAmt;

	@Column (name = "outsource_purchase_amt")
	private Integer outsourcePurchaseAmt;

	@Column (name = "remark", length = 1000)
	private String remark;

	@Column (name = "use_yn", nullable = false, length = 10)
	private String useYn;
  
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getOwnerTeamName() {
		return ownerTeamName;
	}

	public void setOwnerTeamName(String ownerTeamName) {
		this.ownerTeamName = ownerTeamName;
	}

	public String getOwnerPartName() {
		return ownerPartName;
	}

	public void setOwnerPartName(String ownerPartName) {
		this.ownerPartName = ownerPartName;
	}

	public Integer getProjectStatusCd() {
		return projectStatusCd;
	}

	public void setProjectStatusCd(Integer projectStatusCd) {
		this.projectStatusCd = projectStatusCd;
	}

	public String getSupplySite() {
		return supplySite;
	}

	public void setSupplySite(String supplySite) {
		this.supplySite = supplySite;
	}

	public Date getContractDt() {
		return contractDt;
	}

	public void setContractDt(Date contractDt) {
		this.contractDt = contractDt;
	}

	public Date getProjectEndDt() {
		return projectEndDt;
	}

	public void setProjectEndDt(Date projectEndDt) {
		this.projectEndDt = projectEndDt;
	}

	public Integer getStartYear() {
		return startYear;
	}

	public void setStartYear(Integer startYear) {
		this.startYear = startYear;
	}

	public Integer getEndYear() {
		return endYear;
	}

	public void setEndYear(Integer endYear) {
		this.endYear = endYear;
	}

	public String getFinalCustomerName() {
		return finalCustomerName;
	}

	public void setFinalCustomerName(String finalCustomerName) {
		this.finalCustomerName = finalCustomerName;
	}

	public String getFinalCustomerContact() {
		return finalCustomerContact;
	}

	public void setFinalCustomerContact(String finalCustomerContact) {
		this.finalCustomerContact = finalCustomerContact;
	}

	public String getMidCustomerName() {
		return midCustomerName;
	}

	public void setMidCustomerName(String midCustomerName) {
		this.midCustomerName = midCustomerName;
	}

	public String getMidCustomerContact() {
		return midCustomerContact;
	}

	public void setMidCustomerContact(String midCustomerContact) {
		this.midCustomerContact = midCustomerContact;
	}

	public String getContractCompanyName() {
		return contractCompanyName;
	}

	public void setContractCompanyName(String contractCompanyName) {
		this.contractCompanyName = contractCompanyName;
	}

	public String getPlName() {
		return plName;
	}

	public void setPlName(String plName) {
		this.plName = plName;
	}

	public String getSubPlName() {
		return subPlName;
	}

	public void setSubPlName(String subPlName) {
		this.subPlName = subPlName;
	}

	public Integer getSalesTotalAmt() {
		return salesTotalAmt;
	}

	public void setSalesTotalAmt(Integer salesTotalAmt) {
		this.salesTotalAmt = salesTotalAmt;
	}

	public Integer getSalesContractAmt() {
		return salesContractAmt;
	}

	public void setSalesContractAmt(Integer salesContractAmt) {
		this.salesContractAmt = salesContractAmt;
	}

	public Integer getEquipPurchaseAmt() {
		return equipPurchaseAmt;
	}

	public void setEquipPurchaseAmt(Integer equipPurchaseAmt) {
		this.equipPurchaseAmt = equipPurchaseAmt;
	}

	public Integer getOutsourcePurchaseAmt() {
		return outsourcePurchaseAmt;
	}

	public void setOutsourcePurchaseAmt(Integer outsourcePurchaseAmt) {
		this.outsourcePurchaseAmt = outsourcePurchaseAmt;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public String getUseYn() {
		return useYn;
	}

	public void setUseYn(String useYn) {
		this.useYn = useYn;
	}	
}
