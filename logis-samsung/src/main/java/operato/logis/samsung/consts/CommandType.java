package operato.logis.samsung.consts;

import java.util.Map;

/** CommandType & ResultType 공용 */
public enum CommandType implements BaseEnum<Integer> {
    UNKNOWN        (new EnumHelper<>(0,   "알 수 없음")),

    /* 10x: MEASURE (BCR/비전 공통) */
    QSTS       (new EnumHelper<>(10, "설비 상태 조회")),

    /* 20x: VALIDATION */
    DVRT       (new EnumHelper<>(20, "컨베이어 분기")),
    TURN       (new EnumHelper<>(30, "턴 디버터 박스 회전 지시")),
    PMOV       (new EnumHelper<>(40, "파렛트 이송")),
    PLTZ       (new EnumHelper<>(50, "파렛타이저 포인트 도착 보고"));

    private static final Map<Integer, CommandType> VALUE_MAP =
            BaseEnum.createLookupMap(CommandType.class);

    private final EnumHelper<Integer> helper;

    CommandType(EnumHelper<Integer> helper) { this.helper = helper; }

    @Override public EnumHelper<Integer> getHelper() { return helper; }

    public static CommandType fromValue(Integer value) {
        return VALUE_MAP.getOrDefault(value, UNKNOWN);
    }
}
