package operato.logis.kmat_2026.biz.ecs.sineva.consts;

/**
 * Sineva 설비 작업 타입
 */
public enum EquipTaskType implements EnumCode<String> {
    UNKNOWN("-1", "알 수 없음"),
    MULTI_FREIGHT_MOVE("1", "다중 toSide 화물 운송"),
    FREIGHT_MOVE("2", "화물 운송"),
    SIMPLE_MOVE("3", "단순 이동"),
    ONLY_TO_SIDE_MOVE("4", "목적지 이동");

    private final String code;
    private final String desc;

    EquipTaskType(String code, String desc) {
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

    public static EquipTaskType fromCodeOrNull(Object code) {
        return EnumCodeUtil.fromCodeOrNull(EquipTaskType.class, code, false, null);
    }

    public static EquipTaskType fromCode(Object code) {
        EquipTaskType result = fromCodeOrNull(code);
        return result != null ? result : UNKNOWN;
    }
}