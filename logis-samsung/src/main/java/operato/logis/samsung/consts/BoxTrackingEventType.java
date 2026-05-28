package operato.logis.samsung.consts;

import java.util.Map;

/**
 * 박스 트래킹 이벤트 타입 (INIT: 최초, UPDATE: 갱신)
 */
public enum BoxTrackingEventType implements BaseEnum<Integer> {

    BCR_EVENT   (new EnumHelper<>(10, "BCR 스캔/등록")),
    VISION_EVENT   (new EnumHelper<>(11, "VISION 스캔/등록")),
    SDS_VISION_EVENT   (new EnumHelper<>(12, "생기연_VISION 스캔/등록")),
    HOKUSHO_EVENT (new EnumHelper<>(20, "스캔 갱신/추가정보")),
    XYZ_EVENT (new EnumHelper<>(30, "파렛타이징 완료"));

    private static final Map<Integer, BoxTrackingEventType> VALUE_MAP =
            BaseEnum.createLookupMap(BoxTrackingEventType.class);

    private final EnumHelper<Integer> helper;

    BoxTrackingEventType(EnumHelper<Integer> helper) {
        this.helper = helper;
    }

    @Override
    public EnumHelper<Integer> getHelper() {
        return helper;
    }

}
