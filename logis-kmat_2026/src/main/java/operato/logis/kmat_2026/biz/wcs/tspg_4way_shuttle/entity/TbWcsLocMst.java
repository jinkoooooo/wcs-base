package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.*;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

/**
 * [WCS 로케이션 마스터]
 * 15년 차 팁: 로케이션 데이터는 정적이지만 조회가 빈번하므로 인덱스 설계가 핵심입니다.
 */
@Getter
@Setter
@Table(name = "tb_wcs_loc_mst", idStrategy = GenerationRule.UUID,
        // [핵심] 구역(eqGroupId) 내에서 로케이션 코드는 유일해야 함
        uniqueFields = "locCode,eqGroupId",
        indexes = {
                @Index(name = "ux_tb_wcs_loc_mst_code", columnList = "loc_code,eq_group_id", unique = true),
                @Index(name = "ix_tb_wcs_loc_mst_group", columnList = "eq_group_id,loc_type,use_yn"),
                @Index(name = "ix_tb_wcs_loc_mst_rack", columnList = "rack_eq_id,rack_cell_id"),
                @Index(name = "ix_tb_wcs_loc_mst_lock", columnList = "lock_yn,lock_by")
        }
)
public class TbWcsLocMst extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "loc_code", length = 64)
    private String locCode;

    @Column(name = "eq_group_id", length = 20)
    private String eqGroupId;

    @Column(name = "loc_type", length = 16)
    private String locType;

    /**
     * 물리 랙 셀 ID (tb_eq_rack_mst.id 매핑용)
     * 비즈니스 정책상 locCode와 동일한 값을 가짐
     */
    @Column(name = "rack_cell_id", length = 50)
    private String rackCellId;

    /**
     * 물리 랙 장비 ID (tb_eq_rack_mst.eq_id 매핑용)
     */
    @Column(name = "rack_eq_id", length = 50)
    private String rackEqId;

    @Column(name = "capacity")
    private int capacity;

    @Column(name = "max_item_height_mm")
    private Integer maxItemHeightMm;

    @Column(name = "max_item_width_mm")
    private Integer maxItemWidthMm;

    @Column(name = "max_item_length_mm")
    private Integer maxItemLengthMm;

    @Column(name = "max_weight_kg")
    private Double maxWeightKg;

    /**
     * 잠금 여부 (0: 해제, 1: 잠금)
     */
    @Column(name = "lock_yn")
    private int lockYn;

    /**
     * 잠금 주체 (예: OrderKey — 선점한 작업 번호)
     */
    @Column(name = "lock_by", length = 50)
    private String lockBy;

    @Column(name = "use_yn")
    private int useYn;

    @Column(name = "status")
    private Integer status;

    @Column(name = "loc_seq")
    private Integer locSeq;
}