package operato.logis.asrs.dto.request;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 출고 재고 선택 요청 DTO.
 */
@Getter
@Setter
@ToString
public class OutboundLocationSelectRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 영역 코드 */
    @JsonAlias({"areaCode", "area_code"})
    private String areaCode;

    /** 품목 코드 */
    @JsonAlias({"itemCode", "item_code"})
    private String itemCode;

    /** 필요 수량 */
    @JsonAlias({"requiredQty", "required_qty"})
    private Integer requiredQty;

    /** 추천 건수 */
    private Integer limit;
}