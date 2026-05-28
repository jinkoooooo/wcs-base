package operato.logis.wcs.consts;

/**
 * 기본 주문 유형. 입고·출고·이동.
 */
public enum OrderType implements EnumCode {

    INBOUND("INBOUND", "입고 - 물품을 창고의 빈 로케이션에 적재"),
    OUTBOUND("OUTBOUND","출고 - 재고가 있는 로케이션에서 물품을 출고 포트로 이동"),
    MOVE("MOVE",        "이동 - 재고를 한 로케이션에서 다른 로케이션으로 재배치");

    private final String code;
    private final String desc;

    OrderType(String code, String desc) {
        this.code = code; this.desc = desc;
    }

    @Override public String code() { return code; }
    @Override public String desc() { return desc; }

    /** 코드/별칭으로 enum 해석. 미일치 시 null. */
    public static OrderType from(Object code) {
        return EnumCodeUtil.fromCodeOrNull(OrderType.class, code);
    }
}
