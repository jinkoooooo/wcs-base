package operato.logis.wcs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 라벨 재발행 요청 DTO.
 *
 * 박스/파렛트 재발행 엔드포인트 공통 페이로드. 재발행 시 운영자 코멘트 필수.
 * 감사 로그 reason 에 "LABEL_REPRINT: {comment}" 형식으로 적재된다.
 */
@Getter
@Setter
public class LabelReissueRequest {

    @JsonProperty("comment")
    private String comment;
}
