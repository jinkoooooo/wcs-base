package operato.logis.changwon.consts;

public enum ResultType {
    TASK_COMPLETE("B1"),
    ERROR_OCCURRENCE("B2"),
    ERROR_CLEAR("B3"),
    TASK_CANCEL("B4"),
    CHANGE_DESTINATION("A1"),
    REMOVE_STOCK("A2"),
    MANUAL_CANCEL("A3");

    private final String value;

    ResultType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
