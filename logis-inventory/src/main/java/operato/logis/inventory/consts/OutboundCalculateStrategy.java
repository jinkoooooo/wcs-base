package operato.logis.inventory.consts;

public enum OutboundCalculateStrategy {
    MIN_MOVEMENT(0, "Minimum Movement"),
    MIN_PALLET(1, "Minimum Pallet"),
    FIFO(2, "First-In, First-Out"),
    FEFO(3, "First-Expired, First-Out"),
    LIFO(4, "Last-In, First-Out"),
    EXACT_MATCH(5, "No Picking & Repackaging");

    private final Integer value;
    private final String description;

    OutboundCalculateStrategy(Integer value, String description) {
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

    public static OutboundCalculateStrategy valueOf(Integer value) {
        for (OutboundCalculateStrategy type : OutboundCalculateStrategy.values()) {
            if (type.value().equals(value)) {
                return type;
            }
        }
        return OutboundCalculateStrategy.MIN_MOVEMENT;
    }
}