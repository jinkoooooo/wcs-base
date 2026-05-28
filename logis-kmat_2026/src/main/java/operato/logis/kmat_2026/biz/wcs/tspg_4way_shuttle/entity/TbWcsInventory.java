package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.*;
import xyz.elidom.orm.entity.basic.ElidomStampHook;
import java.util.Date;

/**
 * [WCS 통합 재고 마스터]
 * 15년 차 선배의 조언:
 * 1. 이 테이블은 모든 물류 설비의 "화물 정보"를 담는 핵심 엔티티입니다.
 * 2. pallet_id(LPN)를 도입하여 SKU 단위가 아닌 '물리적 화물' 단위의 추적성을 확보했습니다.
 * 3. inbound_at을 통해 생성일(createdAt)과 별개로 실제 입고 완료 시점 기준의 FIFO를 지원합니다.
 */
@Getter
@Setter
@Table(name = "tb_wcs_inventory", idStrategy = GenerationRule.UUID,
        uniqueFields = "eqGroupId,locCode,ownerCode,skuCode,lotNo,palletId",
        indexes = {
                // [Unique Index] 특정 위치에 동일 팔레트/SKU 중복 생성 방지
                @Index(name = "ux_tb_wcs_inventory_key", columnList = "eq_group_id,loc_code,owner_code,sku_code,lot_no,pallet_id", unique = true),

                // [Search Index] 가용 재고 조회 및 선입선출(FIFO) 성능 최적화
                @Index(name = "ix_tb_wcs_inv_search", columnList = "eq_group_id,sku_code,stock_status,inbound_at"),

                // [LPN Search Index] 팔레트 번호로 위치를 즉시 찾기 위한 인덱스
                @Index(name = "ix_tb_wcs_inv_pallet", columnList = "pallet_id"),

                // [Location Search Index] 특정 로케이션의 내용물 조회용
                @Index(name = "ix_tb_wcs_inv_loc", columnList = "eq_group_id,loc_code")
        }
)
public class TbWcsInventory extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "loc_code", length = 64)
    private String locCode;

    @Column(name = "eq_group_id", length = 20)
    private String eqGroupId;

    @Column(name = "owner_code", length = 20)
    private String ownerCode;

    @Column(name = "sku_code", length = 64)
    private String skuCode;

    @Column(name = "lot_no", length = 40)
    private String lotNo;

    /**
     * 팔레트 식별 번호 (LPN)
     * 범용성: 셔틀뿐만 아니라 모든 설비에서 화물을 추적하는 유일키로 활용
     */
    @Column(name = "pallet_id", length = 50)
    private String palletId;

    @Column(name = "qty")
    private int qty;

    /**
     * 작업 할당 수량 (WCS 예약 수량)
     */
    @Column(name = "alloc_qty")
    private int allocQty;

    /**
     * 재고 상태 (1: 정상, 2: 보류, 9: 불량, 99: Mismatch 등)
     */
    @Column(name = "stock_status")
    private int stockStatus;

    /**
     * 실제 입고 완료 일시
     * 범용성: 시스템 생성 시간이 아닌 실물 입고 시간 기준으로 정확한 FIFO(선입선출) 보장
     */
    @Column(name = "inbound_at")
    private Date inboundAt;

    /**
     * 유통기한
     * 범용성: 식품, 의약품 등 유통기한 관리가 필수인 업종 대응
     */
    @Column(name = "exp_date")
    private Date expDate;

    /**
     * 실측 중량 (kg)
     * 범용성: 과적 방지, 설비 부하 계산 및 안전 센싱 데이터로 활용
     */
    @Column(name = "measured_weight")
    private Double measuredWeight;

    /**
     * 잠금 여부 (0: 해제, 1: 잠금)
     * 공출고 등 발생 시 해당 재고를 격리하기 위한 용도
     */
    @Column(name = "lock_yn")
    private int lockYn;
}