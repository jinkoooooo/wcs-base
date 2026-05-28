package operato.logis.wcs.consts;

/**
 * 로케이션 유형. 랙 셀만 Lock 가능하고 포트·가상 위치는 Lock 불가다.
 */
public enum LocType implements EnumCode {

    RACK("RACK",                      true,  "물리 랙 셀 — 재고 보관 위치, Lock 가능"),
    INBOUND_PORT("INBOUND_PORT",      false, "입고 포트 — 컨베이어/입구, Lock 불가"),
    OUTBOUND_PORT("OUTBOUND_PORT",    false, "출고 포트 — 컨베이어/출구, Lock 불가"),
    IN_OUTBOUND_PORT("IN_OUTBOUND_PORT", false, "입/출고 겸용 포트 — Lock 불가"),
    CHARGE_PORT("CHARGE_PORT",        false, "충전 포트 — 셔틀 충전 위치, Lock 불가"),
    VIRTUAL("VIRTUAL",               false, "가상 위치 — 시뮬레이션/테스트용, Lock 불가");

    private final String  code;
    private final boolean lockable;
    private final String  desc;

    LocType(String code, boolean lockable, String desc) {
        this.code = code; this.lockable = lockable; this.desc = desc;
    }

    @Override public String code() { return code; }
    @Override public String desc() { return desc; }

    public boolean isLockable() { return lockable; }

    /** 코드/별칭으로 enum 해석. 미일치 시 null. */
    public static LocType from(Object code) {
        return EnumCodeUtil.fromCodeOrNull(LocType.class, code);
    }

    /** 코드 문자열로 Lock 가능 여부 판정. 미지정/미상 코드는 가능으로 본다. */
    public static boolean isLockable(String locType) {
        LocType type = from(locType);
        return type == null || type.isLockable();
    }
}
