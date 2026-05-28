package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts;

import java.util.Collections;
import java.util.List;

public enum OrderTypeEnumCode implements EnumCode {

    INBOUND("INBOUND", "입고 - 물품을 창고의 빈 로케이션에 적재"),
    OUTBOUND("OUTBOUND", "출고 - 재고가 있는 로케이션에서 물품을 출고 포트로 이동"),
    MOVE("MOVE", "이동 - 재고를 한 로케이션에서 다른 로케이션으로 재배치");

    private final String code;
    private final String desc;

    OrderTypeEnumCode(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override public Object code() { return code; }
    @Override public String desc() { return desc; }

    /** 별칭 필요 시 여기서 확장 */
    public List<String> aliases() {
        return Collections.emptyList();
    }

    public static OrderTypeEnumCode from(Object code) {
        return EnumCodeUtil.fromCodeOrNull(OrderTypeEnumCode.class, code, true, OrderTypeEnumCode::aliases);
    }
}