package operato.logis.asrs.enums;

/**
 * 보관 영역 타입 코드.
 *
 * <p>
 * 창고/영역의 기능적 성격을 구분한다.
 * </p>
 */
public enum AcAreaType {

    ASRS("ASRS", "자동창고 메인 보관영역"),
    BUFFER("BUFFER", "버퍼영역"),
    STAGING("STAGING", "입출고 대기영역"),
    INBOUND("INBOUND", "입고 전용영역"),
    OUTBOUND("OUTBOUND", "출고 전용영역"),
    PICKING("PICKING", "피킹영역"),
    RETURN("RETURN", "반품영역"),
    QC("QC", "검수영역"),
    HOLD("HOLD", "보류영역"),
    DAMAGE("DAMAGE", "파손/불량영역"),
    DISPOSAL("DISPOSAL", "폐기영역"),
    MANUAL("MANUAL", "수동 보관/작업영역");

    private final String code;
    private final String description;

    AcAreaType(String code, String description) {
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
    public static AcAreaType fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Area type is empty.");
        }

        for (AcAreaType type : values()) {
            if (type.code.equalsIgnoreCase(code.trim())) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unsupported area type code: " + code);
    }
}