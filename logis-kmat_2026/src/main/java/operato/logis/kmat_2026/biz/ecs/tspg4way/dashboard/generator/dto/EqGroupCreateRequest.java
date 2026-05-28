package operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.generator.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 설비 그룹 생성 요청 DTO
 * tb_eq_group_mst 테이블에 데이터 생성용
 */
@Getter
@Setter
public class EqGroupCreateRequest {

    /**
     * 설비 그룹 ID (PK)
     * 예: TSPG_AMBIENT, TSPG_COLD
     */
    private String id;

    /**
     * 설비 그룹명
     * 예: 상온 자동창고, 저온 자동창고
     */
    private String name;

    /**
     * 설비 그룹 타입
     * 예: TSPG_4WAY
     */
    private String type;
}
