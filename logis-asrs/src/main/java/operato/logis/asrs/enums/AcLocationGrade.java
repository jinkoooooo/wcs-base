package operato.logis.asrs.enums;

/**
 * 로케이션 등급 코드.
 */
public enum AcLocationGrade {

    A("A", "최우선 접근"),
    B("B", "중상위 접근"),
    C("C", "중하위 접근"),
    D("D", "저우선 접근");

    private final String code;
    private final String description;

    AcLocationGrade(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static AcLocationGrade fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Location grade is empty.");
        }

        for (AcLocationGrade grade : values()) {
            if (grade.code.equalsIgnoreCase(code.trim())) {
                return grade;
            }
        }

        throw new IllegalArgumentException("Unsupported location grade code: " + code);
    }
}