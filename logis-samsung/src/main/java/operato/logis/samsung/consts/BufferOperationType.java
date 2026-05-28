package operato.logis.samsung.consts;

import java.util.Map;

/**
 * 시퀀스버퍼 운영모드
 */
public enum BufferOperationType implements BaseEnum<String> {

    // 비정상
    UNKNOWN(new EnumHelper<>("-1", "UNKNOWN")),
    OFFLINE(new EnumHelper<>("O", "오프라인")),
    MANUAL(new EnumHelper<>("M", "수동")),
    AUTO(new EnumHelper<>("A", "자동"));

    private static final Map<String, BufferOperationType> VALUE_MAP = BaseEnum.createLookupMap(BufferOperationType.class);

    private final EnumHelper<String> helper;

    BufferOperationType(EnumHelper<String> helper) { this.helper = helper; }

    @Override
    public EnumHelper<String> getHelper() { return helper; }

    public static BufferOperationType fromValue(String value) { return VALUE_MAP.getOrDefault(value, UNKNOWN); }
}