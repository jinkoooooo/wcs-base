package operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.generator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 랙 셀 일괄 생성 요청 DTO (GRID 모드)
 *
 * 격자 범위(startRow~endRow, startBay~endBay)를 지정하여 랙 셀을 일괄 생성
 * 동시에 2D Dashboard 아이템(tb_ecs_2d_item)도 자동 생성 가능
 *
 * [ID 생성 규칙]
 * {level}{row:02d}{bay:02d}
 * 예: level=1, row=6, bay=1 → 10601
 *
 * [좌표계]
 * 원점 (Row=0, Bay=0): 왼쪽 하단
 * Bay (X축): 오른쪽으로 증가 →
 * Row (Y축): 위쪽으로 증가 ↑
 *
 * @author WCS Development Team
 * @since 2026-03-27
 */
@Getter
@Setter
public class RackBulkCreateRequest {

    // ============================================
    // 2D Dashboard 연동 설정
    // ============================================

    /**
     * 센터 ID (tb_ecs_2d_page.lc_id)
     * 2D 아이템 생성 시 필수
     */
    @JsonProperty("lcId")
    private String lcId;

    /**
     * 설비 그룹 ID (tb_ecs_2d_page.eq_group_id)
     * 2D 페이지 생성/조회 시 사용
     */
    @JsonProperty("eqGroupId")
    private String eqGroupId;

    /**
     * 2D Dashboard 아이템 자동 생성 여부
     * true: tb_eq_rack_mst와 함께 tb_ecs_2d_item도 생성
     */
    @JsonProperty("create2dItems")
    private boolean create2dItems = true;

    // ============================================
    // 기본 설비 설정
    // ============================================

    /**
     * 기본 설비 ID (tb_eq_mst.id)
     */
    @JsonProperty("eqId")
    private String eqId;

    /**
     * 층 레벨 (tb_eq_rack_mst.level)
     */
    @JsonProperty("floorLevel")
    private int floorLevel = 1;

    /**
     * 랙 타입 (EcsDBConsts.RackType)
     * 11: CELL, 21: INBOUND_PORT, 22: OUTBOUND_PORT, 23: IN_OUTBOUND_PORT
     * 31: CHARGE_PORT, 32: CHARGE_ENTER_PORT
     */
    @JsonProperty("rackType")
    private int rackType = 11;

    // ============================================
    // GRID 범위 설정
    // ============================================

    /**
     * 시작 Row
     */
    @JsonProperty("startRow")
    private int startRow = 1;

    /**
     * 끝 Row
     */
    @JsonProperty("endRow")
    private int endRow = 1;

    /**
     * 시작 Bay
     */
    @JsonProperty("startBay")
    private int startBay = 1;

    /**
     * 끝 Bay
     */
    @JsonProperty("endBay")
    private int endBay = 1;

    // ============================================
    // 주행 전용 설정
    // ============================================

    /**
     * 주행 전용 Row 목록
     * 예: [2, 6] → 2행과 6행은 주행 전용
     */
    @JsonProperty("driveOnlyRows")
    private List<Integer> driveOnlyRows;

    /**
     * 주행 전용 Bay 목록
     * 예: [1, 10] → 1열과 10열은 주행 전용
     */
    @JsonProperty("driveOnlyBays")
    private List<Integer> driveOnlyBays;

    // ============================================
    // 특수 셀 설정
    // ============================================

    /**
     * 특수 셀 설정 (충전포트, 입출고포트 등)
     * 특정 row/bay에 다른 타입을 지정할 때 사용
     */
    @JsonProperty("specialCells")
    private List<SpecialCellConfig> specialCells;

    // ============================================
    // 공통 기본값
    // ============================================

    /**
     * 사용 여부
     */
    @JsonProperty("useYn")
    private boolean useYn = true;

    /**
     * 생성 모드 (현재 GRID만 지원)
     */
    @JsonProperty("createMode")
    private String createMode = "GRID";

    /**
     * 특수 셀 설정 DTO
     */
    @Getter
    @Setter
    public static class SpecialCellConfig {
        /**
         * Row 번호
         */
        private int row;

        /**
         * Bay 번호
         */
        private int bay;

        /**
         * 랙 타입 (EcsDBConsts.RackType)
         */
        private int type;

        /**
         * 주행 전용 여부 (null이면 기본 규칙 따름)
         */
        @JsonProperty("driveOnlyYn")
        private Boolean driveOnlyYn;
    }
}
