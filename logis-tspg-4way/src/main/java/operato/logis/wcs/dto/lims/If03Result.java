package operato.logis.wcs.dto.lims;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * IF03 판정결과 단건.
 */
@Getter
@Setter
public class If03Result {

    /** 시험 의뢰 번호 (WES <-> LIMS Key 값). IF02 의 test_request_no 와 동일. */
    @JsonProperty("test_request_no")
    private String testRequestNo;

    /** 시험 번호 (LIMS 발번 값) = test_no. */
    @JsonProperty("id_text")
    private String idText;

    /** "P": 적합, "F": 부적합, "C": 취소. */
    @JsonProperty("tf_decision")
    private String tfDecision;
}