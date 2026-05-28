package operato.logis.wcs.consts;

import java.util.List;

/**
 * WCS 표준 에러 코드. 외부 응답·로그에 쓰이는 ERR_* 코드와 설명을 정의한다.
 */
public enum WcsError implements EnumCode {

    MISSING_REQUIRED_FIELD("ERR_REQUIRED",       "필수 필드 누락 - 요청에 필수 데이터가 없음"),
    INVALID_ORDER_TYPE("ERR_ORDER_TYPE",          "주문 유형 오류 - 지원하지 않는 orderType 값"),
    DUPLICATE_LINE("ERR_DUP_LINE",                "중복 라인 번호 - 동일 주문 내 lineNo가 중복됨"),
    INSUFFICIENT_STOCK("ERR_STOCK",               "재고 부족/없음 - 요청 수량보다 가용 재고가 적거나 없음"),
    NO_AVAILABLE_LOCATION("ERR_LOC",              "가용 로케이션 없음 - 입고/이동 시 빈 로케이션을 찾을 수 없음"),
    ALLOCATION_FAILED("ERR_ALLOC",                "할당 실패 - 로케이션/재고 할당 과정에서 실패"),
    ECS_SEND_FAILED("ERR_ECS",                    "ECS 전송 실패 - ECS로 명령 전송 중 오류 발생"),
    NO_AVAILABLE_STOCK("ERR_NO_AVAILABLE_STOCK",  "가용 재고 없음 - 해당 SKU/LOT에 대해 출고 가능한 재고가 없음"),
    INVALID_REQUEST("ERR_BAD_REQUEST",            "요청 형식 오류 - JSON 구조/필드 값이 요구사항과 다름"),
    INVALID_ORDER_ITEM("ERR_BAD_ITEM",            "주문 아이템 오류 - item list/lineNo/qty 등 라인 검증 실패"),
    LOCATION_LOCKED("ERR_LOC_LOCKED",             "로케이션 잠금 실패 - 다른 작업이 점유 중이거나 락 획득 실패"),
    ORDER_NOT_FOUND("ERR_ORDER_NOT_FOUND",        "주문 미존재 - 해당 orderKey/order를 찾지 못함"),
    STOCK_RESERVATION_FAILED("ERR_RESERVE",       "재고 예약 실패 - 재고 할당 또는 reservation 처리 실패"),
    INVALID_PARAMETER("ERR_INVALID_PARAM",        "파라미터 오류 - 필수 값이 null이거나 형식이 잘못됨"),
    INTERNAL_ERROR("ERR_INTERNAL",                "내부 오류 - 예상치 못한 시스템 오류"),

    ALLOCATION_GATED("ERR_ALLOC_GATED",              "산출 게이팅 차단"),
    OPERATION_MODE_BLOCKED("ERR_OP_MODE_BLOCKED",    "운영 모드에 의해 차단"),
    SCHEDULED_DATE_NOT_REACHED("ERR_SCHEDULED",      "scheduled_date 미도래"),
    PORT_DISPATCH_LOCKED("ERR_PORT_LOCKED",          "포트 배차 락으로 인한 지연"),
    PORT_LOCK_RELEASE_FAILED("ERR_PORT_UNLOCK",      "포트 락 강제 해제 실패"),
    INVALID_PORT_MODE_CHANGE("ERR_PORT_MODE_CHANGE", "포트 모드 전환 불가"),
    TEST_NOT_PASSED("ERR_TEST_NOT_PASSED",           "시험 미통과 - 산출 진입 불가"),
    TEST_STATE_INVALID("ERR_TEST_STATE",             "시험 상태 불일치 - 결과 수신 거부"),

    NO_AVAILABLE_ITEM_MST("ERR_NO_AVAILABLE_ITEM_MST",  "자재 마스터 값 없음"),

    PORT_MODE_NOT_READY("ERR_PORT_MODE_NOT_READY",   "포트 모드 불일치 - 현재 포트 모드로는 해당 주문 산출 불가");

    private final String code;
    private final String desc;

    WcsError(String code, String desc) {
        this.code = code; this.desc = desc;
    }

    @Override public String code() { return code; }
    @Override public String desc() { return desc; }

    /** 구 명칭 호환을 위한 동의어 코드 매핑. */
    @Override public List<String> aliases() {
        return switch (this) {
            case TEST_NOT_PASSED   -> List.of("INSPECTION_NOT_PASSED");
            case TEST_STATE_INVALID -> List.of("INSPECTION_STATE_INVALID");
            default -> List.of();
        };
    }

    /** 코드/별칭으로 enum 해석. 미일치 시 null. */
    public static WcsError from(Object codeOrAlias) {
        return EnumCodeUtil.fromCodeOrNull(WcsError.class, codeOrAlias);
    }
}
