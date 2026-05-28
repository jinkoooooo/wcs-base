package operato.logis.ecs.base.wcs.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.*;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

@Getter
@Setter
@Table(name = "tb_wcs_crane_order", idStrategy = GenerationRule.UUID,
        uniqueFields = "orderKey",
        indexes = {
                @Index(name = "ux_tb_wcs_crane_order_key", columnList = "order_key", unique = true),
        })
public class TbWcsCraneOrder extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 50)
    private String id;

    @Column(name = "order_key", nullable = false, length = 100)
    private String orderKey;

    @Column(name = "order_type", nullable = false, length = 30)
    private String orderType;

    @Column(name = "order_status", nullable = false)
    private int orderStatus;

    @Column(name = "priority", nullable = false)
    private int priority = 0;

    @Column(name = "from_loc_code", nullable = true, length = 50)
    private String fromLocCode;

    @Column(name = "to_loc_code", nullable = true, length = 50)
    private String toLocCode;

    @Column(name = "ecs_if_status", nullable = false)
    private int ecsIfStatus = 0;

    @Column(name = "owner_code", nullable = true, length = 20)
    private String ownerCode;

    @Column(name = "eq_group_id", nullable = false, length = 20)
    private String eqGroupId;

    @Column(name = "barcode", nullable = true, length = 100)
    private String barcode;

    @Column(name = "level", nullable = true)
    private int level;

    @Column(name = "parent_order_key", nullable = true, length = 100)
    private String parentOrderKey;

    @Column(name = "remark", nullable = true, length = 4000)
    private String remark;

    /** 번들 기아 방지용 폴링 사이클 카운터 (쿼터 부족으로 대기한 횟수) — OUTBOUND 전용 */
    @Column(name = "aging_count", nullable = true)
    private Integer agingCount = 0;

    /** ECS 전송 실패 재시도 횟수 — OUTBOUND 전용 */
    @Column(name = "retry_count", nullable = true)
    private Integer retryCount = 0;

    /** 운영자 강제 방출 플래그 (1=강제, 0=일반) — OUTBOUND 전용 */
    @Column(name = "force_release", nullable = true)
    private Integer forceRelease = 0;

    /** 이 오더가 운반 중인 재고의 stockId (FROM_LOADING 시점에 세팅, COMPLETE 시점에 to에 매핑) */
    @Column(name = "carrying_stock_id", nullable = true, length = 100)
    private String carryingStockId;

    @Column(name = "host_order_key", nullable = true, length = 100)
    private String hostOrderKey;

    /** 시험 대상 여부 — host_order.test_required 에서 복사 (운영 편의용 readonly). */
    @Column(name = "test_required")
    private Boolean testRequired;
}