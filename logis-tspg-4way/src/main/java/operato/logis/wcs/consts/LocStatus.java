package operato.logis.wcs.consts;

import java.util.List;

/**
 * 로케이션 점유 상태. 비어있음·재고있음·작업중·이상감지(공출고/이중입고)·사용불가.
 */
public enum LocStatus implements EnumCode {

    NULL(null,    "NULL"),
    EMPTY(0,      "비어있음 - 재고 없음, 입고 가능"),
    OCCUPIED(10,  "재고있음 - 재고 존재, 출고 가능"),
    LOCKED(20,    "작업중 - 입출고 주문이 점유 중, 입출고 불가"),
    EMPTY_PICK(30,"공출고 감지 - 시스템 재고O 실물X, 수동 재고 삭제로 복구 필요"),
    DOUBLE_ENTRY(40,"이중입고 감지 - 실물O 시스템 재고X, 수동 재고 생성으로 복구 필요"),
    DISABLED(90,  "사용불가 - 운영자 통제 상태, 입출고 불가");

    private final Integer code;
    private final String  desc;

    LocStatus(Integer code, String desc) {
        this.code = code; this.desc = desc;
    }

    @Override public Integer code() { return code; }
    @Override public String  desc() { return desc; }

    /** 외부 시스템이 보내는 동의어 코드 매핑. */
    @Override public List<String> aliases() {
        return switch (this) {
            case NULL         -> null;
            case EMPTY        -> List.of("FREE", "AVAILABLE");
            case OCCUPIED     -> List.of("FULL", "IN_USE");
            case LOCKED       -> List.of("PROCESSING", "IN_PROGRESS");
            case EMPTY_PICK   -> List.of("PHANTOM_STOCK", "EMPTY_PICK_ERROR");
            case DOUBLE_ENTRY -> List.of("GHOST_CARGO", "DOUBLE_ENTRY_ERROR");
            case DISABLED     -> List.of("BLOCKED", "INACTIVE");
        };
    }

    /** 코드/별칭으로 enum 해석. 미일치 시 null. */
    public static LocStatus from(Object codeOrAlias) {
        return EnumCodeUtil.fromCodeOrNull(LocStatus.class, codeOrAlias);
    }
}
