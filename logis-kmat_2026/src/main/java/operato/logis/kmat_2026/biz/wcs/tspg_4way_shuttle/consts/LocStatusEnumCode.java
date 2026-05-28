package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts;

import java.util.List;

/**
 * 로케이션 상태 코드 (tb_wcs_loc_mst.status)
 *
 * DB 저장값 = INTEGER
 *
 * 0  : EMPTY
 * 10 : OCCUPIED
 * 20 : LOCKED
 * 90 : DISABLED
 */
public enum LocStatusEnumCode implements EnumCode {

    NULL(null,"NULL"),

    EMPTY(0, "비어있음 - 재고 없음, 입고 가능"),

    OCCUPIED(10, "재고있음 - 재고 존재, 출고 가능"),

    LOCKED(20, "작업중 - 입출고 주문이 점유 중, 입출고 불가"),

    DISABLED(90, "사용불가 - 운영자 통제 상태, 입출고 불가");

    private final Integer code;
    private final String desc;

    LocStatusEnumCode(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public Integer code() {
        return code;
    }

    @Override
    public String desc() {
        return desc;
    }

    public List<String> aliases() {
        return switch (this) {
            case NULL -> null;
            case EMPTY    -> List.of("FREE", "AVAILABLE");
            case OCCUPIED -> List.of("FULL", "IN_USE");
            case LOCKED   -> List.of("PROCESSING", "IN_PROGRESS");
            case DISABLED -> List.of("BLOCKED", "INACTIVE");
        };
    }

    public static LocStatusEnumCode from(Object codeOrAlias) {
        return EnumCodeUtil.fromCodeOrNull(
                LocStatusEnumCode.class,
                codeOrAlias,
                true,
                LocStatusEnumCode::aliases
        );
    }
}