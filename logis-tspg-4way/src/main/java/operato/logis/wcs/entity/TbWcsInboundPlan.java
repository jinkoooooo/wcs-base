package operato.logis.wcs.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.*;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

import java.util.Date;

/**
 * 입고 예정 마스터 — (입고예정일, SKU, LOT) 단위 1건.
 *
 * 관리자가 미리 등록하고, 입고 주문(host_order)은 이 예정을 끌어와 발행한다.
 * 입고 주문 발행 시 planned_qty 한도 내에서 ordered_qty 를 가산하고,
 * host_order.parent_host_order_key 에 이 예정 id 를 기록해 연계한다.
 */
@Getter
@Setter
@Table(name = "tb_wcs_inbound_plan", idStrategy = GenerationRule.UUID,
        indexes = {
                @Index(name = "ix_tb_wcs_inbound_plan_date_sku", columnList = "plan_date,item_code,lot_no")
        })
public class TbWcsInboundPlan extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    /** 입고 예정일 — 미지정 등록 시 오늘. */
    @JsonProperty("plan_date")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    @Column(name = "plan_date", nullable = false, type = ColumnType.DATETIME)
    private Date planDate;

    /** 품목코드(SKU). */
    @Column(name = "item_code", nullable = false, length = 100)
    private String itemCode;

    /** 로트번호 — 미부여는 빈 문자열로 통일. */
    @Column(name = "lot_no", nullable = false, length = 100)
    private String lotNo;

    /** 화주 코드. */
    @Column(name = "item_owner", length = 50)
    private String itemOwner;

    /** 입고 예정 수량 — 등록 시 고정. */
    @Column(name = "planned_qty", nullable = false)
    private Integer plannedQty;

    /** 누적 입고 주문 수량 — 입고 주문 발행마다 가산(기본 0). */
    @Column(name = "ordered_qty", nullable = false)
    private Integer orderedQty;

    /** 입고 단위. */
    @Column(name = "uom", length = 20)
    private String uom;

    /** 제조일자. */
    @JsonProperty("produce_date")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    @Column(name = "produce_date", type = ColumnType.DATETIME)
    private Date produceDate;

    /** 사용기한. */
    @JsonProperty("expiry_date")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    @Column(name = "expiry_date", type = ColumnType.DATETIME)
    private Date expiryDate;

    /** QC 시험 대상 여부 — true 면 등록 시 qc_test_request 자동 생성. */
    @Column(name = "test_required")
    private Boolean testRequired;

    /** 국검(NIA) 대상 여부. */
    @Column(name = "nia_required")
    private Boolean niaRequired;
}
