package operato.logis.wcs.consts;

/**
 * WCS 운영 모드. 모드별로 허용되는 주문 유형이 달라진다.
 */
public enum WcsOperationMode implements EnumCode {

    NORMAL("NORMAL",                     "정상"),
    INBOUND_PRIORITY("INBOUND_PRIORITY", "입고 우선"),
    OUTBOUND_PRIORITY("OUTBOUND_PRIORITY","출고 우선"),
    RELOCATION("RELOCATION",             "재배치"),
    MAINTENANCE("MAINTENANCE",           "점검");

    private final String code;
    private final String desc;

    WcsOperationMode(String code, String desc) {
        this.code = code; this.desc = desc;
    }

    @Override public String code() { return code; }
    @Override public String desc() { return desc; }

    /** 현재 모드에서 해당 주문 유형 산출을 허용하는지. */
    public boolean allows(OrderType type) {
        return switch (this) {
            case NORMAL            -> true;
            case INBOUND_PRIORITY  -> type == OrderType.INBOUND  || type == OrderType.MOVE;
            case OUTBOUND_PRIORITY -> type == OrderType.OUTBOUND || type == OrderType.MOVE;
            case RELOCATION        -> type == OrderType.MOVE;
            case MAINTENANCE       -> false;
        };
    }

    /** 코드/별칭으로 enum 해석. 미일치 시 null. */
    public static WcsOperationMode from(Object code) {
        return EnumCodeUtil.fromCodeOrNull(WcsOperationMode.class, code);
    }
}
