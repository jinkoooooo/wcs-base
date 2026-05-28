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
 * 재고 재입고 요청 DTO.
 *
 * <p>
 * 1차 기준 재입고는 신규 stock_unit 생성 방식으로 처리한다.
 * 필요 시 원 출고 stockUnitNo 를 parent 추적으로 연결할 수 있도록 originalStockUnitNo 를 둔다.
 * </p>
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReturnInboundRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 신규 재고 단위 번호 */
    @JsonAlias({"stockUnitNo", "stock_unit_no"})
    private String stockUnitNo;

    /** 원 출고 재고 단위 번호 (선택) */
    @JsonAlias({"originalStockUnitNo", "original_stock_unit_no"})
    private String originalStockUnitNo;

    /** 품목코드 */
    @JsonAlias({"itemCode", "item_code"})
    private String itemCode;

    /** LOT번호 */
    @JsonAlias({"lotNo", "lot_no"})
    private String lotNo;

    /** 영역 코드 */
    @JsonAlias({"areaCode", "area_code"})
    private String areaCode;

    /** 재입고 로케이션 코드 */
    @JsonAlias({"locationCode", "location_code"})
    private String locationCode;

    /** 재고 단위 유형 */
    @JsonAlias({"stockUnitType", "stock_unit_type"})
    private String stockUnitType;

    /** 재입고 수량 */
    @JsonAlias({"qty"})
    private Integer qty;

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