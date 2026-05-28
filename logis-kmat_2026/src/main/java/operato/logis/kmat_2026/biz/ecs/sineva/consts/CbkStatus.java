package operato.logis.kmat_2026.biz.ecs.sineva.consts;

/**
 * Sineva 콜백 상태 코드
 */
public enum CbkStatus implements EnumCode<String> {
    UNKNOWN("-1", "알 수 없음"),
    ERROR("0", "오류 발생"),
    IN_PROGRESS("1", "작업 시작"),
    FINISH_LOADING("2", "로딩 완료(fromSide)"),
    SUCCESS("3", "중간 포인트 도착"),
    END("4", "모든 작업 완료"),
    ERROR_RECOVERY("5", "Error 해제");

    private final String code;
    private final String desc;

    CbkStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String desc() {
        return desc;
    }

    public static CbkStatus fromCodeOrNull(Object code) {
        return EnumCodeUtil.fromCodeOrNull(CbkStatus.class, code, false, null);
    }

    public static CbkStatus fromCode(Object code) {
        CbkStatus result = fromCodeOrNull(code);
        return result != null ? result : UNKNOWN;
    }
}