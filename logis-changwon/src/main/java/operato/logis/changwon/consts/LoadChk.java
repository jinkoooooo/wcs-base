package operato.logis.changwon.consts;

public enum LoadChk {
    NOT_LOAD(0),
    ONLY_STOCK(1),
    ONLY_RUNNER(2),
    STOCK_AND_RUNNER(3);

    private final Integer value;

    LoadChk(Integer value) {
        this.value = value;
    }

    public Integer value() {
        return value;
    }
}
