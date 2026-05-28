package operato.logis.ecs.tspg4way.dashboard.generator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 랙 셀 일괄 생성 요청 DTO (GRID 모드). 격자 범위(row/bay/level)로 셀을 일괄 생성하며 층마다 별도 페이지 생성/조회.
 * ID 규칙 {level}{row:02d}{bay:02d} (예: 1/6/1 → 10601). 좌표계: 원점=좌하단, Bay=X축(→), Row=Y축(↑), Level=Z축(층별 독립 페이지).
 */
@Getter
@Setter
public class RackBulkCreateRequest {

    // 2D Dashboard 연동 설정

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

    // 기본 설비 설정

    /**
     * 기본 설비 ID (tb_eq_mst.id)
     */
    @JsonProperty("eqId")
    private String eqId;

    /**
     * 시작 층 (tb_eq_rack_mst.level)
     */
    @JsonProperty("startLevel")
    private int startLevel = 1;

    /**
     * 끝 층 (tb_eq_rack_mst.level)
     * startLevel == endLevel 이면 단일 층
     */
    @JsonProperty("endLevel")
    private int endLevel = 1;

    /**
     * 랙 타입 (EcsDBConsts.RackType)
     * 11: CELL, 21: INBOUND_PORT, 22: OUTBOUND_PORT, 23: IN_OUTBOUND_PORT
     * 31: CHARGE_PORT, 32: CHARGE_ENTER_PORT
     */
    @JsonProperty("rackType")
    private int rackType = 11;

    // GRID 범위 설정

    /** 시작 Row */
    @JsonProperty("startRow")
    private int startRow = 1;

    /** 끝 Row */
    @JsonProperty("endRow")
    private int endRow = 1;

    /** 시작 Bay */
    @JsonProperty("startBay")
    private int startBay = 1;

    /** 끝 Bay */
    @JsonProperty("endBay")
    private int endBay = 1;

    // 주행 전용 설정 (전체 층 공통 적용)

    /**
     * 주행 전용 Row 목록
     * 예: [2, 6] → 2행과 6행은 주행 전용 (모든 층 공통)
     */
    @JsonProperty("driveOnlyRows")
    private List<Integer> driveOnlyRows;

    /**
     * 주행 전용 Bay 목록
     * 예: [1, 10] → 1열과 10열은 주행 전용 (모든 층 공통)
     */
    @JsonProperty("driveOnlyBays")
    private List<Integer> driveOnlyBays;

    // 특수 셀 설정 (전체 층 공통 적용)

    /**
     * 특수 셀 설정 (충전포트, 입출고포트 등)
     * 특정 row/bay에 다른 타입을 지정할 때 사용 (모든 층 공통)
     */
    @JsonProperty("specialCells")
    private List<SpecialCellConfig> specialCells;

    // 공통 기본값

    /** 사용 여부 */
    @JsonProperty("useYn")
    private boolean useYn = true;

    /** 생성 모드 (현재 GRID만 지원) */
    @JsonProperty("createMode")
    private String createMode = "GRID";

    /**
     * 특수 셀 설정 DTO
     */
    @Getter
    @Setter
    public static class SpecialCellConfig {
        /** Row 번호 */
        private int row;

        /** Bay 번호 */
        private int bay;

        /** 랙 타입 (EcsDBConsts.RackType) */
        private int type;

        /** 주행 전용 여부 (null이면 기본 규칙 따름) */
        @JsonProperty("driveOnlyYn")
        private Boolean driveOnlyYn;
    }

    // 하위 호환 (구버전 floorLevel 필드)

    /**
     * @deprecated startLevel / endLevel 을 사용하세요.
     *
     * 구버전 호환용 getter. startLevel 을 반환.
     */
    @Deprecated
    @JsonProperty("floorLevel")
    public int getFloorLevel() {
        return startLevel;
    }

    /**
     * @deprecated startLevel / endLevel 을 사용하세요.
     *
     * 구버전 호환용 setter. startLevel 과 endLevel 을 동시에 설정.
     * 구 버전 요청({"floorLevel": 3}) 이 들어와도 동작하게 한다.
     */
    @Deprecated
    @JsonProperty("floorLevel")
    public void setFloorLevel(int floorLevel) {
        this.startLevel = floorLevel;
        this.endLevel = floorLevel;
    }
}
