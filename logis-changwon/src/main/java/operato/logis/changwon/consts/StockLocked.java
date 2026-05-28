package operato.logis.changwon.consts;

public enum StockLocked {
    IDLE(0),
    SORT_IN_PROGRESS(1),
    INBOUND_IN_PROGRESS(2),
    OUTBOUND_IN_PROGRESS(3);

    private final Integer value;

    StockLocked(Integer value) {
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

    public static StockLocked valueOf(Integer value) {
        for (StockLocked type : StockLocked.values()) {
            if (type.value().equals(value)) {
                return type;
            }
        }
        return StockLocked.IDLE;
    }
}