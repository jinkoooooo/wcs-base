package operato.logis.asrs.enums;

/**
 * 로케이션 운영 상태 코드.
 *
 * <p>
 * 실시간 점유 여부가 아니라 운영 허용 / 차단 상태 성격으로 사용한다.
 * </p>
 */
public enum AcLocationUsageStatus {

    ENABLED("ENABLED", "사용 가능"),
    DISABLED("DISABLED", "사용 중지"),
    BLOCKED("BLOCKED", "운영 차단"),
    MAINTENANCE("MAINTENANCE", "점검중"),
    HOLD("HOLD", "운영 보류");

    private final String code;
    private final String description;

    AcLocationUsageStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static AcLocationUsageStatus fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Usage status code is empty.");
        }

        for (AcLocationUsageStatus status : values()) {
            if (status.code.equalsIgnoreCase(code.trim())) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unsupported usage status code: " + code);
    }
}