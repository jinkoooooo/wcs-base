package operato.logis.lms.consts;

import java.util.Map;

public enum SupportCategory implements BaseEnum<String> {
    UNKNOWN(new EnumHelper<>("0", "알 수 없음")),
    EQUIP_ISSUE(new EnumHelper<>("1", "설비 이슈")),
    OPERATION(new EnumHelper<>("2", "운영 관련")),
    PREVENTIVE(new EnumHelper<>("3", "예방 정비")),
    UI(new EnumHelper<>("4", "UI 관련")),
    NETWORK(new EnumHelper<>("5", "네트워크")),
    TRAINING(new EnumHelper<>("6", "교육 훈련")),
    OTHERS(new EnumHelper<>("7", "기타"));

    private static final Map<String, SupportCategory> VALUE_MAP = BaseEnum.createLookupMap(SupportCategory.class);

    private final EnumHelper<String> helper;

    SupportCategory(EnumHelper<String> helper) { this.helper = helper; }

    @Override
    public EnumHelper<String> getHelper() { return helper; }

    /**
     * 주어진 값을 기반으로 Enum을 찾는 메서드 (O(1) 조회)
     */
    public static SupportCategory fromValue(String value) {
        return VALUE_MAP.getOrDefault(value, UNKNOWN);
    }
}
