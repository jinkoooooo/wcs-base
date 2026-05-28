package operato.logis.changwon.entity.MFC;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;

@Setter
@Getter
@Table(name = "c_job_ret", idStrategy = GenerationRule.UUID)
public class JobRet extends xyz.elidom.orm.entity.basic.ElidomStampHook {

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;

	@Column (name = "order_id", nullable = false, length = 12)
	private String orderId;

	@Column (name = "job_no", nullable = false, length = 4)
	private Integer jobNo;

	@Column (name = "wms_ord_no", nullable = false, length = 30)
	private String wmsOrdNo;

	@Column (name = "update_datetime", nullable = false)
	private Date updateDatetime;

	@Column (name = "order_kind", nullable = false, length = 1)
	private Integer orderKind;

	@Column (name = "result_type", nullable = false, length = 2)
	private String resultType;

	@Column (name = "complete_type", length = 1)
	private Integer completeType;

	@Column (name = "error_machine", length = 6)
	private String errorMachine;

	@Column (name = "error_code", length = 6)
	private Integer errorCode;

	@Column (name = "error_z", length = 1)
	private Integer errorZ;

	@Column (name = "error_x", length = 2)
	private Integer errorX;

	@Column (name = "error_y", length = 2)
	private Integer errorY;

	@Column (name = "flag", nullable = false, length = 1)
	private Integer flag;
}