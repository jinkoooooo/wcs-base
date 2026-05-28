package operato.logis.changwon.consts;

public enum RackLocked {
    IDLE(0),
    PATH_RESERVED(1),
    INBOUND_RESERVED(2),
    OUTBOUND_RESERVED(3);

    private final Integer value;

    RackLocked(Integer value) {
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

    public static RackLocked valueOf(Integer value) {
        for (RackLocked type : RackLocked.values()) {
            if (type.value().equals(value)) {
                return type;
            }
        }
        return RackLocked.IDLE;
    }
}