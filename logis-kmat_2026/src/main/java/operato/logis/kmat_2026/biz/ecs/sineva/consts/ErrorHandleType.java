package operato.logis.kmat_2026.biz.ecs.sineva.consts;

/**
 * 에러 복구 처리 방식
 */
public enum ErrorHandleType implements EnumCode<String> {
    UNKNOWN("-1", "알 수 없음"),
    FULL_RETRY("1", "전체 경로 재시작"),
    PARTIAL_RETRY("2", "현재 위치 이후 경로만"),
    NEAREST_RETRY("3", "현재 위치 근처부터 재시작");

    private final String code;
    private final String desc;

    ErrorHandleType(String code, String desc) {
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

    public static ErrorHandleType fromCodeOrNull(Object code) {
        return EnumCodeUtil.fromCodeOrNull(ErrorHandleType.class, code, false, null);
    }

    public static ErrorHandleType fromCode(Object code) {
        ErrorHandleType result = fromCodeOrNull(code);
        return result != null ? result : UNKNOWN;
    }
}