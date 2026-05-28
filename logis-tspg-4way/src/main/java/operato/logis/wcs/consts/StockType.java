package operato.logis.wcs.consts;

import xyz.elidom.util.ValueUtil;

/**
 * 재고 분류. 일반·시험대기·시험부적합·국가검열대기·국가검정불승인·반품·폐기.
 */
public enum StockType implements EnumCode {

    NORMAL("NORMAL",           "일반"),
    QC_PENDING("QC_PENDING",   "시험 대기"),
    QC_FAIL("QC_FAIL",         "시험 부적합"),
    NIA_PENDING("NIA_PENDING", "국가 검열 대기"),
    NIA_FAIL("NIA_FAIL",       "국가 검정 불승인"),
    RETURN("RETURN",           "반품"),
    DISPOSAL("DISPOSAL",       "폐기");

    private final String code;
    private final String desc;

    StockType(String code, String desc) {
        this.code = code; this.desc = desc;
    }

    @Override public String code() { return code; }
    @Override public String desc() { return desc; }

    /** 코드/별칭으로 enum 해석. 미일치 시 null. */
    public static StockType from(Object codeOrAlias) {
        return EnumCodeUtil.fromCodeOrNull(StockType.class, codeOrAlias);
    }

    /** 코드 정확 일치로 enum 해석. 미일치/null 이면 NORMAL fallback. */
    public static StockType of(String code) {
        if (ValueUtil.isEmpty(code)) return NORMAL;
        for (StockType t : StockType.values()) {
            if (t.code.equals(code)) return t;
        }
        return NORMAL;
    }
}
