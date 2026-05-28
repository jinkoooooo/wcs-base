package operato.logis.lms.consts;

import java.util.Map;

public enum SessionStatus implements BaseEnum<Integer> {
    UNKNOWN(new EnumHelper<>(0, "알 수 없음")),
    ACTIVE(new EnumHelper<>(1, "활성 세션")),
    LOGGED_OUT(new EnumHelper<>(2, "로그아웃으로 만료된 세션")),
    EXPIRED(new EnumHelper<>(3, "만료된 세션"));

    private static final Map<Integer, SessionStatus> VALUE_MAP = BaseEnum.createLookupMap(SessionStatus.class);

    private final EnumHelper<Integer> helper;

    SessionStatus(EnumHelper<Integer> helper) { this.helper = helper; }

    @Override
    public EnumHelper<Integer> getHelper() { return helper; }

    /**
     * 주어진 값을 기반으로 Enum을 찾는 메서드 (O(1) 조회)
     */
    public static SessionStatus fromValue(String value) {
        return VALUE_MAP.getOrDefault(value, UNKNOWN);
    }
}
