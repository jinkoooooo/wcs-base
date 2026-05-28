package operato.logis.asrs.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * 상품 사용 여부 변경 요청 DTO.
 */
@Data
public class ItemActiveToggleRequest {

    /** Y / N */
    @JsonAlias({"activeYn", "active_yn"})
    private String activeYn;
}