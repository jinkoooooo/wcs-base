package operato.logis.samsung.consts;

import java.util.Map;

/**
 * 시퀀스버퍼 재고 상태
 */
public enum BufferStockStatus implements BaseEnum<Integer> {

    UNKNOWN(new EnumHelper<>(0, "UNKNOWN")),
    IN_PRG_INBOUND(new EnumHelper<>(10, "입고중")),
    INBOUND(new EnumHelper<>(11, "보관중")),
    IN_PROGRESS_RELOCATION(new EnumHelper<>(20, "재배치중")),
    IN_PROGRESS_OUTBOUND(new EnumHelper<>(30, "출고중")),
    OUTBOUND(new EnumHelper<>(31, "출고완료"));

    private static final Map<Integer, BufferStockStatus> VALUE_MAP = BaseEnum.createLookupMap(BufferStockStatus.class);

    private final EnumHelper<Integer> helper;

    BufferStockStatus(EnumHelper<Integer> helper) { this.helper = helper; }

    @Override
    public EnumHelper<Integer> getHelper() { return helper; }

    public static BufferStockStatus fromValue(Integer value) { return VALUE_MAP.getOrDefault(value, UNKNOWN); }
}