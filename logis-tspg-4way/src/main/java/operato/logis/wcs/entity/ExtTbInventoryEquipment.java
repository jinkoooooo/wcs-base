package operato.logis.wcs.entity;

import lombok.Getter;
import lombok.Setter;
import operato.logis.inventory.entity.TbInventoryEquipment;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.util.ValueUtil;

/**
 * WCS 설비 확장 엔티티.
 * 재고 모듈 TbInventoryEquipment 를 상속해 WCS 전용 동작만 추가한다.
 */
@Getter
@Setter
@Table(name = "tb_inventory_equipment")
public class ExtTbInventoryEquipment extends TbInventoryEquipment {

    /** 생성 직전 stamp 세팅 + UUID 직접 발번(부모 PK 리플렉션 미탐지 우회) + 검증. */
    @Override
    public void beforeCreate() {
        this._setDefault_(true, false);

        if (ValueUtil.isEmpty(this.getId())) {
            this.setId(java.util.UUID.randomUUID().toString());
        }

        this.validationCheck(OrmConstants.CUD_FLAG_CREATE);
    }
}
