package operato.logis.lms.consts;

import java.util.Map;

public enum SupportStatus implements BaseEnum<String> {
    UNKNOWN(new EnumHelper<>("0", "알 수 없음")),
    DRAFT(new EnumHelper<>("1", "임시 저장")),
    REGISTERED(new EnumHelper<>("2", "요청 등록")),
    ASSIGNED(new EnumHelper<>("3", "담당자 지정")),
    IN_PROGRESS(new EnumHelper<>("4", "작업 수행")),
    COMPLETED(new EnumHelper<>("5", "완료")),
    CANCEL_REQ(new EnumHelper<>("6", "요청 취소"));

    private static final Map<String, SupportStatus> VALUE_MAP = BaseEnum.createLookupMap(SupportStatus.class);

    private final EnumHelper<String> helper;

    SupportStatus(EnumHelper<String> helper) { this.helper = helper; }

    @Override
    public EnumHelper<String> getHelper() { return helper; }

    /**
     * 주어진 값을 기반으로 Enum을 찾는 메서드 (O(1) 조회)
     */
    public static SupportStatus fromValue(String value) {
        return VALUE_MAP.getOrDefault(value, UNKNOWN);
    }
}
