package operato.logis.wcs.consts;

/**
 * QC 시험 의뢰 상태 (시험 마스터 단위).
 *
 * PENDING — 의뢰 등록 직후, LIMS 결과 미수신.
 * COMPLETED — LIMS 결과(test_no, PDF) 도착으로 의뢰 종결.
 *
 * {@link QcTestStatus}(item 의 통과/실패) 와 달리 의뢰 자체의 진행 상태를 표현한다.
 */
public enum QcTestRequestStatus implements EnumCode {

    DRAFT("DRAFT","작성중 (필수값 미완성)"),
    PENDING("PENDING",     "시험 결과 미수신 - 의뢰 진행 중"),
    COMPLETED("COMPLETED", "시험 결과 수신 완료 - 종결");

    private final String code;
    private final String desc;

    QcTestRequestStatus(String code, String desc) {
        this.code = code; this.desc = desc;
    }

    @Override public String code() { return code; }
    @Override public String desc() { return desc; }

    /** 코드/별칭으로 enum 해석. 미일치 시 null. */
    public static QcTestRequestStatus from(Object codeOrAlias) {
        return EnumCodeUtil.fromCodeOrNull(QcTestRequestStatus.class, codeOrAlias);
    }
}
