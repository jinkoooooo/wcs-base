package operato.logis.wcs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 수동 출고 지시 요청.
 *
 * 운영자가 Dashboard2D 셀 메뉴에서 수동 출고를 누를 때 본 DTO 로 호출.
 * comment 는 필수. portCode 는 미지정 시 자동 배정.
 */
@Getter
@Setter
public class ManualOutboundRequest {

    @JsonProperty("eqGroupId")
    private String eqGroupId;

    /** 출고 포트 코드. 비워두면 자동 배정. */
    @JsonProperty("portCode")
    private String portCode;

    @JsonProperty("comment")
    private String comment;
}
