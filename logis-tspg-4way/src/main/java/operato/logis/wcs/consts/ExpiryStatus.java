package operato.logis.wcs.consts;

/**
 * 사용기한 색상 상태.
 *
 * 임계값:
 *   D-180 이상 → NORMAL
 *   D-90 이상 D-180 미만 → WARN
 *   D-0 이상 D-90 미만 → ALERT
 *   만료(D < 0) → EXPIRED
 *   expiry_days/produce_date 누락 → UNKNOWN
 */
public enum ExpiryStatus implements EnumCode {

    NORMAL("NORMAL",     "정상 (D-180 이상)"),
    WARN("WARN",         "주의 (D-90 이상 D-180 미만)"),
    ALERT("ALERT",       "경고 (D-0 이상 D-90 미만)"),
    EXPIRED("EXPIRED",   "만료"),
    UNKNOWN("UNKNOWN",   "계산 불가");

    private final String code;
    private final String desc;

    ExpiryStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override public String code() { return code; }
    @Override public String desc() { return desc; }

    /** 코드/별칭으로 enum 해석. 미일치 시 null. */
    public static ExpiryStatus from(Object codeOrAlias) {
        return EnumCodeUtil.fromCodeOrNull(ExpiryStatus.class, codeOrAlias);
    }
}
