package operato.logis.changwon.consts;

public enum DataTransmitStatus {
    NEW(0),
    SENT(1),
    COMPLETED(2),
    NG(9);

    private final Integer value;

    DataTransmitStatus(Integer value) {
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

    public static DataTransmitStatus valueOf(Integer value) {
        for (DataTransmitStatus type : DataTransmitStatus.values()) {
            if (type.value().equals(value)) {
                return type;
            }
        }
        return DataTransmitStatus.NEW;
    }
}