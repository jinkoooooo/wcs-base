package operato.logis.wcs.consts;

import java.util.Arrays;
import java.util.List;

import xyz.elidom.util.ValueUtil;

/**
 * 셔틀 오더 라이프사이클 상태.
 *
 * 진행중(0~40) 정상 단조 전이, 완전종료(90~95) 역행 차단 대상,
 * 에러(100~190) 운영자 force 복구 가능.
 */
public enum ShuttleOrderStatus implements EnumCode {

    CREATED(0,  "생성됨 - 주문 생성 및 데이터 준비 완료"),
    SENT(10,    "전송됨 - ECS 인터페이스 전송 완료"),
    ACCEPTED(20,"수락됨 - ECS에서 주문 수신 및 적합성 확인"),
    WAITING(25, "대기 - 물리적 트리거 대기 (BCR 스캔 전)"),
    RUNNING(30, "실행중 - 설비(셔틀/컨베이어) 가동 중"),
    ARRIVED(40, "도착 - 목적지 렉단/컨베이어 도착 완료"),

    COMPLETED(90, "완료 - 전체 작업 정상 종료"),
    CANCELLED(91, "취소 - 사용자 또는 시스템에 의한 강제 취소"),
    ABORTED(95,   "중단 - 작업 중 미완료 상태로 강제 종료"),

    ERROR_GENERAL(100,        "일반 에러 - 정의되지 않은 일반 오류"),
    ERROR_SEND_FAIL(110,      "전송 실패 - ECS 통신/소켓 전송 실패"),
    ERROR_TIMEOUT(120,        "응답 지연 - ECS 작업 지시 응답 시간 초과"),
    ERROR_HARDWARE(130,       "설비 에러 - 셔틀/설비 하드웨어 장애(알람)"),
    ERROR_LOCATION(140,       "로케이션 에러 - 위치 부정합 또는 가득 참"),
    ERROR_INVENTORY(150,      "재고 에러 - 재고 부족 또는 유실 발생"),
    ERROR_SYSTEM_RESTART(190, "시스템 재시작 에러 - 서버 재기동 시 진행 중 상태로 남은 고아 오더");

    private final Integer code;
    private final String  desc;

    ShuttleOrderStatus(Integer code, String desc) {
        this.code = code; this.desc = desc;
    }

    @Override public Integer code() { return code; }
    @Override public String desc() { return desc; }

    /** 외부 시스템이 보내는 동의어 코드 매핑. */
    @Override public List<String> aliases() {
        return switch (this) {
            case RUNNING   -> List.of("IN_PROGRESS", "WORK");
            case COMPLETED -> List.of("COMPLETE", "FINISHED", "SUCCESS");
            case CANCELLED -> List.of("CANCEL", "VOID");
            case ERROR_GENERAL -> List.of("ERROR", "FAILED", "FAIL", "ERR");
            default -> List.of();
        };
    }

    /** 코드/별칭으로 enum 해석. 미일치 시 null. */
    public static ShuttleOrderStatus from(Object codeOrAlias) {
        return EnumCodeUtil.fromCodeOrNull(ShuttleOrderStatus.class, codeOrAlias);
    }

    /**
     * 완전 종료 — 더 이상 어떤 전이도 일어나서는 안 되는 상태(COMPLETED/CANCELLED/ABORTED).
     * 역행 차단의 기준. 늦게 도착한 중복 콜백으로 비종료 상태로 되돌아가지 않게 한다.
     * 에러 상태는 복구 가능하므로 제외한다.
     */
    public static boolean isTerminal(Integer code) {
        if (ValueUtil.isEmpty(code)) return false;
        int c = code;
        return c == COMPLETED.code() || c == CANCELLED.code() || c == ABORTED.code();
    }

    /**
     * 에러 상태 — 작업이 멈췄으나 운영자 복구(force)로 재개 가능(ERROR_* 100~190).
     * 자동 콜백 경로로는 다른 상태로 전이시키지 않는다.
     */
    public static boolean isError(Integer code) {
        return ValueUtil.isNotEmpty(code) && code >= ERROR_GENERAL.codeAsIntOrNull();
    }

    /**
     * 자동 진행 중단 기준 — 완전 종료 + 에러 둘 다 포함(>= COMPLETED).
     * 역행 차단에는 {@link #isTerminal(Integer)}, 복구 판정에는 {@link #isError(Integer)} 를 쓴다.
     */
    public static boolean isFinalStatus(Integer code) {
        return ValueUtil.isNotEmpty(code) && code >= COMPLETED.codeAsIntOrNull();
    }

    /** ARRIVED(40) 도착 상태 여부. */
    public static boolean isArrived(Integer code) {
        return ValueUtil.isNotEmpty(code) && code.intValue() == ARRIVED.codeAsIntOrNull();
    }

    /** 활성 상태 — 미종료(not-empty && !isFinalStatus). 진행 중인 오더 판정. */
    public static boolean isActive(Integer code) {
        return ValueUtil.isNotEmpty(code) && !isFinalStatus(code);
    }

    /**
     * 역행 차단 대상 코드 목록 = 완전 종료(COMPLETED/CANCELLED/ABORTED)만.
     * transitionOrderStatus(force=false) 의 WHERE order_status NOT IN (...) 에 사용.
     * 에러 코드(100~190)는 복구 가능하므로 포함하지 않는다.
     */
    public static List<Integer> terminalCodes() {
        return Arrays.stream(values())
                .map(ShuttleOrderStatus::codeAsIntOrNull)
                .filter(ShuttleOrderStatus::isTerminal)
                .toList();
    }
}