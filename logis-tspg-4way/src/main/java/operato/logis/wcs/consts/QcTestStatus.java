package operato.logis.wcs.consts;

import xyz.elidom.util.ValueUtil;

/**
 * 호스트오더 item 의 시험 결과 상태. 의뢰됨·통과·실패·취소(보류).
 */
public enum QcTestStatus implements EnumCode {

    REQUESTED("REQUESTED", "시험 의뢰됨 - 외부 시험 결과 대기 중"),
    PASSED("PASSED",       "시험 통과 - 산출 진입 가능"),
    FAILED("FAILED",       "시험 실패 - 사용자 확인 필요"),
    CANCEL("CANCEL",       "시험 취소 - 운영자 보류");

    private final String code;
    private final String desc;

    QcTestStatus(String code, String desc) {
        this.code = code; this.desc = desc;
    }

    @Override public String code() { return code; }
    @Override public String desc() { return desc; }

    /** 코드/별칭으로 enum 해석. 미일치 시 null. */
    public static QcTestStatus from(Object codeOrAlias) {
        return EnumCodeUtil.fromCodeOrNull(QcTestStatus.class, codeOrAlias);
    }

    /** 입력 문자열이 이 상태 코드와 (대소문자 무시) 일치하는지. */
    public boolean matches(String value) {
        return ValueUtil.isNotEmpty(value) && code.equalsIgnoreCase(value.trim());
    }
}
