package operato.logis.samsung.consts;

import java.util.Map;

/**
 * 시퀀스버퍼 작업 상태
 */
public enum BufferTaskStatus implements BaseEnum<Integer> {
    UNKNOWN(new EnumHelper<>(0, "알 수 없음")),
    READY(new EnumHelper<>(1, "작업 시작 전")),
    RUNNING(new EnumHelper<>(2, "작업 중")),
    WAITING(new EnumHelper<>(3, "작업 중 정지")),
    COMPLETE(new EnumHelper<>(4, "작업 완료")),
    CANCEL(new EnumHelper<>(9, "작업 취소"));

    private static final Map<Integer, BufferTaskStatus> VALUE_MAP = BaseEnum.createLookupMap(BufferTaskStatus.class);

    private final EnumHelper<Integer> helper;

    BufferTaskStatus(EnumHelper<Integer> helper) { this.helper = helper; }

    @Override
    public EnumHelper<Integer> getHelper() { return helper; }

    public static BufferTaskStatus fromValue(Integer value) {
        return VALUE_MAP.getOrDefault(value, UNKNOWN);
    }
}