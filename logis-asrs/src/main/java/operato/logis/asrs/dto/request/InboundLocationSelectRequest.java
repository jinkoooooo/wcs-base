package operato.logis.asrs.dto.request;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 입고 로케이션 추천 요청 DTO.
 */
@Getter
@Setter
@ToString
public class InboundLocationSelectRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 영역 코드 */
    @JsonAlias({"areaCode", "area_code"})
    private String areaCode;

    /** 품목 코드 */
    @JsonAlias({"itemCode", "item_code"})
    private String itemCode;

    /** 추천 건수 */
    private Integer limit;
}