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
 * 재고 적치 요청 DTO.
 *
 * <p>
 * 신규 표준은 business key 기반이다.
 * </p>
 *
 * <ul>
 *   <li>재고단위: stockUnitNo</li>
 *   <li>영역: areaCode</li>
 *   <li>목표 로케이션: targetLocationCode</li>
 * </ul>
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PutawayStockRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 재고 단위 번호 */
    @JsonAlias({"stockUnitNo", "stock_unit_no"})
    private String stockUnitNo;

    /** 목표 영역 코드 */
    @JsonAlias({"areaCode", "area_code"})
    private String areaCode;

    /** 목표 적치 로케이션 코드 */
    @JsonAlias({"targetLocationCode", "target_location_code"})
    private String targetLocationCode;

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