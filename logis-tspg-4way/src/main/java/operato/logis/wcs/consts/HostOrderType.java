package operato.logis.wcs.consts;

/**
 * HOST 주문 유형. base OrderType(입고/출고/이동) 과 sub type(반품/폐기/샘플) 으로 분해된다.
 */
public enum HostOrderType implements EnumCode {

    INBOUND     ("INBOUND",      OrderType.INBOUND,  null,                       "일반 입고"),
    OUTBOUND    ("OUTBOUND",     OrderType.OUTBOUND, null,                       "일반 출고"),
    MOVE        ("MOVE",         OrderType.MOVE,     null,                       "이동"),
    RETURN_INBOUND   ("RETURN_INBOUND",    OrderType.INBOUND,  SubOrderType.RETURN_IN,     "반품 입고"),
    RETURN_OUTBOUND  ("RETURN_OUTBOUND",   OrderType.OUTBOUND, SubOrderType.RETURN_OUT,    "반품 출고"),
    DISPOSAL_OUTBOUND("DISPOSAL_OUTBOUND", OrderType.OUTBOUND, SubOrderType.DISPOSAL_OUT,  "폐기 출고"),
    SAMPLE_OUTBOUND("SAMPLE_OUTBOUND",   OrderType.OUTBOUND, SubOrderType.SAMPLE_OUT,    "샘플 출고 (재입고 필수)");

    private final String       code;
    private final OrderType    baseOrderType;
    private final SubOrderType subOrderType;
    private final String       desc;

    HostOrderType(String code, OrderType baseOrderType, SubOrderType subOrderType, String desc) {
        this.code = code;
        this.baseOrderType = baseOrderType;
        this.subOrderType = subOrderType;
        this.desc = desc;
    }

    @Override public String code() { return code; }
    @Override public String desc() { return desc; }

    public OrderType baseOrderType() { return baseOrderType; }
    public SubOrderType subOrderType() { return subOrderType; }

    /** 코드/별칭으로 enum 해석. 미일치 시 null. */
    public static HostOrderType from(Object code) {
        return EnumCodeUtil.fromCodeOrNull(HostOrderType.class, code);
    }

    /** orderTypeCode → base OrderType. HostOrderType 우선, 미일치 시 OrderType 직접 해석. */
    public static OrderType resolveBaseType(String orderTypeCode) {
        HostOrderType hostType = from(orderTypeCode);
        return hostType != null ? hostType.baseOrderType() : OrderType.from(orderTypeCode);
    }
}
