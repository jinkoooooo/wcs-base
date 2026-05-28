package xyz.elidom.dev.consts;

public enum ScheduleType {
    CRON("CRON"),
    FIXED_RATE("FIXED_RATE"),
    FIXED_DELAY("FIXED_DELAY"),
    INITIAL_DELAY("INITIAL_DELAY");

    private final String value;

    ScheduleType(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }
}
