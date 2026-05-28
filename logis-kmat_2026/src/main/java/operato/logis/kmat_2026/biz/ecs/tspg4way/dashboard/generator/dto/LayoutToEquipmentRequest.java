package operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.generator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 2D 레이아웃 → 설비 일괄 생성 요청 DTO
 * 레이아웃 아이템들을 기반으로 실운영 설비 테이블에 데이터 생성
 */
@Getter
@Setter
public class LayoutToEquipmentRequest {

    /**
     * LC ID
     */
    @JsonProperty("lcId")
    private String lcId;

    /**
     * 설비 그룹 ID
     * 기존 그룹 사용 시: 해당 ID
     * 신규 생성 시: null + newEqGroup 설정
     */
    @JsonProperty("eqGroupId")
    private String eqGroupId;

    /**
     * 신규 설비 그룹 생성 정보
     */
    @JsonProperty("newEqGroup")
    private EqGroupCreateRequest newEqGroup;

    /**
     * 페이지 ID (대상 층)
     */
    @JsonProperty("pageId")
    private String pageId;

    /**
     * 대상 레이아웃 아이템 ID 목록
     * null/empty면 해당 페이지의 모든 RACK/CONVEYOR/LIFTER 아이템 대상
     */
    @JsonProperty("targetLayoutIds")
    private List<String> targetLayoutIds;

    /**
     * 좌표 → row/bay 변환 설정
     */
    @JsonProperty("coordinateConfig")
    private CoordinateConfig coordinateConfig;

    /**
     * 기존 설비 처리 방식
     * SKIP: 이미 매핑된 것은 건너뜀
     * OVERWRITE: 기존 매핑 덮어쓰기
     * ERROR: 이미 매핑된 것이 있으면 에러
     */
    @JsonProperty("existingMode")
    private String existingMode = "SKIP";

    @Getter
    @Setter
    public static class CoordinateConfig {
        /**
         * 원점 X (픽셀)
         */
        @JsonProperty("originX")
        private int originX = 0;

        /**
         * 원점 Y (픽셀)
         */
        @JsonProperty("originY")
        private int originY = 0;

        /**
         * 셀 너비 (픽셀)
         */
        @JsonProperty("cellWidth")
        private int cellWidth = 100;

        /**
         * 셀 높이 (픽셀)
         */
        @JsonProperty("cellHeight")
        private int cellHeight = 100;

        /**
         * Row 방향
         * X: posX 기준 (좌→우)
         * Y: posY 기준 (상→하)
         */
        @JsonProperty("rowDirection")
        private String rowDirection = "Y";

        /**
         * Bay 방향
         * X: posX 기준 (좌→우)
         * Y: posY 기준 (상→하)
         */
        @JsonProperty("bayDirection")
        private String bayDirection = "X";
    }
}
