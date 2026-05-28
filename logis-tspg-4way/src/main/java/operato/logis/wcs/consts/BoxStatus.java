package operato.logis.wcs.consts;

import xyz.elidom.util.ValueUtil;

/**
 * 박스 라이프사이클 상태.
 *
 * 전이: DRAFT(10) → PENDING(20) → PRINTED(30) → SCANNED(40) → DEPLETED(90)
 * SCANNED 는 부분 출고 시 자기 자신 유지. * → VOID(99) 는 어느 상태에서나 가능.
 *
 * 코드는 10자리 단위로 띄워 중간 상태 삽입 여지를 둔다.
 */
public enum BoxStatus implements EnumCode {

    DRAFT   (10, "생성됨 - 일련번호 미발번"),
    PENDING (20, "확정됨 - 라벨 미발행"),
    PRINTED (30, "라벨 발행됨 - 입고 스캔 대기"),
    SCANNED (40, "입고 스캔 완료 - 재고 있음"),
    DEPLETED(90, "출고 완료 - 박스 비움"),
    VOID    (99, "폐기");

    private final Integer code;
    private final String  desc;

    BoxStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override public Integer code() { return code; }
    @Override public String  desc() { return desc; }

    /** Integer 코드로 enum 해석. 미일치 시 null. */
    public static BoxStatus fromCode(Integer code) {
        if (ValueUtil.isEmpty(code)) return null;
        for (BoxStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }

    /**
     * 상태 전이 허용 여부.
     * DRAFT→PENDING, PENDING→PRINTED/SCANNED, PRINTED→SCANNED/DEPLETED, SCANNED→DEPLETED.
     * 자기 자신은 항상 허용(부분 출고). VOID 는 VOID 외 어느 상태에서나 가능.
     */
    public boolean canTransitionTo(BoxStatus next) {
        if (ValueUtil.isEmpty(next)) return false;
        if (this == next) return true;          // 자기 자신은 항상 허용 (부분 출고 등)
        if (next == VOID) return this != VOID;  // VOID 는 어느 상태에서나 가능

        return switch (this) {
            case DRAFT    -> next == PENDING;
            case PENDING  -> next == PRINTED || next == SCANNED;
            case PRINTED  -> next == SCANNED || next == DEPLETED;
            case SCANNED  -> next == DEPLETED;
            case DEPLETED -> false;
            case VOID     -> false;
        };
    }
}