package operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.generator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 단일 랙 셀 생성 요청 DTO
 * tb_eq_rack_mst 테이블에 데이터 생성용
 */
@Getter
@Setter
public class RackCellCreateRequest {

    /**
     * 셀 ID (PK)
     * 규칙: {level}{row}{bay} 또는 사용자 정의
     * 예: 10101 (1층 1열 1행)
     */
    private String id;

    /**
     * 설비 ID (FK: tb_eq_mst.id)
     */
    @JsonProperty("eqId")
    private String eqId;

    /**
     * 랙 타입 (EcsDBConsts.RackType 기준)
     * 11: CELL, 21: INBOUND_PORT, 22: OUTBOUND_PORT, 31: CHARGE_PORT
     */
    private int type;

    /**
     * 행 위치 (Row)
     */
    private int row;

    /**
     * 열 위치 (Bay)
     */
    private int bay;

    /**
     * 층 위치 (Level)
     */
    private int level;

    /**
     * 주행 전용 여부 (화물 보관 불가)
     */
    @JsonProperty("driveOnlyYn")
    private boolean driveOnlyYn;

    /**
     * 사용 여부
     */
    @JsonProperty("useYn")
    private boolean useYn = true;
}
