package operato.logis.ecs.base.ecs.dashboard.realtime.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 셀 상태 일괄 변경 요청 DTO.
 *
 * 두 가지 사용 패턴을 지원:
 *  1) 개별 선택:  cellIds 만 지정 → 해당 셀들에 action 적용
 *  2) ZONE 일괄: cellIds 비우고 eqGroupId + level 지정
 *              → FORBID_IN_ALL / FORBID_OUT_ALL / LOCK / UNLOCK 등에서
 *                해당 층 전체를 대상으로 action 적용
 *
 * 또한 /stock_detail_multi 에서도 cellIds 필드를 재사용한다.
 */
public class CellStatusUpdateRequest {

    /** 대상 셀 id 목록. 개별 선택 시 사용 */
    @JsonProperty("cell_ids")
    private List<String> cellIds;

    /** 수행할 액션 코드 */
    @JsonProperty("action")
    private String action;

    /** ZONE 일괄 적용 시 대상 ZONE */
    @JsonProperty("eq_group_id")
    private String eqGroupId;

    /** ZONE 일괄 적용 시 대상 적재단 */
    @JsonProperty("level")
    private Integer level;

    public List<String> getCellIds() {
        return cellIds;
    }

    public void setCellIds(List<String> cellIds) {
        this.cellIds = cellIds;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEqGroupId() {
        return eqGroupId;
    }

    public void setEqGroupId(String eqGroupId) {
        this.eqGroupId = eqGroupId;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }
}