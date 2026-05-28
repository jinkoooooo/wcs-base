package operato.logis.ecs.base.wcs.entity;

import lombok.Getter;
import lombok.Setter;
import operato.logis.inventory.entity.TbInventoryLocation;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.util.ValueUtil;

/**
 * [WCS 로케이션 확장 엔티티]
 * - 재고 모듈의 TbInventoryLocation을 상속
 * - TbInventoryLocation으로 대체 불가한 WCS 전용 필드만 추가
 *
 * - locGroup 내에 TbEQ에서 사용하는 EqGroupId 와 매칭시킨다.
 * - LocId에는 TbEqRackMst 와 똑같은 10601 같은 형식을 사용한다.
 *
 * [Elidom 상속 이슈]
 * beforeCreate()를 오버라이드하여 _setId_() NPE를 우회한다.
 */
@Getter
@Setter
@Table(name = "tb_inventory_location", idStrategy = GenerationRule.UUID)
public class ExtTbInventoryLocation extends TbInventoryLocation {

    /** 물리 랙 장비 ID (tb_eq_rack_mst.eq_id 매핑용) */
    @Column(name = "rack_eq_id", length = 50)
    private String rackEqId;

    /** 로케이션 타입 (RACK / INBOUND_PORT / OUTBOUND_PORT / IN_OUTBOUND_PORT 등) */
    @Column(name = "loc_type", length = 16)
    private String locType;

    /** 포트 운영 모드 (겸용 포트 전용) */
    @Column(name = "port_mode", length = 20)
    private String portMode;

    /** 현재 이 포트를 향해 할당/이동 중인 셔틀 오더 수 (로드밸런싱 기준) */
    @Column(name = "active_task_count")
    private Integer activeTaskCount;

    /** 해당 로케이션에 적재된 화물의 물리적 바코드 값 (BCR 스캔 결과) */
    @Column(name = "barcode", length = 100)
    private String barcode;

    /**
     * 격리 발생 원인 오더 키 (DOUBLE_IN/EMPTY_OUT 시 저장).
     * task_id 가 "DOUBLE_IN"/"EMPTY_OUT" 마커로 덮어써지므로 원인 오더를 별도 보존.
     */
    @Column(name = "block_reason_order_key", length = 40)
    private String blockReasonOrderKey;

    /**
     * 격리 발생 시점의 stockId 스냅샷.
     * 이후 ECS 재지정 등으로 stock_id 컬럼이 변경되어도 발생 당시 값은 여기에 보존됨.
     */
    @Column(name = "block_snapshot_stock_id", length = 100)
    private String blockSnapshotStockId;

    @Override
    public void beforeCreate() {
        this._setDefault_(true, false);

        if (ValueUtil.isEmpty(this.getId())) {
            this.setId(java.util.UUID.randomUUID().toString());
        }

        this.validationCheck(OrmConstants.CUD_FLAG_CREATE);
    }
}
