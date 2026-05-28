package operato.logis.asrs.enums;

/**
 * 재고 보관 타입 공통 코드.
 *
 * <p>
 * tb_ac_stock_unit.stock_unit_type 에 저장되는 재고 유형을 표준화한다.
 * </p>
 */
public enum AcStockUnitType {

    /** 파렛트 단위 */
    PALLET,

    /** 토트 박스 */
    TOTE_BOX,

    /** 일반 박스 */
    BOX,

    /** 번들/묶음 */
    BUNDLE,

    /** 개별 단품 */
    EACH,

    /** 기타 */
    ETC;

    public static AcStockUnitType from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Stock unit type is empty.");
        }

        for (AcStockUnitType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }

        throw new IllegalArgumentException("Invalid stock unit type: " + value);
    }

    public static AcStockUnitType fromOrDefault(String value, AcStockUnitType defaultType) {
        if (value == null || value.isBlank()) {
            return defaultType;
        }
        return from(value);
    }
}