package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts;

import java.util.List;

public enum ShuttleOrderStatusEnumCode implements EnumCode {

    // --- [정상 프로세스: 0 ~ 89] ---
    CREATED(0, "생성됨 - 주문 생성 및 데이터 준비 완료"),
    SENT(10, "전송됨 - ECS 인터페이스 전송 완료"),
    ACCEPTED(20, "수락됨 - ECS에서 주문 수신 및 적합성 확인"),
    WAITING(25, "대기 - 물리적 트리거 대기 (BCR 스캔 전)"),
    RUNNING(30, "실행중 - 설비(셔틀/컨베이어) 가동 중"),
    ARRIVED(40, "도착 - 목적지 렉단/컨베이어 도착 완료"),

    // --- [종료 상태: 90 ~ 99] ---
    COMPLETED(90, "완료 - 전체 작업 정상 종료"),
    CANCELLED(91, "취소 - 사용자 또는 시스템에 의한 강제 취소"),
    ABORTED(95, "중단 - 작업 중 미완료 상태로 강제 종료"),

    // --- [에러 상태: 100 이상] ---
    ERROR_GENERAL(100, "일반 에러 - 정의되지 않은 일반 오류"),
    ERROR_SEND_FAIL(110, "전송 실패 - ECS 통신/소켓 전송 실패"),
    ERROR_TIMEOUT(120, "응답 지연 - ECS 작업 지시 응답 시간 초과"),
    ERROR_HARDWARE(130, "설비 에러 - 셔틀/설비 하드웨어 장애(알람)"),
    ERROR_LOCATION(140, "로케이션 에러 - 위치 부정합 또는 가득 참"),
    ERROR_INVENTORY(150, "재고 에러 - 재고 부족 또는 유실 발생");

    private final Integer code;
    private final String desc;

    ShuttleOrderStatusEnumCode(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override public Object code() { return code; }
    @Override public String desc() { return desc; }

    /** * 레거시/외부 시스템 호환 별칭
     * - 외부에서 "ERROR"나 "FAILED"로 들어오면 대표 에러 코드(100)로 매핑합니다.
     */
    public List<String> aliases() {
        return switch (this) {
            case RUNNING -> List.of("IN_PROGRESS", "WORK");
            case COMPLETED -> List.of("COMPLETE", "FINISHED", "SUCCESS");
            case CANCELLED -> List.of("CANCEL", "VOID");
            case ERROR_GENERAL -> List.of("ERROR", "FAILED", "FAIL", "ERR");
            default -> List.of();
        };
    }

    public static boolean isFinalStatus(Integer code) {
        return code >= ShuttleOrderStatusEnumCode.COMPLETED.codeAsIntOrNull();
    }

    public static ShuttleOrderStatusEnumCode from(Object codeOrAlias) {
        // EnumCodeUtil을 사용하여 숫자 코드와 문자열(aliases) 모두 지원
        return EnumCodeUtil.fromCodeOrNull(ShuttleOrderStatusEnumCode.class, codeOrAlias, true, ShuttleOrderStatusEnumCode::aliases);
    }
}