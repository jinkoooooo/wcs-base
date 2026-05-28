package operato.logis.wcs.consts;

import xyz.elidom.util.ValueUtil;

/**
 * 주문 세부 유형. 박스 스캔·자동 finalize·포트 락 등 처리 분기를 결정한다.
 */
public enum SubOrderType implements EnumCode {

    NORMAL("NORMAL", "일반 - 박스 스캔 필수"),
    PARTIAL_OUT("PARTIAL_OUT", "부분 출고 - 잔여 재입고 예정, 포트 락 필요"),
    SAMPLE_OUT("SAMPLE_OUT", "시험용 출고 - 박스 스캔 생략, 도착 즉시 자동 finalize"),
    SAMPLE_DISCARD("SAMPLE_DISCARD", "시험 부적합 폐기 출고 - 박스 스캔 생략"),
    RETURN_IN("RETURN_IN", "반품 입고 - finalize 시 stock_type=RETURN 강제"),
    RETURN_OUT("RETURN_OUT", "반품 출고 - 박스 스캔 생략, 도착 즉시 자동 finalize"),
    DISPOSAL_OUT("DISPOSAL_OUT", "폐기 출고 - 박스 스캔 생략, finalize 시 논리 삭제");

    private final String code;
    private final String desc;

    SubOrderType(String code, String desc) {
        this.code = code; this.desc = desc;
    }

    @Override public String code() { return code; }
    @Override public String desc() { return desc; }

    /** 코드/별칭으로 enum 해석. 미일치 시 null. */
    public static SubOrderType from(Object codeOrAlias) {
        return EnumCodeUtil.fromCodeOrNull(SubOrderType.class, codeOrAlias);
    }

    /** 도착 즉시 박스 스캔 없이 자동 finalize 되는 유형인지. */
    public boolean isAutoFinalize() {
        return this == SAMPLE_OUT || this == SAMPLE_DISCARD
                || this == DISPOSAL_OUT || this == RETURN_OUT;
    }

    /** 시험용 샘플 흐름인지. */
    public boolean isSampleFlow() {
        return this == SAMPLE_OUT || this == SAMPLE_DISCARD;
    }

    /** 출고 후 잔여 재고 자동 재입고 대상인지. */
    public boolean isAutoReInbound() {
        return this == NORMAL || this == PARTIAL_OUT;
    }

    /** 코드/별칭으로 enum 해석. 미일치/null 이면 NORMAL fallback. */
    public static SubOrderType fromOrNormal(Object codeOrAlias) {
        SubOrderType s = from(codeOrAlias);
        return ValueUtil.isNotEmpty(s) ? s : NORMAL;
    }

    /** ECS 송신 시 포트 락이 필요한 sub type. 신규 케이스는 여기에만 추가. */
    public boolean requiresPortLock() {
        return this == PARTIAL_OUT;
    }
}
