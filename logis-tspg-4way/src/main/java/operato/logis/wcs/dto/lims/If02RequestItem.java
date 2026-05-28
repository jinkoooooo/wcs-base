package operato.logis.wcs.dto.lims;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * IF02 시험 의뢰 단건.
 */
@Getter
@Setter
public class If02RequestItem {

    /** 시험 의뢰 번호 (WES <-> LIMS Test Key 값). */
    @JsonProperty("test_request_no")
    private String testRequestNo;

    /** 자재 코드. */
    @JsonProperty("product_code")
    private String productCode;

    /** 의뢰구분 (WF01, WF02, ...). */
    @JsonProperty("tf_wf_type")
    private String tfWfType;

    /** 의뢰 내용 (R, M, DP, IPC, ...). */
    @JsonProperty("tf_req_desc")
    private String tfReqDesc;

    /** 제조처. */
    @JsonProperty("tf_MFR")
    private String tfMfr;

    /** 제조번호. */
    @JsonProperty("tf_lot_id")
    private String tfLotId;

    /** 제조일자. */
    @JsonProperty("tf_mfd_on")
    private String tfMfdOn;

    /** 제조 수량. */
    @JsonProperty("tf_mfr_qty")
    private Integer tfMfrQty;

    /** 사용기한. */
    @JsonProperty("tf_mfr_unit")
    private String tfMfrUnit;

    /** 입고 수량. */
    @JsonProperty("tf_incom_cntr_qty")
    private Integer tfIncomCntrQty;
}