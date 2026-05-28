package operato.logis.wcs.dto.lims;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * LIMS → WES 공통 요청 베이스. 모든 인터페이스의 최상위 공통 필드.
 */
@Getter
@Setter
public class LimsBaseRequest {

    /** 요청 ID. */
    @JsonProperty("request_id")
    private String requestId;

    /** 요청 시간 (yyyyMMdd). */
    @JsonProperty("req_dt")
    private String reqDt;
}