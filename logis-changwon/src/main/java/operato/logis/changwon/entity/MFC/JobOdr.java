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
@Table(name = "c_job_odr", idStrategy = GenerationRule.UUID)
public class JobOdr extends xyz.elidom.orm.entity.basic.ElidomStampHook {

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;

	@Column (name = "order_id", nullable = false, length = 12)
	private String orderId;

	@Column (name = "wms_ord_no", nullable = false, length = 30)
	private String wmsOrdNo;

	@Column (name = "job_no", nullable = false, length = 4)
	private Integer jobNo;

	@Column (name = "am_kbn", nullable = false, length = 1)
	private String amKbn;

	@Column (name = "order_status", nullable = false, length = 2)
	private Integer orderStatus;

	@Column (name = "order_type", nullable = false, length = 3)
	private Integer orderType;

	@Column (name = "order_kind", nullable = false, length = 1)
	private Integer orderKind;

	@Column (name = "order_phase", nullable = false, length = 1)
	private Integer orderPhase;

	@Column (name = "order_trans_status", nullable = false, length = 2)
	private Integer orderTransStatus;

	@Column (name = "mfc_result", nullable = false, length = 2)
	private Integer mfcResult;

	@Column (name = "sn", nullable = false, length = 3)
	private Integer sn;

	@Column (name = "sz", length = 1)
	private Integer sz;

	@Column (name = "sx", length = 2)
	private Integer sx;

	@Column (name = "sy", length = 2)
	private Integer sy;

	@Column (name = "en", length = 3)
	private Integer en;

	@Column (name = "ez", length = 1)
	private Integer ez;

	@Column (name = "ex", length = 2)
	private Integer ex;

	@Column (name = "ey", length = 2)
	private Integer ey;

	@Column (name = "current_machine", length = 2)
	private Integer currentMachine;

	@Column (name = "current_position", length = 3)
	private Integer currentPosition;

	@Column (name = "error_count", length = 3)
	private Integer errorCount;

	@Column (name = "order_priority", nullable = false, length = 4)
	private Integer orderPriority;

	@Column (name = "order_receive_datetime", nullable = false)
	private Date orderReceiveDatetime;

	@Column (name = "order_mfc_datetime")
	private Date orderMfcDatetime;

	@Column (name = "update_datetime")
	private Date updateDatetime;

	@Column (name = "storage_id", length = 4)
	private String storageId;

	@Column (name = "source_id", length = 10)
	private String sourceId;

	@Column (name = "rack_no", length = 10)
	private String rackNo;

	@Column (name = "lugg_info", length = 1)
	private Integer luggInfo;

	@Column (name = "pallet_id", length = 20)
	private String palletId;

	@Column (name = "userdata")
	private String userdata;

	@Column(name = "data_transmit_status", nullable = false, length = 4)
	private Integer dataTransmitStatus;
}