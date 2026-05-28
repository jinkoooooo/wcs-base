package operato.logis.asrs.enums;

/**
 * ASRS 로케이션 Side 코드.
 *
 * <p>
 * Aisle 기준 좌/우 저장면을 의미하며,
 * 공통 위치키는 Aisle / Side / Bay / Level / Depth 기준으로 관리한다.
 * </p>
 */
public enum AcLocationSide {

    /** Left : Aisle 기준 좌측 면 */
    L,

    /** Right : Aisle 기준 우측 면 */
    R;

    /**
     * 문자열을 Side Enum 으로 변환한다.
     *
     * @param value 입력 문자열
     * @return AcLocationSide
     */
    public static AcLocationSide from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Location side is empty.");
        }

        for (AcLocationSide side : values()) {
            if (side.name().equalsIgnoreCase(value.trim())) {
                return side;
            }
        }

        throw new IllegalArgumentException("Invalid location side: " + value);
    }
}