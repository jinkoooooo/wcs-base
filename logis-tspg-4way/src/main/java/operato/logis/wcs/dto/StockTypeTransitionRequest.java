package operato.logis.wcs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 재고 카테고리(stock_type) 전이 요청.
 *
 * 운영자가 Dashboard2D 셀 메뉴에서 반품/폐기/국검 승인/국검 미승인/국검 대기 복귀 등을
 * 누를 때 본 DTO 로 호출. comment 는 모든 액션에 필수. test_request_no 는
 * to=NIA_PENDING 인 경우 선택적으로 매핑.
 */
@Getter
@Setter
public class StockTypeTransitionRequest {

    @JsonProperty("to")
    private String to;

    @JsonProperty("comment")
    private String comment;

    /** to=NIA_PENDING 인 경우에만 의미 있음. 비어 있어도 전환은 진행 (audit 에 미입력 표시). */
    @JsonProperty("testRequestNo")
    private String testRequestNo;
}
