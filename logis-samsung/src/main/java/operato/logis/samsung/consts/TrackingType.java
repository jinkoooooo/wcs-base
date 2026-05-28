package operato.logis.samsung.consts;

import java.util.Map;

/** 박스 전과정의 트레킹 타입 */
public enum TrackingType implements BaseEnum<Integer> {

    UNKNOWN     (new EnumHelper<>(0,  "알 수 없음")),

    MEASURE     (new EnumHelper<>(10, "측정(BCR/체적/비전)")),         // BCR1, BCR2 모두 포함 (equipId로 구분)
    VALIDATION  (new EnumHelper<>(20, "검증 진행중(메뉴얼/코그닉스/생기연)")),
    EXECUTION   (new EnumHelper<>(50, "설비 실행 관련")), // 지시-수락-작업 중-완료
    REPORTING   (new EnumHelper<>(60, "상위 실적 보고")),
    FINALIZE    (new EnumHelper<>(70, "적지지시 내림")),

    SYSTEM      (new EnumHelper<>(90, "시스템(취소/오류)"));

    private static final Map<Integer, TrackingType> VALUE_MAP =
            BaseEnum.createLookupMap(TrackingType.class);

    private final EnumHelper<Integer> helper;

    TrackingType(EnumHelper<Integer> helper) { this.helper = helper; }

    @Override public EnumHelper<Integer> getHelper() { return helper; }

    public static TrackingType fromValue(Integer value) {
        return VALUE_MAP.getOrDefault(value, UNKNOWN);
    }
}
