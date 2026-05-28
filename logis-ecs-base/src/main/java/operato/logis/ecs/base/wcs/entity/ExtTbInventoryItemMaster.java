package operato.logis.ecs.base.wcs.entity;

import lombok.Getter;
import lombok.Setter;
import operato.logis.inventory.entity.TbInventoryItemMaster;
import operato.logis.ecs.base.wcs.consts.WcsDomainEnums;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.util.ValueUtil;

@Getter
@Setter
@Table(name = "tb_inventory_item_mst", idStrategy = GenerationRule.UUID)
public class ExtTbInventoryItemMaster extends TbInventoryItemMaster {

    /** 규격 */
    @Column(name = "item_spec", length = 200)
    private String itemSpec;

    /** 낱개 1개당 무게 (kg) — 출고/재고 계산의 기준 단위 */
    @Column(name = "item_weight", length = 10)
    private Double itemWeight;

    /** 기본단위 */
    @Column(name = "item_unit", length = 20)
    private String itemUnit;

    /** 세부규격 */
    @Column(name = "item_spec_detail", length = 500)
    private String itemSpecDetail;

    /** 유효기간 (일 수) */
    @Column(name = "expiry_days")
    private Integer expiryDays;

    /** 박스당 낱개 수량 (EA/BOX) - 입고 시 환산용, nullable */
    @Column(name = "box_qty")
    private Integer boxQty;

    /** 팔레트당 박스 수량 (BOX/PLT) - 입고 시 환산용, nullable */
    @Column(name = "pallet_qty")
    private Integer palletQty;

    /** 비고 */
    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Override
    public void beforeCreate() {
        this._setDefault_(true, false);

        if (ValueUtil.isEmpty(this.getId())) {
            this.setId(java.util.UUID.randomUUID().toString());
        }

        this.validationCheck(OrmConstants.CUD_FLAG_CREATE);
    }

    /**
     * 주어진 UOM/수량을 EA(낱개)로 환산
     *
     * @param uom UomType (null 이면 EA)
     * @param qty UOM 기준 수량
     * @return EA 환산 수량
     */
    public int toEaQty(WcsDomainEnums.UomType uom, int qty) {
        if (qty <= 0) return 0;
        WcsDomainEnums.UomType u = (uom == null) ? WcsDomainEnums.UomType.EA : uom;

        return switch (u) {
            case EA -> qty;
            case BOX -> {
                requirePositive(boxQty, "box_qty");
                yield qty * boxQty;
            }
            case PLT -> {
                requirePositive(boxQty, "box_qty");
                requirePositive(palletQty, "pallet_qty");
                yield qty * palletQty * boxQty;
            }
        };
    }

    /** 문자열 UOM 오버로드 — enum 변환 후 위임 */
    public int toEaQty(String uomCode, int qty) {
        return toEaQty(WcsDomainEnums.UomType.fromOrDefault(uomCode), qty);
    }

    /** EA 수량의 총 무게 */
    public double calculateWeightByEa(int eaQty) {
        if (itemWeight == null || eaQty <= 0) return 0.0;
        return itemWeight * eaQty;
    }

    /** UOM/수량을 받아 바로 무게 계산 */
    public double calculateWeight(WcsDomainEnums.UomType uom, int qty) {
        return calculateWeightByEa(toEaQty(uom, qty));
    }

    private void requirePositive(Integer v, String field) {
        if (v == null || v <= 0) {
            throw new IllegalStateException(
                    String.format("%s 미등록 또는 0 이하 - itemOwner=%s, itemCode=%s",
                            field, getItemOwner(), getItemCode()));
        }
    }
}