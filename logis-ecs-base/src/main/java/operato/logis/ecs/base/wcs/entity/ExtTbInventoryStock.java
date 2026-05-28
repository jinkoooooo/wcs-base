package operato.logis.ecs.base.wcs.entity;

import lombok.Getter;
import lombok.Setter;
import operato.logis.inventory.entity.TbInventoryStock;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.ColumnType;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.util.ValueUtil;

import java.util.Date;

/**
 * [WCS 통합 재고 확장 엔티티]
 * - 재고 모듈의 TbInventoryStock을 상속
 * - TbInventoryStock으로 대체 불가한 WCS 전용 필드만 추가
 *
 * [Elidom 상속 이슈]
 * Elidom의 _setId_()는 FieldUtils.getDeclaredField(clazz, "id")로 현재 클래스만 스캔하여
 * 부모(TbInventoryStock)의 @PrimaryKey id 필드를 찾지 못해 NPE 발생.
 * beforeCreate()를 오버라이드하여 UUID 직접 생성 후 나머지 stamp 처리를 수행한다.
 */
@Getter
@Setter
@Table(name = "tb_inventory_stock", idStrategy = GenerationRule.UUID)
public class ExtTbInventoryStock extends TbInventoryStock {

    /** 생산일자 - 입고 등록 시 전달받거나, 로트/배치 추적용으로 저장 */
    @Column(name = "produce_date", type = ColumnType.DATETIME)
    private Date produceDate;

    /** 설비 그룹 ID (셔틀 구역 단위) */
    @Column(name = "eq_group_id", length = 20)
    private String eqGroupId;

    @Override
    public void beforeCreate() {
        // 1. stamp 필드 세팅 (domainId, createdAt, updatedAt, creatorId, updaterId)
        this._setDefault_(true, false);

        // 2. UUID 직접 생성 (_setId_ 대체 — 부모의 @PrimaryKey 필드를 리플렉션으로 찾지 못하는 이슈 우회)
        if (ValueUtil.isEmpty(this.getId())) {
            this.setId(java.util.UUID.randomUUID().toString());
        }

        // 3. 유효성 검사
        this.validationCheck(OrmConstants.CUD_FLAG_CREATE);
    }
}
