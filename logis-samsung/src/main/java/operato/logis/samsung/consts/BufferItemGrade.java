package operato.logis.samsung.consts;

import java.util.Map;

/**
 * 재고별 등급
 */
public enum BufferItemGrade implements BaseEnum<Integer> {
    UNKNOWN(new EnumHelper<>(0, "알 수 없음")),
    CLASS_A(new EnumHelper<>(1, "고빈도")),
    CLASS_B(new EnumHelper<>(2, "저빈도")),
    CLASS_C(new EnumHelper<>(3, "초저빈도"));

    private static final Map<Integer, BufferItemGrade> VALUE_MAP = BaseEnum.createLookupMap(BufferItemGrade.class);

    private final EnumHelper<Integer> helper;

    BufferItemGrade(EnumHelper<Integer> helper) { this.helper = helper; }

    @Override
    public EnumHelper<Integer> getHelper() { return helper; }

    public static BufferItemGrade fromValue(Integer value) {
        return VALUE_MAP.getOrDefault(value, UNKNOWN);
    }
}