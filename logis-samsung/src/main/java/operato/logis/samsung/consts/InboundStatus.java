package operato.logis.samsung.consts;

public enum InboundStatus {
    READY(0),
    RUNNING(1),
    COMPLETE(2),
    PAUSED(8),
    ABORTED(9);

    private final Integer value;

    InboundStatus(Integer value) {
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

    public static InboundStatus valueOf(Integer value) {
        for (InboundStatus type : InboundStatus.values()) {
            if (type.value().equals(value)) {
                return type;
            }
        }
        return InboundStatus.READY;
    }
}