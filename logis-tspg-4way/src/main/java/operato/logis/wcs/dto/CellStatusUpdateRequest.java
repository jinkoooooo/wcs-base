package operato.logis.wcs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 셀 상태 일괄 변경 요청 DTO (UI).
 *  - 개별 선택: cellIds 만 지정 → 해당 셀들에 action 적용
 *  - ZONE 일괄: cellIds 비우고 eqGroupId + level 지정 → 해당 ZONE/층 전체에 action 적용
 */
@Getter
@Setter
public class CellStatusUpdateRequest {

    @JsonProperty("cell_ids")
    private List<String> cellIds;

    @JsonProperty("action")
    private String action;

    @JsonProperty("eq_group_id")
    private String eqGroupId;

    @JsonProperty("level")
    private Integer level;

    /**
     * 운영자 코멘트
     * update_status 호출 시 필수. 감사 로그 reason 에 {@code "{action}: {comment}"} 형태로 적재된다.
     * 다른 엔드포인트(분류 변경, 재고 상세 조회)는 본 필드를 사용하지 않는다.
     */
    @JsonProperty("comment")
    private String comment;
}
