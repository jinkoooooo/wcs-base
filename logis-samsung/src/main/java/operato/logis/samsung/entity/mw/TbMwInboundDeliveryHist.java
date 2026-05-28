package operato.logis.samsung.entity.mw;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

import java.util.Date;

@Getter
@Setter
@Table(name = "tb_mw_inbound_delivery_hist", idStrategy = GenerationRule.UUID)
public class TbMwInboundDeliveryHist extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "lc_id", nullable = false, length = 40)
    private String lcId; // 센터 ID

    @Column(name = "lc_nm", nullable = false, length = 40)
    private String lcNm; // 센터 명

    @Column(name = "inbound_seq", nullable = false, length = 100)
    private String inboundSeq;

    @Column(name = "cntr_no", nullable = false, length = 20)
    private String cntrNo; // 컨테이너 번호

    @Column(name = "dock_id", length = 10)
    private String dockId;

    @Column(name = "cust_id", length = 100)
    private String custId;

    @Column(name = "cust_nm", length = 200)
    private String custNm;

    @Column(name = "bl_no", nullable = false, length = 50)
    private String blNo; // B/L NO.

    @Column(name = "invoice", nullable = false, length = 50)
    private String invoice; // Invoice

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Column(name = "inbound_date", nullable = false, length = 20)
    private Date inboundDate; // 입고 예정일

    @Column(name = "remark")
    private String remark;

    @Column(name = "lot_no", length = 100)
    private String lotNo;

    @Column(name = "box_barcode", length = 100)
    private String boxBarcode;

    @Column(name = "real_order_no", length = 100)
    private String realOrderNo;

    @Column(name = "item_order_no", length = 100)
    private String itemOrderNo;

    @Column(name = "item_code", nullable = false, length = 100)
    private String itemCode; // Material

    @Column(name = "owner_item_code", length = 100)
    private String ownerItemCode;

    @Column(name = "inner_item_code", length = 100)
    private String innerItemCode;

    @Column(name = "item_name", length = 200)
    private String itemName;

    @Column(name = "item_type", nullable = false, length = 20)
    private String itemType; // 제품 (SBS, BMF, ...)

    @Column(name = "item_desc", nullable = false, length = 100)
    private String itemDesc; // 제품 (냉장고, 식기세척기, ...)

    @Column(name = "item_qty", nullable = false)
    private Integer itemQty; // 수량

    @Column(name = "item_cbm", nullable = false)
    private Double itemCbm; // 용적

    @Column(name = "total_cbm", nullable = false)
    private Double totalCbm; // 용적 합

    @Column(name = "item_priority", nullable = false, length = 40)
    private String itemPriority; // 우선순위

    @Column(name = "reg_id", length = 32)
    private String regId;

    @Column(name = "reg_time", length = 32)
    private String regTime;

    @Column(name = "inbound_status", nullable = false)
    private Integer inboundStatus;

    @Column(name = "start_datetime")
    private Date startDatetime;

    @Column(name = "complete_datetime")
    private Date completeDatetime;

    @Column(name = "manual_flag", nullable = false)
    private boolean manualFlag;

    @Column(name = "pass_qty", nullable = false)
    private Integer passQty; // 수량

    @Column(name = "ng_qty", nullable = false)
    private Integer ngQty; // 수량
}