package operato.logis.asrs.enums;

/**
 * 센터 유형 코드.
 *
 * <p>
 * 센터의 물류/설비 운영 성격을 구분한다.
 * </p>
 */
public enum AcCenterType {

    ASRS("ASRS", "자동창고 센터"),
    WAREHOUSE("WAREHOUSE", "일반 창고"),
    DISTRIBUTION("DISTRIBUTION", "물류센터"),
    FACTORY_WAREHOUSE("FACTORY_WAREHOUSE", "공장 창고"),
    RETURN_CENTER("RETURN_CENTER", "반품 센터"),
    MANUAL("MANUAL", "수동 운영 센터");

    private final String code;
    private final String description;

    AcCenterType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * DB 저장 코드 반환.
     */
    public String getCode() {
        return code;
    }

    /**
     * 화면 표시용 설명 반환.
     */
    public String getDescription() {
        return description;
    }

    /**
     * 문자열 코드로 enum 조회.
     */
    public static AcCenterType fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Center type is empty.");
        }

        for (AcCenterType type : values()) {
            if (type.code.equalsIgnoreCase(code.trim())) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unsupported center type code: " + code);
    }
}