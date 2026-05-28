package operato.logis.asrs.enums;

/**
 * 전략 기반 재배치 Task 유형.
 */
public enum StrategyTaskType {

    /** 고등급 상품을 전면권으로 당기는 작업 */
    FORWARD_RELOCATION("FORWARD_RELOCATION", "전면 이동"),

    /** 저등급 상품을 후면권으로 미는 작업 */
    BACKWARD_RELOCATION("BACKWARD_RELOCATION", "후면 이동"),

    /** 명일 수요/출고 대비 선배치 */
    PREPOSITION("PREPOSITION", "선배치");

    private final String code;
    private final String description;

    StrategyTaskType(String code, String description) {
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