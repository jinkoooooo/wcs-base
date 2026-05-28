package operato.logis.asrs.enums;

/**
 * Access Point 운영 목적 코드.
 *
 * <p>
 * 하나의 Access Point는 복수 목적을 가질 수 있으며,
 * 로케이션 접근성 재산출 시 특정 목적 기준으로 Access Point를 선택한다.
 * </p>
 */
public enum AccessPurposeType {

    /** 입고 기준 */
    INBOUND("INBOUND", "입고"),

    /** 출고 기준 */
    OUTBOUND("OUTBOUND", "출고"),

    /** 피킹 기준 */
    PICK("PICK", "피킹"),

    /** 재배치 기준 */
    RELOCATION("RELOCATION", "재배치");

    /** DB 저장 코드 */
    private final String code;

    /** 코드 설명 */
    private final String description;

    AccessPurposeType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /** DB 저장 코드 반환 */
    public String getCode() {
        return code;
    }

    /** 코드 설명 반환 */
    public String getDescription() {
        return description;
    }

    /**
     * 문자열 코드로 enum 조회.
     *
     * @param code 목적 코드
     * @return AccessPurposeType
     */
    public static AccessPurposeType fromCode(String code) {
        for (AccessPurposeType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported AccessPurposeType code: " + code);
    }
}