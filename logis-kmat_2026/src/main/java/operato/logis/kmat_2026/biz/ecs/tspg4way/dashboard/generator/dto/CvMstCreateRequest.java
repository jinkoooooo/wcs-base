package operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.generator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 컨베이어/리프터 설비 생성 요청 DTO
 * tb_eq_cv_mst 테이블에 데이터 생성용
 */
@Getter
@Setter
public class CvMstCreateRequest {

    /**
     * 설비 ID (PK)
     */
    private String id;

    /**
     * 기본 설비 ID (FK: tb_eq_mst.id)
     */
    @JsonProperty("eqId")
    private String eqId;

    /**
     * 컨베이어 타입 (EcsDBConsts.ConveyorType 기준)
     * 1: GROUND, 2: INBOUND, 3: OUTBOUND, 11: LIFT, 12: RACK_IN
     */
    private int type;

    /**
     * 층 레벨
     */
    private int level;

    /**
     * 사용 여부
     */
    @JsonProperty("useYn")
    private boolean useYn = true;

    /**
     * 자동 모드 여부
     */
    @JsonProperty("autoYn")
    private boolean autoYn = true;
}
