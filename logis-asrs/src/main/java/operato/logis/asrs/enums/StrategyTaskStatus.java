package operato.logis.asrs.enums;

/**
 * 전략 Task 상태 코드.
 */
public enum StrategyTaskStatus {

    CREATED("CREATED", "생성"),
    READY("READY", "실행대기"),
    IN_PROGRESS("IN_PROGRESS", "진행중"),
    DONE("DONE", "완료"),
    CANCELED("CANCELED", "취소");

    private final String code;
    private final String description;

    StrategyTaskStatus(String code, String description) {
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