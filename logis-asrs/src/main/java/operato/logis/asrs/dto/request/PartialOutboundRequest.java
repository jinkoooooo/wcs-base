package operato.logis.asrs.dto.request;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 재고 부분출고 요청 DTO.
 *
 * <p>
 * 외부 입력은 business key 기준으로 받는다.
 * </p>
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PartialOutboundRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 재고 단위 번호 */
    @JsonAlias({"stockUnitNo", "stock_unit_no"})
    private String stockUnitNo;

    /** 출고 수량 */
    @JsonAlias({"outboundQty", "outbound_qty"})
    private Integer outboundQty;

    /** 참조 문서 유형 */
    @JsonAlias({"refDocType", "ref_doc_type"})
    private String refDocType;

    /** 참조 문서 번호 */
    @JsonAlias({"refDocNo", "ref_doc_no"})
    private String refDocNo;

    /** 참조 문서 라인 번호 */
    @JsonAlias({"refLineNo", "ref_line_no"})
    private String refLineNo;

    /** 사유 코드 */
    @JsonAlias({"reasonCode", "reason_code"})
    private String reasonCode;

    /** 비고 */
    @JsonAlias({"remark"})
    private String remark;
}