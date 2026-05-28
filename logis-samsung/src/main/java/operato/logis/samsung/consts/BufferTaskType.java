package operato.logis.samsung.consts;

import java.util.Map;

/**
 * 시퀀스버퍼 작업 유형
 */
public enum BufferTaskType implements BaseEnum<String> {

    UNKNOWN(new EnumHelper<>("-1", "UNKNOWN")),
    INBOUND(new EnumHelper<>("I", "입고")),
    OUTBOUND(new EnumHelper<>("O", "출고")),
    RELOCATION(new EnumHelper<>("R", "재배치"));

    private static final Map<String, BufferTaskType> VALUE_MAP = BaseEnum.createLookupMap(BufferTaskType.class);

    private final EnumHelper<String> helper;

    BufferTaskType(EnumHelper<String> helper) { this.helper = helper; }

    @Override
    public EnumHelper<String> getHelper() { return helper; }

    public static BufferTaskType fromValue(String value) { return VALUE_MAP.getOrDefault(value, UNKNOWN); }
}