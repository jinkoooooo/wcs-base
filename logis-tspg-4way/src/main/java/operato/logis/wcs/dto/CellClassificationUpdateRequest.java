package operato.logis.wcs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 셀 분류·제약 일괄 변경 요청 DTO.
 *
 * 각 필드(item_type, item_group, max_weight, max_height) 는 {@link FieldChange}
 * 형태로 받아 mode = "set" / "clear" / "skip" 으로 의도를 명확히 분리한다.
 *  - set   : value 로 UPDATE
 *  - clear : 기본값(빈문자/0)으로 UPDATE — DB NOT NULL 보호
 *  - skip  : SET 절에서 제외 (변경 안 함)
 *
 * 대상 선택:
 *  - 개별 선택: cellIds 만 지정
 *  - ZONE 전체: cellIds 비우고 eqGroupId + level 지정
 *
 * eqGroupId 는 항상 필수 — tb_inventory_location 의 동일 loc_id 가 ZONE 별
 * 병존하므로 다른 ZONE 오염 방지.
 */
@Getter
@Setter
public class CellClassificationUpdateRequest {

    @JsonProperty("cell_ids")
    private List<String> cellIds;

    @JsonProperty("eq_group_id")
    private String eqGroupId;

    @JsonProperty("level")
    private Integer level;

    @JsonProperty("item_type")
    private FieldChange<String> itemType;

    @JsonProperty("item_group")
    private FieldChange<String> itemGroup;

    @JsonProperty("max_weight")
    private FieldChange<Integer> maxWeight;

    @JsonProperty("max_height")
    private FieldChange<Integer> maxHeight;

    @Getter
    @Setter
    public static class FieldChange<T> {
        /** "set" | "clear" | "skip" — null 또는 미지정 시 skip 으로 간주 */
        @JsonProperty("mode")
        private String mode;

        @JsonProperty("value")
        private T value;
    }
}
