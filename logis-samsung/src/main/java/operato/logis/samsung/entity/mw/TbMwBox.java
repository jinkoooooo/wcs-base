package operato.logis.samsung.entity.mw;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;

import java.util.Date;

@Getter
@Setter
@Table(name = "tb_mw_box", idStrategy = GenerationRule.UUID)
public class TbMwBox extends xyz.elidom.orm.entity.basic.ElidomStampHook {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 712759917998649163L;

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;

	@Column (name = "box_id", length = 100)
	private String boxId;

	@Column (name = "parcel_id", nullable = false, length = 100)
	private String parcelId;

	@Column (name = "item_code", nullable = false, length = 100)
	private String itemCode;

	@Column (name = "item_name", length = 100)
	private String itemName;

	@Column (name = "received_at")
	private Date receivedAt;

	@Column (name = "first_line_id", length = 40)
	private String firstLineId;

	@Column (name = "first_equip_id", length = 40)
	private String firstEquipId;

	@Column (name = "cognex_result")
	private Integer cognexResult;

	@Column (name = "cognex_result_rmk", length = 200)
	private String cognexResultRmk;

	@Column (name = "cognex_result_at")
	private Date cognexResultAt;

	@Column (name = "sds_ai_result")
	private Integer sdsAiResult;

	@Column (name = "sds_ai_result_rmk", length = 200)
	private String sdsAiResultRmk;

	@Column (name = "sds_ai_result_at")
	private Date sdsAiResultAt;

	@Column (name = "manual_result")
	private Integer manualResult;

	@Column (name = "manual_result_rmk", length = 200)
	private String manualResultRmk;

	@Column (name = "manual_result_at")
	private Date manualResultAt;

	@Column (name = "file_name_top", length = 200)
	private String fileNameTop;

	@Column (name = "file_name_front", length = 200)
	private String fileNameFront;

	@Column (name = "file_name_back", length = 200)
	private String fileNameBack;

	@Column (name = "file_name_left", length = 200)
	private String fileNameLeft;

	@Column (name = "file_name_right", length = 200)
	private String fileNameRight;

	@Column (name = "file_name_bottom_left", length = 200)
	private String fileNameBottomLeft;

	@Column (name = "file_name_bottom_right", length = 200)
	private String fileNameBottomRight;

	@Column (name = "tracking_status")
	private Integer trackingStatus;

	@Column (name = "tracking_desc", length = 100)
	private String trackingDesc;

	@Column (name = "tracking_at")
	private Date trackingAt;

	@Column (name = "box_length")
	private Integer boxLength;

	@Column (name = "box_height")
	private Integer boxHeight;

	@Column (name = "box_width")
	private Integer boxWidth;

	@Column (name = "box_angle")
	private Integer boxAngle;

	@Column (name = "final_status")
	private Integer finalStatus;

	@Column (name = "end_point_cd", length = 20)
	private String endPointCd;

	@Column (name = "final_remark", length = 200)
	private String finalRemark;

	@Column (name = "final_at", length = 2)
	private Date finalAt;

	@Column (name = "reject_desc")
	private String rejectDesc;

	@Column (name = "reject_type", length = 20)
	private String rejectType;

	@Column (name = "attribute_1", length = 200)
	private String attribute1;

	@Column (name = "attribute_2", length = 200)
	private String attribute2;

	@Column (name = "attribute_3", length = 200)
	private String attribute3;

	// 해당 박스 메뉴얼 여부 "true"/"false"
	@Column (name = "attribute_4", length = 200)
	private String attribute4;

	@Column (name = "plc_seq_no", length = 100)
	private String plcSeqNo;

	@Column (name = "bl_no", length = 50)
	private String blNo;

	@Column (name = "invoice", length = 50)
	private String invoice;

	@Column (name = "cntr_no", length = 50)
	private String cntrNo;
}
