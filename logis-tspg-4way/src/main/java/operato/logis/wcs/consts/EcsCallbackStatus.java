package operato.logis.wcs.consts;

import java.util.List;

/**
 * ECS → WCS 콜백 상태. 셔틀 작업의 수락·시작·진행·완료·오류·취소를 표현한다.
 */
public enum EcsCallbackStatus implements EnumCode {

    ACCEPTED("ACCEPTED",   "작업 수락 - ECS가 작업 지시를 정상적으로 수신하고 처리 대기열에 등록"),
    STARTED("STARTED",     "작업 시작 - 셔틀이 해당 작업을 시작함"),
    IN_PROGRESS("IN_PROGRESS", "작업 진행 중 - 중간 보고(선택)"),
    FROM_LOADING_COMPLETE("FROM_LOADING_COMPLETE", "출발지 로딩 완료 - from에서 화물 픽업 완료"),
    TO_UNLOADING_COMPLETE("TO_UNLOADING_COMPLETE", "목적지 언로딩 완료 - to에 화물 적재 완료"),
    RACK_CONVEYOR_ARRIVED("RACK_CONVEYOR_ARRIVED", "작업 진행 중 - 렉단 컨베이어 도착"),
    COMPLETE("COMPLETE",   "작업 완료 - 셔틀이 작업을 성공적으로 완료함"),
    ERROR("ERROR",         "작업 오류 - 셔틀 작업 중 오류 발생"),
    CANCELLED("CANCELLED", "작업 취소 - 작업이 취소됨(운영 정책에 따라 사용)");

    private final String code;
    private final String desc;

    EcsCallbackStatus(String code, String desc) {
        this.code = code; this.desc = desc;
    }

    @Override public String code() { return code; }
    @Override public String desc() { return desc; }

    /** 외부 시스템이 보내는 동의어 코드 매핑. */
    @Override public List<String> aliases() {
        return switch (this) {
            case COMPLETE -> List.of("COMPLETED");
            case ERROR    -> List.of("FAILED");
            default -> List.of();
        };
    }

    /** 코드/별칭으로 enum 해석. 미일치 시 null. */
    public static EcsCallbackStatus from(Object codeOrAlias) {
        return EnumCodeUtil.fromCodeOrNull(EcsCallbackStatus.class, codeOrAlias);
    }

    public static boolean isAccepted(Object codeOrAlias)    { return ACCEPTED.equals(from(codeOrAlias)); }
    public static boolean isStarted(Object codeOrAlias)     { return STARTED.equals(from(codeOrAlias)); }
    public static boolean isInProgress(Object codeOrAlias)  { return IN_PROGRESS.equals(from(codeOrAlias)); }
    public static boolean isComplete(Object codeOrAlias)    { return COMPLETE.equals(from(codeOrAlias)); }
    public static boolean isError(Object codeOrAlias)       { return ERROR.equals(from(codeOrAlias)); }
    public static boolean isCancelled(Object codeOrAlias)   { return CANCELLED.equals(from(codeOrAlias)); }
}
