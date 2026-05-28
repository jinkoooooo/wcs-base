package operato.logis.lms.consts;

import java.util.Map;

public enum SignOutType implements BaseEnum<String> {
    UNKNOWN(new EnumHelper<>("UNKNOWN", "알 수 없음")),
    MENU(new EnumHelper<>("MENU", "메뉴 로그아웃")),
    TIMEOUT(new EnumHelper<>("TIMEOUT", "유효시간 초과")),
    MULTI_LOGIN(new EnumHelper<>("MULTI_LOGIN", "중복 로그인")),
    TOKEN_INVALIDATED(new EnumHelper<>("TOKEN_INVALIDATED", "무효한 토큰"));

    private static final Map<String, SignOutType> VALUE_MAP = BaseEnum.createLookupMap(SignOutType.class);

    private final EnumHelper<String> helper;

    SignOutType(EnumHelper<String> helper) { this.helper = helper; }

    @Override
    public EnumHelper<String> getHelper() { return helper; }

    /**
     * 주어진 값을 기반으로 Enum을 찾는 메서드 (O(1) 조회)
     */
    public static SignOutType fromValue(String value) {
        return VALUE_MAP.getOrDefault(value, UNKNOWN);
    }
}
