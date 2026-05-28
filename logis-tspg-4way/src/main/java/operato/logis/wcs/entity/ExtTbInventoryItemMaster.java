package operato.logis.wcs.entity;

import lombok.Getter;
import lombok.Setter;
import operato.logis.inventory.entity.TbInventoryItemMaster;
import operato.logis.wcs.consts.*;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.util.ValueUtil;

import java.util.Date;

/**
 * WCS 자재 마스터 확장 엔티티.
 * 재고 모듈 TbInventoryItemMaster 를 상속해 무게·환산 단위·분류 등 WCS 전용 필드와 EA 환산 로직을 추가한다.
 */
@Getter
@Setter
@Table(name = "tb_inventory_item_mst", idStrategy = GenerationRule.UUID)
public class ExtTbInventoryItemMaster extends TbInventoryItemMaster {

    /** 규격 */
    @Column(name = "item_spec", length = 200)
    private String itemSpec;

    /** 낱개 1개당 무게(kg) — 출고/재고 계산의 기준 단위 */
    @Column(name = "item_weight", length = 10)
    private Double itemWeight;

    /** 기본단위 */
    @Column(name = "item_unit", length = 20)
    private String itemUnit;

    /** 세부규격 */
    @Column(name = "item_spec_detail", length = 500)
    private String itemSpecDetail;

    /** 유효기간(일 수) */
    @Column(name = "expiry_days")
    private Integer expiryDays;

    /** 박스당 낱개 수량(EA/BOX) — 입고 환산용, nullable */
    @Column(name = "box_qty")
    private Integer boxQty;

    /** 팔레트당 박스 수량(BOX/PLT) — 입고 환산용, nullable */
    @Column(name = "pallet_qty")
    private Integer palletQty;

    /** 비고 */
    @Column(name = "remarks", length = 1000)
    private String remarks;

    /** 제조처 */
    @Column(name = "manufacturer", length = 100)
    private String manufacturer;

    /**
     * 자재 분류 — {@link ItemCategory} 의 code 문자열. nullable.
     * 마이그레이션 초기 row 는 빈 값 허용, 빈 값이면 운영자 선택 폴백.
     */
    @Column(name = "item_category", length = 20, nullable = true)
    private String itemCategory;

    /** 상위(MES/ERP) 인수 여부. */
    @Column(name = "fetched", nullable = true)
    private Boolean fetched = false;

    /** soft delete 시각. null=활성. 물리삭제 대신 tombstone(삭제 시 fetched=false 동반 → 상위 pull 재동기화). */
    @Column(name = "deleted_at", nullable = true)
    private Date deletedAt;

    /** 생성 직전 stamp 세팅 + UUID 발번 + 미인수 초기화 + 분류 검증. */
    @Override
    public void beforeCreate() {
        this._setDefault_(true, false);

        if (ValueUtil.isEmpty(this.getId())) {
            this.setId(java.util.UUID.randomUUID().toString());
        }

        // 신규 마스터는 항상 상위 미인수 상태로 진입
        this.fetched = Boolean.FALSE;

        this.validateItemCategory();
        this.validationCheck(OrmConstants.CUD_FLAG_CREATE);
    }

    /** 수정 시 상위 인수 표시 무효화(재pull 유도) + 분류 검증. */
    @Override
    public void beforeUpdate() {
        super.beforeUpdate();
        // 마스터 변경 시 fetched 를 무효화. 재인수는 auto-ack 또는 markFetched 경로로만 처리
        this.fetched = Boolean.FALSE;
        this.validateItemCategory();
    }

    /** item_category 가 채워졌으면 알려진 ItemCategory code 인지 검증. */
    private void validateItemCategory() {
        if (ValueUtil.isNotEmpty(this.itemCategory) && ItemCategory.from(this.itemCategory) == null) {
            throw new IllegalArgumentException(
                    "Unknown item_category: %s (allowed: RAW_MATERIAL/MATERIAL/SEMI_PRODUCT/PRODUCT/OWN_PRODUCT)"
                            .formatted(this.itemCategory));
        }
    }

    /** 주어진 UOM/수량을 EA(낱개)로 환산. null UOM 은 EA 로 간주. */
    public int toEaQty(UomType uom, int qty) {
        if (qty <= 0) return 0;
        UomType u = ValueUtil.isEmpty(uom) ? UomType.EA : uom;

        return switch (u) {
            case EA  -> qty;
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
        return toEaQty(UomType.fromOrDefault(uomCode), qty);
    }

    /** EA 수량의 총 무게 */
    public double calculateWeightByEa(int eaQty) {
        if (ValueUtil.isEmpty(itemWeight) || eaQty <= 0) return 0.0;
        return itemWeight * eaQty;
    }

    /** UOM/수량을 받아 바로 무게 계산 */
    public double calculateWeight(UomType uom, int qty) {
        return calculateWeightByEa(toEaQty(uom, qty));
    }

    private void requirePositive(Integer v, String field) {
        if (ValueUtil.isEmpty(v) || v <= 0) {
            throw new IllegalStateException(
                    String.format("%s 미등록 또는 0 이하 - itemOwner=%s, itemCode=%s",
                            field, getItemOwner(), getItemCode()));
        }
    }
}