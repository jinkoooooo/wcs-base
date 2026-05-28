package operato.logis.lms.consts;

import java.util.Map;

public enum PartitionTable implements BaseEnum<String> {

    UNKNOWN(new EnumHelper<>("unknown", "")),
    SESSION(new EnumHelper<>("session", "세션 로그 테이블")),
    SIGNIN(new EnumHelper<>("sign_in", "로그인/로그아웃 로그 테이블")),
    SYSTEM(new EnumHelper<>("sys", "시스템 사용 로그 테이블"));

    private static final Map<String, PartitionTable> VALUE_MAP = BaseEnum.createLookupMap(PartitionTable.class);

    private final EnumHelper<String> helper;

    PartitionTable(EnumHelper<String> helper) { this.helper = helper; }

    @Override
    public EnumHelper<String> getHelper() { return helper; }

    /**
     * 주어진 값을 기반으로 Enum을 찾는 메서드 (O(1) 조회)
     */
    public static PartitionTable fromValue(String value) {
        return VALUE_MAP.getOrDefault(value, UNKNOWN);
    }
}
