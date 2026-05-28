package operato.logis.samsung.entity.mw;

import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;

import java.util.Date;

@Table(name = "tb_mw_box_track", idStrategy = GenerationRule.UUID)
public class TbMwBoxTrack extends xyz.elidom.orm.entity.basic.ElidomStampHook {
	/**
	 * SerialVersion UID
	 */
	private static final long serialVersionUID = 515761446051649217L;

	@PrimaryKey
	@Column (name = "id", nullable = false, length = 40)
	private String id;

	@Column (name = "box_id", nullable = false, length = 100)
	private String boxId;

	@Column (name = "plc_seq_no", length = 100)
	private String plcSeqNo;

	@Column (name = "parcel_id", length = 100)
	private String parcelId;

	@Column (name = "tracking_type", length = 40)
	private String trackingType;

	@Column (name = "tracking_status")
	private Integer trackingStatus;

	@Column (name = "tracking_desc", length = 100)
	private String trackingDesc;

	@Column (name = "tracking_at")
	private Date trackingAt;

	@Column (name = "line_id", length = 40)
	private String lineId;

	@Column (name = "equip_id", length = 40)
	private String equipId;

	@Column (name = "command_type", length = 40)
	private String commandType;

	@Column (name = "command_id", length = 100)
	private String commandId;

	@Column (name = "command_data", length = 100)
	private String commandData;

	@Column (name = "command_desc", length = 200)
	private String commandDesc;

	@Column (name = "result_type", length = 40)
	private String resultType;

	@Column (name = "result_data", length = 100)
	private String resultData;

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

	public String getBoxId() {
		return boxId;
	}

	public void setBoxId(String boxId) {
		this.boxId = boxId;
	}

	public String getPlcSeqNo() {
		return plcSeqNo;
	}

	public void setPlcSeqNo(String plcSeqNo) {
		this.plcSeqNo = plcSeqNo;
	}

	public String getParcelId() {
		return parcelId;
	}

	public void setParcelId(String parcelId) {
		this.parcelId = parcelId;
	}

	public String getTrackingType() {
		return trackingType;
	}

	public void setTrackingType(String trackingType) {
		this.trackingType = trackingType;
	}

	public Integer getTrackingStatus() {
		return trackingStatus;
	}

	public void setTrackingStatus(Integer trackingStatus) {
		this.trackingStatus = trackingStatus;
	}

	public String getTrackingDesc() {
		return trackingDesc;
	}

	public void setTrackingDesc(String trackingDesc) {
		this.trackingDesc = trackingDesc;
	}

	public Date getTrackingAt() {
		return trackingAt;
	}

	public void setTrackingAt(Date trackingAt) {
		this.trackingAt = trackingAt;
	}

	public String getLineId() {
		return lineId;
	}

	public void setLineId(String lineId) {
		this.lineId = lineId;
	}

	public String getEquipId() {
		return equipId;
	}

	public void setEquipId(String equipId) {
		this.equipId = equipId;
	}

	public String getCommandType() {
		return commandType;
	}

	public void setCommandType(String commandType) {
		this.commandType = commandType;
	}

	public String getCommandId() {
		return commandId;
	}

	public void setCommandId(String commandId) {
		this.commandId = commandId;
	}

	public String getCommandData() {
		return commandData;
	}

	public void setCommandData(String commandData) {
		this.commandData = commandData;
	}

	public String getCommandDesc() {
		return commandDesc;
	}

	public void setCommandDesc(String commandDesc) {
		this.commandDesc = commandDesc;
	}

	public String getResultType() {
		return resultType;
	}

	public void setResultType(String resultType) {
		this.resultType = resultType;
	}

	public String getResultData() {
		return resultData;
	}

	public void setResultData(String resultData) {
		this.resultData = resultData;
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
