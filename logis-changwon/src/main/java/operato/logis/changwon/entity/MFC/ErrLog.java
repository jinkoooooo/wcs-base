package operato.logis.changwon.entity.MFC;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;

@Getter
@Setter
@Table(name = "c_err_log", idStrategy = GenerationRule.UUID)
public class ErrLog extends xyz.elidom.orm.entity.basic.ElidomStampHook {

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;

	@Column (name = "order_id", length = 12)
	private String orderId;

	@Column (name = "job_no", length = 4)
	private Integer jobNo;

	@Column (name = "error_datetime", nullable = false)
	private Date errorDatetime;

	@Column (name = "error_machine", length = 6)
	private String errorMachine;

	@Column (name = "error_code", length = 6)
	private Integer errorCode;

	@Column (name = "error_tier", length = 1)
	private Integer errorTier;

	@Column (name = "error_bay", length = 2)
	private Integer errorBay;

	@Column (name = "error_bank", length = 2)
	private Integer errorBank;

	@Column (name = "reset_datetime")
	private Date resetDatetime;

	@Column (name = "rack_no", length = 10)
	private String rackNo;

	@Column (name = "flag", nullable = false, length = 1)
	private Integer flag;

	@Column (name = "is_checked")
	private Boolean isChecked;
}