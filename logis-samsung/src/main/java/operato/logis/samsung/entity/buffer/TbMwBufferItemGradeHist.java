package operato.logis.samsung.entity.buffer;

import lombok.Getter;
import lombok.Setter;
import operato.logis.samsung.entity.buffer.relation.TbMwItemMasterRef;
import xyz.elidom.dbist.annotation.*;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.orm.entity.basic.DomainCreateStampHook;

/**
 * 재고관리 - 시퀀스버퍼 SKU별 등급 이력 관리
 */
@Getter
@Setter
@Table(name = "tb_mw_buffer_item_grade_hist", idStrategy = GenerationRule.UUID)
public class TbMwBufferItemGradeHist extends DomainCreateStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = OrmConstants.FIELD_SIZE_UUID)
    private String id;

    @Column(name = "item_id", length = OrmConstants.FIELD_SIZE_UUID)
    private String itemId; // FK: 상품마스터 (tb_mw_item_master.id)

    @Relation(field = "itemId")
    private TbMwItemMasterRef item;

    @Column(name = "item_code", nullable = false, length = 100)
    private String itemCode; // 상품코드 (tb_mw_inbound_delivery의 item_code와 동일)

    @Column(name = "inner_item_code", nullable = false, length = 100)
    private String innerItemCode; // 88바코드 (tb_mw_inbound_delivery의 inner_item_code와 동일)

    @Column(name = "before_grade")
    private Integer beforeGrade; // 변경 전 SKU 등급

    @Column(name = "after_grade", nullable = false)
    private Integer afterGrade; // 변경 후 SKU 등급

    @Column(name = "before_inbound_qty")
    private Integer beforeInboundQty; // 변경 전 입고 예정 수량

    @Column(name = "after_inbound_qty", nullable = false)
    private Integer afterInboundQty; // 변경 후 입고 예정 수량
}