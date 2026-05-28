package operato.logis.asrs.enums;

/**
 * 로케이션 선택 목적 코드.
 */
public enum LocationSelectionPurposeType {

    /** 입고 적치 */
    INBOUND("INBOUND", "입고"),

    /** 출고 선택 */
    OUTBOUND("OUTBOUND", "출고"),

    /** 피킹 출고 */
    PICK("PICK", "피킹"),

    /** 재배치 */
    RELOCATION("RELOCATION", "재배치");

    private final String code;
    private final String description;

    LocationSelectionPurposeType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}