package operato.logis.changwon.consts;

public enum ProcessStatus {
    UNKNOWN(0),
    WAITING_FOR_PREV_TASK(30),
    TASK_READY(31),
    TASK_STARTING(32),
    TASK_COMPLETE(33),
    TASK_ERROR(39);

    private final Integer value;

    ProcessStatus(Integer value) {
        this.value = value;
    }

    public Integer value() {
        return value;
    }

    public String toCode() {
        return String.format("%01d", value);
    }
    public String toCode(Integer length) {
        return String.format("%0"+length+"d", value);
    }

    public static ProcessStatus valueOf(Integer value) {
        for (ProcessStatus type : ProcessStatus.values()) {
            if (type.value().equals(value)) {
                return type;
            }
        }
        return ProcessStatus.UNKNOWN;
    }
}