package operato.logis.asrs.enums;

/**
 * 로케이션 타입 코드.
 *
 * <p>
 * 개별 로케이션 / 로케이션 프로필의 물리/운영상 성격을 구분한다.
 * </p>
 */
public enum AcLocationType {

    NORMAL("NORMAL", "일반 보관 로케이션"),
    BUFFER("BUFFER", "버퍼 로케이션"),
    INBOUND("INBOUND", "입고 전용 로케이션"),
    OUTBOUND("OUTBOUND", "출고 전용 로케이션"),
    PICK("PICK", "피킹 전용 로케이션"),
    RETURN("RETURN", "반품 전용 로케이션"),
    QC("QC", "검수 전용 로케이션"),
    HOLD("HOLD", "보류 전용 로케이션"),
    DAMAGE("DAMAGE", "파손/불량 전용 로케이션"),
    DISPOSAL("DISPOSAL", "폐기 전용 로케이션"),
    VIRTUAL("VIRTUAL", "가상/논리 로케이션"),
    TEMP("TEMP", "임시 로케이션");

    private final String code;
    private final String description;

    AcLocationType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static AcLocationType fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Location type is empty.");
        }

        for (AcLocationType type : values()) {
            if (type.code.equalsIgnoreCase(code.trim())) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unsupported location type code: " + code);
    }
}