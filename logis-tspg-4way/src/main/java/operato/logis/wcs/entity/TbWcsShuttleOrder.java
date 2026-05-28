package operato.logis.wcs.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.*;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

import java.util.Date;

/**
 * 셔틀 작업 오더 헤더 엔티티.
 * order_key 단위 1건이며 ECS 송신 상태·선후행 관계·후속 작업 펜딩 정보를 보유한다.
 */
@Getter
@Setter
@Table(name = "tb_wcs_shuttle_order", idStrategy = GenerationRule.UUID,
        uniqueFields = "orderKey",
        indexes = {
                @Index(name = "ux_tb_wcs_shuttle_order_key", columnList = "order_key", unique = true),
                @Index(name = "ix_shuttle_order_type_status_upd", columnList = "order_type, order_status, updated_at")
        })
public class TbWcsShuttleOrder extends ElidomStampHook {

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

    /**
     * 선행 오더 키 — 가리키는 오더가 COMPLETED 되어야 본 오더가 송신 가능.
     * 방해물 부모 OUTBOUND (마지막 자식 MOVE 가리킴) / 형제 OUTBOUND 시퀀스 (앞 형제 가리킴) 에서 사용.
     */
    @Column(name = "prerequisite_order_key", nullable = true, length = 100)
    private String prerequisiteOrderKey;

    @Column(name = "remark", nullable = true, length = 4000)
    private String remark;

    /** 번들 기아 방지용 폴링 사이클 카운터 */
    @Column(name = "aging_count", nullable = true)
    private Integer agingCount = 0;

    /** 이 오더가 운반 중인 재고의 stockId (FROM_LOADING 시점에 세팅, COMPLETE 시점에 to에 매핑) */
    @Column(name = "carrying_stock_id", nullable = true, length = 100)
    private String carryingStockId;

    @Column(name = "host_order_key", nullable = true, length = 100)
    private String hostOrderKey;

    @Column(name = "sub_order_type", nullable = true, length = 100)
    private String subOrderType;

    /**
     * 시험 대상 여부 — host_order.test_required 에서 복사 (운영 편의용 readonly).
     */
    @Column(name = "test_required")
    private Boolean testRequired;

    /** 후속 작업(재입고 등) 펜딩 시작 시간. NULL = 후속 없음 또는 이미 발급됨. */
    @Column(name = "follow_up_since", nullable = true, type = ColumnType.DATETIME)
    private Date followUpSince;
}