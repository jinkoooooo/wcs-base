package operato.logis.kmat_2026.biz.ecs.sineva.consts;

/**
 * HTTP 메서드 Enum
 */
public enum HttpMethod implements EnumCode<String> {
    GET("GET", "데이터 조회"),
    POST("POST", "데이터 생성 및 전송");

    private final String code;
    private final String desc;

    HttpMethod(String code, String desc) {
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

    public static HttpMethod fromCodeOrNull(Object code) {
        return EnumCodeUtil.fromCodeOrNull(HttpMethod.class, code, true, null);
    }

    public static HttpMethod fromCode(Object code) {
        HttpMethod result = fromCodeOrNull(code);
        return result != null ? result : GET;
    }
}