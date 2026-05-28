package operato.logis.wcs.dto.lims;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * IF01 자재마스터 단건.
 */
@Getter
@Setter
public class If01Item {

    /** 자재 코드 (실제 자재 코드). */
    @JsonProperty("product_code")
    private String productCode;

    /** 자재명. */
    private String name;

    /** "insert": 추가, "update": 수정, "delete": 삭제. */
    private String type;
}