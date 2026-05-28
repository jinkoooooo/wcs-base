package operato.logis.ecs.base.ecs.dashboard.generator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * Rack cell 단건 update 요청 DTO
 *
 * 모든 필드는 optional. null 이면 변경하지 않음.
 * tb_eq_rack_mst 의 (eqGroupId, eqId, rackId) 3중 키 식별 후 부분 갱신 용도.
 */
@Getter
@Setter
public class RackCellUpdateRequest {

    /** 랙 타입 (EcsDBConsts.RackType) */
    private Integer type;

    /** 행 위치 */
    private Integer row;

    /** 열 위치 */
    private Integer bay;

    /** 층 위치 */
    private Integer level;

    /** 적재 SKU ID */
    @JsonProperty("skuId")
    private String skuId;

    /** 적재 SKU 수량 */
    @JsonProperty("skuQty")
    private Integer skuQty;

    /** 사용 여부 */
    @JsonProperty("useYn")
    private Boolean useYn;

    /** 화물 적재 여부 */
    @JsonProperty("cargoYn")
    private Boolean cargoYn;

    /** 버퍼 셀 여부 */
    @JsonProperty("bufferYn")
    private Boolean bufferYn;

    /** 주행 전용 여부 */
    @JsonProperty("driveOnlyYn")
    private Boolean driveOnlyYn;
}
