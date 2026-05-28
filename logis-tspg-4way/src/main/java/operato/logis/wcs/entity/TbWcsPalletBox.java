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
import xyz.elidom.util.ValueUtil;

import java.util.Date;

/**
 * WCS 파렛트 박스 엔티티.
 *
 * 수량 모델:
 *   total_qty     - 박스 처음 담긴 양. 입고 후 불변. 라벨 인쇄용
 *   picked_qty    - 이번 출고로 빠진 양. 출고 확정 시 0으로 리셋
 *   remaining_qty - 박스 현재 보유량. 평소엔 불변. 출고 확정 시 picked_qty 만큼 차감
 *
 * box_seq / box_barcode 는 입고 등록 시 NULL 이며 확정 액션으로 일괄 발번된다.
 * box_seq 는 (item_code, lot_no, 입고일자) 그룹 단위 1부터 증가하며 파렛트 경계를 넘어 이어지고,
 * box_barcode = B-{itemCode}-{lotNo}-{YYYYMMDD}-{seq4} 로 확정 후 불변이다.
 */
@Getter
@Setter
@Table(name = "tb_wcs_pallet_box", idStrategy = GenerationRule.UUID,
        indexes = {
                @Index(name = "ix_pb_pallet",        columnList = "pallet_barcode"),
                @Index(name = "ix_pb_host",          columnList = "host_order_key"),
                @Index(name = "ix_pb_barcode",       columnList = "box_barcode"),
                @Index(name = "ix_pb_remaining",     columnList = "pallet_barcode,box_status,remaining_qty"),
                @Index(name = "ix_pb_outbound",      columnList = "outbound_order_key")
        })
public class TbWcsPalletBox extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "eq_group_id", length = 64)
    private String eqGroupId;

    @Column(name = "host_order_key", nullable = false, length = 64)
    private String hostOrderKey;

    @Column(name = "pallet_barcode", nullable = false, length = 100)
    private String palletBarcode;

    @Column(name = "box_seq")
    private Integer boxSeq;

    @Column(name = "box_barcode", length = 100)
    private String boxBarcode;

    @Column(name = "item_code", length = 64)
    private String itemCode;

    @Column(name = "lot_no", length = 40)
    private String lotNo;

    /** 입고 후 불변. 라벨 인쇄용 */
    @Column(name = "total_qty", nullable = false)
    private Integer totalQty;

    /** 이번 출고로 빠진 양. 출고 확정 시 0으로 리셋 */
    @Column(name = "picked_qty", nullable = false)
    private Integer pickedQty = 0;

    /** 박스 현재 보유량. 출고 확정 시 picked_qty 만큼 차감 */
    @Column(name = "remaining_qty", nullable = false)
    private Integer remainingQty;

    @Column(name = "uom", length = 10)
    private String uom;

    /** 0=PENDING / 1=PRINTED / 2=SCANNED / 9=DEPLETED / 99=VOID */
    @Column(name = "box_status")
    private Integer boxStatus;

    @Column(name = "outbound_order_key", length = 100)
    private String outboundOrderKey;

    @Column(name = "picked_at", type = ColumnType.DATETIME)
    private Date pickedAt;

    @Column(name = "test_request_no", length = 50)
    private String testRequestNo;

    @Column(name = "test_no", length = 50)
    private String testNo;

    @Column(name = "produce_date", type = ColumnType.DATETIME)
    private Date produceDate;

    @Column(name = "expiry_date", type = ColumnType.DATETIME)
    private Date expiryDate;

    @Column(name = "printed_at", type = ColumnType.DATETIME)
    private Date printedAt;

    @Column(name = "print_count", nullable = true)
    private Integer printCount = 0;

    @Column(name = "scanned_at", type = ColumnType.DATETIME)
    private Date scannedAt;

    /** 박스 현재 잔량 — remaining_qty, null 이면 0. */
    public int calcRemainingQty() {
        return ValueUtil.isEmpty(remainingQty) ? 0 : remainingQty;
    }

    /** 한 번이라도 담았다가 잔량 0 이 된 소진 박스 여부. */
    public boolean isDepleted() {
        return calcRemainingQty() == 0
                && ValueUtil.isNotEmpty(totalQty) && totalQty > 0;
    }

    /** picked_qty, null 이면 0. */
    public int getPickedQtyOrZero() {
        return ValueUtil.isEmpty(pickedQty) ? 0 : pickedQty;
    }

    /** 이번 출고에서 추가로 잡을 수 있는 수량 = 잔량 - 이미 picked. */
    public int availableToPickQty() {
        return calcRemainingQty() - getPickedQtyOrZero();
    }
}