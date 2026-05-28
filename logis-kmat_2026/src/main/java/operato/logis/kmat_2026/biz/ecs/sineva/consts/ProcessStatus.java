package operato.logis.kmat_2026.biz.ecs.sineva.consts;

/**
 * 작업 프로세스 상태
 */
public enum ProcessStatus implements EnumCode<Integer> {
    UNKNOWN(0, "알 수 없음"),
    PREPARING(5, "작업 준비 중 - fromSide 조건 미충족"),
    READY(10, "작업 대기"),
    NEXT_TASK_READY(11, "다음 작업 대기"),
    NEXT_RELEASE_READY(13, "다음 작업 대기 상태"),
    MPS_ZONE_RELEASE_READY(14, "releaseCode 작업 대기"),
    AWAITING_FINAL_RELEASE(15, "최종 목적지 releaseCode 대기"),
    DISTRIBUTION_RELEASE_READY(16, "유통가공장 releaseCode 대기"),
    STARTING(20, "작업 시작"),
    FINISH_FROM_SIDE_LOADING(21, "fromSide 로딩 완료"),
    COMPLETE(30, "작업 완료"),
    CANCEL(50, "작업 취소"),
    ERROR(90, "작업 에러 발생"),
    ERROR_WAITING_RESUME(91, "작업 에러 발생 - 재개 조건 대기 중");

    private final Integer code;
    private final String desc;

    ProcessStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public Integer code() {
        return code;
    }

    @Override
    public String desc() {
        return desc;
    }

    public static ProcessStatus fromCodeOrNull(Object code) {
        return EnumCodeUtil.fromCodeOrNull(ProcessStatus.class, code, false, null);
    }

    public static ProcessStatus fromCode(Object code) {
        ProcessStatus result = fromCodeOrNull(code);
        return result != null ? result : UNKNOWN;
    }
}