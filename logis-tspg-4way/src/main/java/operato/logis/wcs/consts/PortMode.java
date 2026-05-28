package operato.logis.wcs.consts;

/**
 * 포트 운영 모드. 입/출고 가능 여부와 전환 대기(draining) 상태를 표현한다.
 */
public enum PortMode implements EnumCode {

    IDLE("IDLE",
            "유휴 - 입/출고 모두 가능한 대기 상태"),
    INBOUND("INBOUND",
            "입고 모드 - 현재 입고 작업 중 (출고 할당 차단)"),
    OUTBOUND("OUTBOUND",
            "출고 모드 - 현재 출고 작업 대기/진행 중"),
    OUTBOUND_PRIORITY("OUTBOUND_PRIORITY",
            "출고 우선 모드 - 기아 방지로 인한 신규 입고 차단, 현재 입고 완료 후 강제 출고 전환"),

    SWITCHING_TO_OUTBOUND("SWITCHING_TO_OUTBOUND",
            "출고 전환 대기 - 진행중 입고 작업 완료 시 자동으로 OUTBOUND 로 전환"),

    SWITCHING_TO_INBOUND("SWITCHING_TO_INBOUND",
            "입고 전환 대기 - 진행중 출고/재입고 작업 완료 시 자동으로 INBOUND 로 전환");

    private final String code;
    private final String desc;

    PortMode(String code, String desc) {
        this.code = code; this.desc = desc;
    }

    @Override public String code() { return code; }
    @Override public String desc() { return desc; }

    /** 전환 대기(진행 작업 소진 중) 상태인지. */
    public boolean isDraining() {
        return this == SWITCHING_TO_INBOUND || this == SWITCHING_TO_OUTBOUND;
    }

    /** 전환 대기 상태가 완료 후 도달할 목표 모드. 비전환 상태면 null. */
    public PortMode pendingTarget() {
        return switch (this) {
            case SWITCHING_TO_INBOUND  -> INBOUND;
            case SWITCHING_TO_OUTBOUND -> OUTBOUND;
            default -> null;
        };
    }

    /** 전환 대기 중 소진해야 할 직전 모드. 비전환 상태면 자기 자신. */
    public PortMode drainingFrom() {
        return switch (this) {
            case SWITCHING_TO_INBOUND  -> OUTBOUND;
            case SWITCHING_TO_OUTBOUND -> INBOUND;
            default -> this;
        };
    }

    /** 코드/별칭으로 enum 해석. 미일치 시 null. */
    public static PortMode from(Object code) {
        return EnumCodeUtil.fromCodeOrNull(PortMode.class, code);
    }
}
