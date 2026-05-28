package operato.logis.inventory.consts;

public enum StockStatus {
    IDLE(0, "사용 가능"),
    INBOUND(1, "입고 중"),
    OUTBOUND(2, "출고 중"),
    RELOCATION(3, "정렬 중"),
    INBOUND_READY(4, "입고 대기"),
    HOST_PENDING(5, "호스트 예약"),
    HOLD(7, "입출고 불가");

    private final Integer value;
    private final String description;

    StockStatus(Integer value, String description) {
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

    public static StockStatus valueOf(Integer value) {
        for (StockStatus type : StockStatus.values()) {
            if (type.value().equals(value)) {
                return type;
            }
        }
        return StockStatus.IDLE;
    }
}