package operato.logis.wcs.entity;

import lombok.Getter;
import lombok.Setter;
import operato.logis.inventory.entity.TbInventoryStock;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.ColumnType;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Index;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.util.ValueUtil;

import java.util.Date;

/**
 * WCS 통합 재고 확장 엔티티.
 * 재고 모듈 TbInventoryStock 을 상속해 WCS 전용 필드만 추가한다.
 *
 * Elidom 의 _setId_() 가 부모의 @PrimaryKey id 필드를 리플렉션으로 못 찾아 NPE 가 나므로,
 * beforeCreate() 에서 UUID 를 직접 생성한 뒤 나머지 stamp 처리를 수행한다.
 */
@Getter
@Setter
@Table(name = "tb_inventory_stock", idStrategy = GenerationRule.UUID,
        indexes = {
                @Index(name = "ix_inv_stock_type_enabled", columnList = "stock_type, is_enabled"),
                @Index(name = "ix_inv_stock_item_code",    columnList = "item_code"),
                @Index(name = "ix_inv_stock_inb_dt",       columnList = "inb_datetime"),
                @Index(name = "ix_inv_stock_lot_no",       columnList = "lot_no"),
                @Index(name = "ix_inv_stock_eq_group",     columnList = "eq_group_id")
        })
public class ExtTbInventoryStock extends TbInventoryStock {

    /** 생산일자 - 입고 등록 시 전달받거나, 로트/배치 추적용으로 저장 */
    @Column(name = "produce_date", type = ColumnType.DATETIME)
    private Date produceDate;

    /** 재고 카테고리 (NORMAL/QC_PENDING/QC_FAIL/NIA_PENDING/RETURN/DISPOSAL) */
    @Column(name = "stock_type", length = 20)
    private String stockType;

    /** 설비 그룹 ID (셔틀 구역 단위) */
    @Column(name = "eq_group_id", length = 20)
    private String eqGroupId;

    @Column(name = "origin_host_order_key", length = 64)
    private String originHostOrderKey;

    /** 시험의뢰번호 */
    @Column(name = "test_request_no", length = 50)
    private String testRequestNo;

    /** 시험번호 */
    @Column(name = "test_no", length = 50)
    private String testNo;

    /** 생성 직전 stamp 세팅 + UUID 직접 발번(부모 PK 리플렉션 미탐지 우회) + 검증. */
    @Override
    public void beforeCreate() {
        // stamp 필드 세팅 (domainId, createdAt, updatedAt, creatorId, updaterId)
        this._setDefault_(true, false);

        // UUID 직접 생성 (_setId_ 대체)
        if (ValueUtil.isEmpty(this.getId())) {
            this.setId(java.util.UUID.randomUUID().toString());
        }

        // 유효성 검사
        this.validationCheck(OrmConstants.CUD_FLAG_CREATE);
    }
}
