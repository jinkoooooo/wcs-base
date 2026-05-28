package operato.logis.samsung.consts;

import java.util.Map;

/** 세부 트레킹 상태 정의 */
public enum TrackingStatus implements BaseEnum<Integer> {

    UNKNOWN        (new EnumHelper<>(0,   "알 수 없음")),

    /* 10x: MEASURE (BCR/비전 공통) */
    FIRST_INPUT       (new EnumHelper<>(100, "최초 입고")),
    BCR_MEASURED       (new EnumHelper<>(110, "측정 완료(바코드/규격)")),
    VISION_MEASURED       (new EnumHelper<>(120, "측정 완료(이미지 확보)")),
    BCR_NOREAD       (new EnumHelper<>(130, "BCR NoRead")),

    /* 20x: VALIDATION */
    VISION_VALID_WAIT       (new EnumHelper<>(200, "검증 대기중")),
    VISION_VALID_OK       (new EnumHelper<>(210, "비전 검증 OK")),
    VISION_VALID_NG       (new EnumHelper<>(211, "비전 검증 NG")),
    HEIGHT_VALID_OK       (new EnumHelper<>(220, "높이 검증 OK")),
    HEIGHT_VALID_NG       (new EnumHelper<>(221, "높이 검증 NG")),
    CHUTE_NOT_ASSIGN       (new EnumHelper<>(230, "현재 미 진행 상품 NG")),
    MANUAL_FLAG_TRUE       (new EnumHelper<>(240, "수동 적치 상품")),
    FINAL_VALID_OK       (new EnumHelper<>(250, "작업자 최종 검증 OK")),
    FINAL_VALID_NG       (new EnumHelper<>(251, "작업자 최종 검증 NG")),

    /* 50x: EXECUTION (컨베이어/XYZ 등 공통 실행 단계) */
    COMMANDED_DVRT    (new EnumHelper<>(500, "분기지시 송신")),
    REPORT_DVRT   (new EnumHelper<>(501, "분기완료 실적수신")),
    COMMANDED_TURN    (new EnumHelper<>(510, "회전지시 송신")),
    COMMANDED_TURN_STOP    (new EnumHelper<>(519, "최종 BCR NoRead")),
    REPORT_TURN   (new EnumHelper<>(511, "회전완료 실적수신")),
    COMMANDED_PMOV    (new EnumHelper<>(520, "파렛트 보충지시 송신")),
    REPORT_PMOV   (new EnumHelper<>(521, "파렛트 보충완료 실적수신")),
    REPORT_PLTZ   (new EnumHelper<>(531, "파렛타이징 요청수신")),

    /* 60x: REPORTING */
    REPORTED       (new EnumHelper<>(600, "상위 보고 완료")),
    REPORT_ACK     (new EnumHelper<>(601, "상위 보고 수락")),

    /* 70x/80x: FINALIZE */
    STORED         (new EnumHelper<>(700, "적치 완료")),
    REJECTED       (new EnumHelper<>(701, "리젝 완료")),

    /* 90x: SYSTEM */
    CANCELED       (new EnumHelper<>(900, "취소")),
    ERROR          (new EnumHelper<>(990, "오류"));

    private static final Map<Integer, TrackingStatus> VALUE_MAP =
            BaseEnum.createLookupMap(TrackingStatus.class);

    private final EnumHelper<Integer> helper;

    TrackingStatus(EnumHelper<Integer> helper) { this.helper = helper; }

    @Override public EnumHelper<Integer> getHelper() { return helper; }

    public static TrackingStatus fromValue(Integer value) {
        return VALUE_MAP.getOrDefault(value, UNKNOWN);
    }

    public static TrackingStatus toMeasureStatus(BoxTrackingEventType t) {
        if (t == BoxTrackingEventType.BCR_EVENT)    return TrackingStatus.BCR_MEASURED;
        if (t == BoxTrackingEventType.VISION_EVENT) return TrackingStatus.VISION_MEASURED;
        return TrackingStatus.UNKNOWN;
    }
    public static TrackingStatus toVisionStatus(VisionJudgeResult t) {
        if (t == VisionJudgeResult.HAS_NG) return TrackingStatus.VISION_VALID_NG;
        if (t == VisionJudgeResult.ALL_OK) return TrackingStatus.VISION_VALID_OK;
        return TrackingStatus.UNKNOWN;
    }
    public static TrackingStatus toReportStatus(String t) {
        if (t.equals("DVRT")) return TrackingStatus.REPORT_DVRT;
        if (t.equals("PMOV")) return TrackingStatus.REPORT_PMOV;
        if (t.equals("TURN")) return TrackingStatus.REPORT_TURN;
        if (t.equals("PLTZ")) return TrackingStatus.REPORT_PLTZ;
        return TrackingStatus.UNKNOWN;
    }
}
