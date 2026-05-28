package operato.logis.wcs.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.*;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

import java.util.Date;

/**
 * 셔틀 작업 오더 품목 라인 엔티티.
 * 시험 관련 필드는 host_order_item 에서 복사된 스냅샷이다.
 */
@Getter
@Setter
@Table(name = "tb_wcs_shuttle_order_item", idStrategy = GenerationRule.UUID,
        indexes = {
                @Index(name = "ix_shuttle_order_item_key_item", columnList = "order_key, item_code")
        })
public class TbWcsShuttleOrderItem extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "order_key", length = 64)
    private String orderKey;

    @Column(name = "item_code", length = 64)
    private String itemCode;

    @Column(name = "lot_no", length = 40)
    private String lotNo;

    @Column(name = "qty")
    private int qty;

    @Column(name = "uom", length = 10)
    private String uom;

    @Column(name = "produce_date", type = ColumnType.DATETIME)
    private Date produceDate;

    @Column(name = "expiry_date", type = ColumnType.DATETIME)
    private Date expiryDate;

    /** 시험 대상 여부 — host_order_item 에서 복사. */
    @Column(name = "test_required")
    private Boolean testRequired;

    /** 시험 의뢰번호 — host_order_item 에서 복사. */
    @Column(name = "test_request_no", length = 50)
    private String testRequestNo;

    /** 시험 번호 — host_order_item 에서 복사. */
    @Column(name = "test_no", length = 50)
    private String testNo;

    /** 시험 상태 스냅샷 — host_order_item.test_status 산출 시점. */
    @Column(name = "test_status", length = 20)
    private String testStatus;

    @Column(name = "line_status")
    private int lineStatus;
}