package operato.logis.inventory.consts;

public enum RetrevalPlanTaskType {
    WAIT(0, "작업 대기"),
    RELOCATION(1, "정렬 진행");

    private final Integer value;
    private final String description;

    RetrevalPlanTaskType(Integer value, String description) {
        this.value = value;
        this.description = description;
    }

    public Integer value() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public String toCode() {
        return String.format("%01d", value);
    }
    public String toCode(Integer length) {
        return String.format("%0"+length+"d", value);
    }

    public static RetrevalPlanTaskType valueOf(Integer value) {
        for (RetrevalPlanTaskType type : RetrevalPlanTaskType.values()) {
            if (type.value().equals(value)) {
                return type;
            }
        }
        return RetrevalPlanTaskType.WAIT;
    }
}