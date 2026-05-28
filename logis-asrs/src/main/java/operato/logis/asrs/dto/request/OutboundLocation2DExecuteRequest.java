package operato.logis.asrs.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.Data;

/**
 * 2D 로케이션 지정출고 실행 요청 DTO.
 *
 * 규칙:
 * - snake_case / camelCase 모두 수용
 */
@Data
public class OutboundLocation2DExecuteRequest {

    @JsonAlias({"locationId", "location_id"})
    private String locationId;

    @JsonAlias({"stockUnitNo", "stock_unit_no"})
    private String stockUnitNo;

    @JsonAlias({"refDocType", "ref_doc_type"})
    private String refDocType;

    @JsonAlias({"refDocNo", "ref_doc_no"})
    private String refDocNo;

    @JsonAlias({"refLineNo", "ref_line_no"})
    private String refLineNo;

    @JsonAlias({"reasonCode", "reason_code"})
    private String reasonCode;

    @JsonAlias({"remark"})
    private String remark;
}