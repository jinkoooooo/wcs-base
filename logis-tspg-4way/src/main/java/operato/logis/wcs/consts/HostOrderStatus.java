package operato.logis.wcs.consts;

import java.util.List;

/**
 * HOST 주문 라이프사이클 상태. 수신 → 검증 → 산출 준비 → 실행 → 완료/거절/오류.
 */
public enum HostOrderStatus implements EnumCode {

    RECEIVED(0,            "수신됨"),
    WAITING_SCHEDULE(5,    "예정일 대기 - scheduled_date 미도래 상태"),
    VALIDATED(10,          "검증완료"),
    READY_FOR_ALLOC(12,    "산출 준비 완료 - 다음 스케줄러 tick에 산출"),
    WAITING_EXEC(30,       "ECS 실행 대기"),
    EXECUTING(40,          "설비 실행 중"),
    INBOUND_TEST_WAIT(75,  "입고 후 시험 미종결"),
    COMPLETED(80,          "작업 완료"),
    CANCELLED(85,          "취소"),
    REJECTED(88,           "검증 실패 거절"),
    TEST_FAILED(90,        "시험 부적합 - 폐기 대기"),
    ERROR(100,             "처리 중 오류");

    private final Integer code;
    private final String  desc;

    HostOrderStatus(Integer code, String desc) {
        this.code = code; this.desc = desc;
    }

    @Override public Integer code() { return code; }
    @Override public String desc() { return desc; }

    /** 외부 시스템이 보내는 동의어 코드 매핑. */
    @Override public List<String> aliases() {
        return switch (this) {
            case WAITING_EXEC -> List.of("PROCESSING");
            case VALIDATED    -> List.of("VALIDATE");
            default -> List.of();
        };
    }

    /** 코드/별칭으로 enum 해석. 미일치 시 null. */
    public static HostOrderStatus from(Object codeOrAlias) {
        return EnumCodeUtil.fromCodeOrNull(HostOrderStatus.class, codeOrAlias);
    }
}
