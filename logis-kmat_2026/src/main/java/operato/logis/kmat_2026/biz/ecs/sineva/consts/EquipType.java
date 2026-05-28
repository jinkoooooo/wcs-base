package operato.logis.kmat_2026.biz.ecs.sineva.consts;

/**
 * 설비 유형
 */
public enum EquipType implements EnumCode<String> {
    UNKNOWN("unknown", "알 수 없음"),
    AMR("0", "AMR"),
    AGF("1", "AGF"),
    AGF_PRC("2", "AGF_PRC"),
    MDAS_AMR("3", "AMR");

    private final String code;
    private final String desc;

    EquipType(String code, String desc) {
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

    public static EquipType fromCodeOrNull(Object code) {
        return EnumCodeUtil.fromCodeOrNull(EquipType.class, code, false, null);
    }

    public static EquipType fromCode(Object code) {
        EquipType result = fromCodeOrNull(code);
        return result != null ? result : UNKNOWN;
    }
}