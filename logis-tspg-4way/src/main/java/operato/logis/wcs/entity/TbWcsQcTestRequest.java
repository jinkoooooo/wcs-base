package operato.logis.wcs.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.*;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

import java.util.Date;

/**
 * QC 시험 의뢰 — (입고일자, SKU, LOT) 단위 1건. IF02(WES→LIMS) 발신 이력 스냅샷.
 */
@Getter
@Setter
@Table(name = "tb_wcs_qc_test_request", idStrategy = GenerationRule.UUID,
        uniqueFields = "inboundDate,itemCode,lotNo",
        indexes = {
                @Index(name = "ux_tb_wcs_qc_test_request_date_sku_lot",
                        columnList = "inbound_date,item_code,lot_no", unique = true),
                @Index(name = "ix_tb_wcs_qc_test_request_status", columnList = "status"),
                @Index(name = "ix_tb_wcs_qc_test_request_fetched", columnList = "fetched"),
                @Index(name = "ix_tb_wcs_qc_test_request_no", columnList = "test_request_no")
        })
public class TbWcsQcTestRequest extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    /** 입고일자. */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    @Column(name = "inbound_date", nullable = false, type = ColumnType.DATETIME)
    private Date inboundDate;

    /** IF02 test_request_no — 의뢰번호 (WES↔LIMS Key). */
    @Column(name = "test_request_no", nullable = false, length = 50)
    private String testRequestNo;

    /** IF02 product_code — 자재코드. */
    @Column(name = "item_code", nullable = false, length = 100)
    private String itemCode;

    /** IF02 tf_wf_type — 의뢰구분 (WF01, WF02, ...). */
    @Column(name = "test_wf_type", length = 30)
    private String testWfType;

    /** IF02 tf_req_desc — 의뢰내용 (R, M, DP, IPC, ...). */
    @Column(name = "test_req_desc", length = 200)
    private String testReqDesc;

    /** IF02 tf_mfr — 제조처. 마스터 스냅샷. */
    @Column(name = "manufacturer", length = 30)
    private String manufacturer;

    /** IF02 tf_lot_id — 제조번호(로트). 미부여 입고는 빈 문자열. */
    @Column(name = "lot_no", nullable = false, length = 100)
    private String lotNo;

    /** IF02 tf_mfd_on — 제조일자. */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    @Column(name = "manufactured_date", type = ColumnType.DATETIME)
    private Date manufacturedDate;

    /** IF02 tf_mfr_qty — 제조수량. */
    @Column(name = "manufactured_qty")
    private Integer manufacturedQty;

    /** IF02 tf_mfr_unit — 입고단위. 마스터 item_unit 스냅샷. */
    @Column(name = "mfr_unit", length = 40)
    private String mfrUnit;

    /** IF02 tf_exp_on — 사용기한. */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    @Column(name = "expiry_date", type = ColumnType.DATETIME)
    private Date expiryDate;

    /** IF02 tf_incom_cntr_qty — 입고수량. */
    @Column(name = "incoming_qty")
    private Integer incomingQty;

    /** IF02 tf_req_dept — 의뢰부서. */
    @Column(name = "req_dept", length = 10)
    private String reqDept;

    /** IF02 submitter_order — 의뢰자 (LIMS 기준). */
    @Column(name = "submitter_order", length = 10)
    private String submitterOrder;

    /** 화주 코드. 발신 이력 보존용 마스터 스냅샷. */
    @Column(name = "item_owner", length = 50)
    private String itemOwner;

    /** IF03 id_text — LIMS 결과 시험번호. 결과 도착 전 null. */
    @Column(name = "test_no", length = 100)
    private String testNo;

    /** 상태 코드 (QcTestRequestStatus). 기본 DRAFT. */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /** 상위(LIMS) 인수 여부. */
    @Column(name = "fetched", nullable = false)
    private Boolean fetched = false;

    /** 시험 결과 PDF 원본 파일 id. */
    @Column(name = "report_pdf_id", length = 40)
    private String reportPdfId;

    /** 결과 종결 시각. */
    @Column(name = "completed_at", type = ColumnType.DATETIME)
    private Date completedAt;
}