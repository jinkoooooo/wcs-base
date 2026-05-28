package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts;

import java.util.Set;

/**
 * 로케이션 타입 코드 (tb_wcs_loc_mst.loc_type)
 *
 * [잠금 정책]
 * - RACK : 물리 랙 셀 — 작업 수행 시 Lock 대상
 * - 그 외  : 입고 포트, 출고 포트, 충전 포트, 가상 위치 —
 *            셔틀이 통과만 하는 곳이므로 Lock 하지 않는다.
 *
 * [사용법]
 *   LocTypeEnumCode.isLockable("INBOUND_PORT") → false
 *   LocTypeEnumCode.isLockable("RACK")         → true
 */
public enum LocTypeEnumCode implements EnumCode {

    /** 물리 랙 셀 — 재고 보관 위치, Lock 가능 */
    RACK("RACK", true),

    /** 입고 포트 — 컨베이어/입구, Lock 불가 */
    INBOUND_PORT("INBOUND_PORT", false),

    /** 출고 포트 — 컨베이어/출구, Lock 불가 */
    OUTBOUND_PORT("OUTBOUND_PORT", false),

    /** 입/출고 포트 - Lock 불가 */
    IN_OUTBOUND_PORT("IN_OUTBOUND_PORT", false),

    /** 충전 포트 — 셔틀 충전 위치, Lock 불가 */
    CHARGE_PORT("CHARGE_PORT", false),

    /** 가상 위치 — 시뮬레이션·테스트용, Lock 불가 */
    VIRTUAL("VIRTUAL", false);

    private static final Set<String> NON_LOCKABLE_CODES = Set.of(
            INBOUND_PORT.code,
            OUTBOUND_PORT.code,
            CHARGE_PORT.code,
            VIRTUAL.code
    );

    private final String code;
    private final boolean lockable;

    LocTypeEnumCode(String code, boolean lockable) {
        this.code = code;
        this.lockable = lockable;
    }

    @Override
    public Object code() {
        return code;
    }

    @Override
    public String desc() {
        return code;
    }

    public boolean isLockable() {
        return lockable;
    }

    /**
     * loc_type 문자열이 잠금 가능한 타입인지 반환한다.
     *
     * - null 또는 알 수 없는 값: 안전하게 잠금 가능(true)으로 처리
     *   (기존 데이터 호환성 유지)
     */
    public static boolean isLockable(String locType) {
        if (locType == null || locType.isBlank()) {
            return true;
        }
        return !NON_LOCKABLE_CODES.contains(locType.toUpperCase());
    }
}
