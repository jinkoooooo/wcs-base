package operato.logis.samsung.consts;

public enum JobStatus {
    READY(0),
    RUNNING(1),
    DONE(2),
    PAUSED(8),
    ABORTED(9);

    private final Integer value;

    JobStatus(Integer value) {
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

    public static JobStatus valueOf(Integer value) {
        for (JobStatus type : JobStatus.values()) {
            if (type.value().equals(value)) {
                return type;
            }
        }
        return JobStatus.READY;
    }
}