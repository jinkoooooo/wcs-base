package operato.logis.changwon.consts;

public enum OrderKind {
    UNKNOWN("0"),
    INBOUND("1"),
    OUTBOUND("2"),
    TRANSFER("3"),
    CANCEL("4"),
    FORCE_INBOUND("8"),
    SORT("9");

    private final String value;

    OrderKind(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
