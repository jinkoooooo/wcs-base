package operato.logis.kmat_2026.biz.ecs.sineva.consts;

/**
 * 로케이션 상태
 */
public enum LocationStatus implements EnumCode<String> {
    EMPTY("EMPTY", "EMPTY"),
    POD("POD", "선반랙"),
    FULL("FULL", "선반랙 + 자재"),
    READY("READY", "대기");

    private final String code;
    private final String desc;

    LocationStatus(String code, String desc) {
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

    public static LocationStatus fromCodeOrNull(Object code) {
        return EnumCodeUtil.fromCodeOrNull(LocationStatus.class, code, false, null);
    }

    public static LocationStatus fromCode(Object code) {
        LocationStatus result = fromCodeOrNull(code);
        return result != null ? result : EMPTY;
    }
}