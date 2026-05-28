package operato.logis.wcs.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.ColumnType;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Index;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

import java.util.Date;

/**
 * HOST 수신 주문 품목 라인 엔티티.
 * 시험·국검 진행 상태의 진실의 원천은 본 라인 단위이며, 헤더 값은 라인 집계 캐시다.
 */
@Getter
@Setter
@Table(name = "tb_wcs_host_order_item", idStrategy = GenerationRule.UUID,
        indexes = {
                @Index(name = "ux_tb_wcs_host_order_item_test_no", columnList = "test_no", unique = true),
                @Index(name = "ix_host_order_item_key_item_lot", columnList = "host_order_key, item_code, lot_no")
        })
public class TbWcsHostOrderItem extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "host_system_code", length = 20)
    private String hostSystemCode;

    @Column(name = "host_order_key", length = 64)
    private String hostOrderKey;

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

    /** 시험 대상 여부 (item 단위 — 진실의 원천). 헤더 host_order.test_required 는 item OR 집계. */
    @Column(name = "test_required")
    private Boolean testRequired;

    /** 시험 의뢰번호 — test_required=true 일 때 채워야 함. LOT 단위 식별자. */
    @Column(name = "test_request_no", length = 50)
    private String testRequestNo;

    /** 시험 번호 — test_required=true 일 때 채워야 함. 시스템 전역 unique. */
    @Column(name = "test_no", length = 50)
    private String testNo;

    /** 시험 진행 상태 (item 단위). null=비대상, REQUESTED/PASSED/FAILED. */
    @Column(name = "test_status", length = 20)
    private String testStatus;

    @Column(name = "test_requested_at", type = ColumnType.DATETIME)
    private Date testRequestedAt;

    @Column(name = "test_resulted_at", type = ColumnType.DATETIME)
    private Date testResultedAt;

    @Column(name = "test_reason", length = 500)
    private String testReason;

    @Column(name = "nia_required")
    private Boolean niaRequired;

    /** 국검 승인번호 */
    @Column(name = "nia_approval_no", length = 50)
    private String niaApprovalNo;

    @Column(name = "line_status")
    private int lineStatus;

    @Column(name = "wcs_order_item_id", length = 50)
    private String wcsOrderItemId;

    @Column(name = "raw_attr")
    private String rawAttr;
}