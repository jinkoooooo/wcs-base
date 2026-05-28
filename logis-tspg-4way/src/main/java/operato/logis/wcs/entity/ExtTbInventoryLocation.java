package operato.logis.wcs.entity;

import lombok.Getter;
import lombok.Setter;
import operato.logis.inventory.entity.TbInventoryLocation;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Index;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.util.ValueUtil;

/**
 * WCS 로케이션 확장 엔티티.
 * 재고 모듈 TbInventoryLocation 을 상속해 WCS 전용 필드만 추가한다.
 *
 * locGroup 은 TbEQ 의 EqGroupId 와 매칭, locId 는 TbEqRackMst 와 동일한 10601 형식을 사용한다.
 *
 * Elidom 상속 이슈로 beforeCreate() 를 오버라이드해 _setId_() NPE 를 우회한다.
 */
@Getter
@Setter
@Table(name = "tb_inventory_location", idStrategy = GenerationRule.UUID,
        indexes = {
                @Index(name = "ix_inv_loc_stock_group", columnList = "stock_id, loc_group"),
                @Index(name = "ix_inv_loc_type",        columnList = "loc_type")
        })
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

    /** 이 포트를 향해 할당/이동 중인 셔틀 오더 수 (로드밸런싱 기준) */
    @Column(name = "active_task_count")
    private Integer activeTaskCount;

    /** 적재 화물의 물리 바코드 값 (BCR 스캔 결과) */
    @Column(name = "barcode", length = 100)
    private String barcode;

    /**
     * 격리 발생 원인 오더 키 (DOUBLE_IN/EMPTY_OUT 시 저장).
     * task_id 가 마커로 덮어써지므로 원인 오더를 별도 보존.
     */
    @Column(name = "block_reason_order_key", length = 40)
    private String blockReasonOrderKey;

    /**
     * 격리 발생 시점의 stockId 스냅샷.
     * 이후 ECS 재지정 등으로 stock_id 가 변경되어도 발생 당시 값은 보존된다.
     */
    @Column(name = "block_snapshot_stock_id", length = 100)
    private String blockSnapshotStockId;

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
