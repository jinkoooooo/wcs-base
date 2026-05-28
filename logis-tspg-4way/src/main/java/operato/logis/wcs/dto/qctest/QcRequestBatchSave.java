package operato.logis.wcs.dto.qctest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * QC 시험의뢰 일괄 저장 요청 DTO. QC 시험요청/입고 등록 화면 공용.
 *
 * 신규/수정 판단:
 *   - (inbound_date, item_code, lot_no) 가 있으면 수정(upsert), 없으면 신규.
 *   - test_request_no 는 신규 시 서버가 자동 발번하므로 받지 않는다.
 *   - manufacturer / item_owner / mfr_unit 은 서버가 item_code 로 자재마스터에서 복사하므로 받지 않는다.
 */
@Getter
@Setter
public class QcRequestBatchSave {

    @JsonProperty("entries")
    private List<QcRequestEntry> entries = new ArrayList<>();

    /**
     * QC 시험의뢰 1건. 정체성은 (inbound_date, item_code, lot_no).
     */
    @Getter
    @Setter
    public static class QcRequestEntry {

        /** 입고일자 "yyyy-MM-dd". (식별 키 1/3) */
        @JsonProperty("inbound_date")
        private String inboundDate;

        /** 품목코드(SKU). IF02 product_code. (식별 키 2/3) */
        @JsonProperty("item_code")
        private String itemCode;

        /** 로트번호. 미부여는 빈 문자열. IF02 tf_lot_id. (식별 키 3/3) */
        @JsonProperty("lot_no")
        private String lotNo;

        /** 시험 결과 PDF 파일 id (report_pdf_id). 첨부/교체 시에만. */
        @JsonProperty("file_id")
        private String fileId;

        /** 의뢰구분 (IF02 tf_wf_type) — 화면 입력. WF01, WF02, ... */
        @JsonProperty("test_wf_type")
        private String testWfType;

        /** 의뢰내용 (IF02 tf_req_desc) — 화면 입력. R, M, DP, IPC, ... */
        @JsonProperty("test_req_desc")
        private String testReqDesc;

        /** 제조일자 "yyyy-MM-dd" (IF02 tf_mfd_on) — 화면 입력. */
        @JsonProperty("manufactured_date")
        private String manufacturedDate;

        /** 사용기한 "yyyy-MM-dd" (IF02 tf_exp_on) — 화면 입력. */
        @JsonProperty("expiry_date")
        private String expiryDate;

        /** 제조 수량 (IF02 tf_mfr_qty) — 화면 입력. */
        @JsonProperty("manufactured_qty")
        private Integer manufacturedQty;

        /** 입고 수량 (IF02 tf_incom_cntr_qty) — 화면 입력. */
        @JsonProperty("incoming_qty")
        private Integer incomingQty;

        /** 의뢰부서 (IF02 tf_req_dept) — 화면 입력. */
        @JsonProperty("req_dept")
        private String reqDept;

        /** 의뢰자 (IF02 submitter_order) — 화면 입력. */
        @JsonProperty("submitter_order")
        private String submitterOrder;
    }
}