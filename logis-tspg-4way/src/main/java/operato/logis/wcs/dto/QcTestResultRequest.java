package operato.logis.wcs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 외부 검수 시스템(LIMS)이 WCS 로 검수 결과를 통보할 때 사용하는 DTO.
 * result = "PASSED" | "FAILED"
 * As-Is TestResultRequest / QcTestResultRequest 두 DTO를 통합.
 */
@Getter
@Setter
public class QcTestResultRequest {

    /** LIMS 가 발급한 시험 식별자. */
    @JsonProperty("testNo")
    private String testNo;

    @JsonProperty("result")
    private String result;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("testerId")
    private String testerId;
}
