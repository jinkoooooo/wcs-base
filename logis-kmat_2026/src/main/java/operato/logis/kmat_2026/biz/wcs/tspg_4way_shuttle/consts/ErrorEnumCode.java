package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts;

import java.util.List;

public enum ErrorEnumCode implements EnumCode {

    MISSING_REQUIRED_FIELD("ERR_REQUIRED", "필수 필드 누락 - 요청에 필수 데이터가 없음"),
    INVALID_ORDER_TYPE("ERR_ORDER_TYPE", "주문 유형 오류 - 지원하지 않는 orderType 값"),
    DUPLICATE_LINE("ERR_DUP_LINE", "중복 라인 번호 - 동일 주문 내 lineNo가 중복됨"),
    INSUFFICIENT_STOCK("ERR_STOCK", "재고 부족/없음 - 요청 수량보다 가용 재고가 적거나 없음"),
    NO_AVAILABLE_LOCATION("ERR_LOC", "가용 로케이션 없음 - 입고/이동 시 빈 로케이션을 찾을 수 없음"),
    ALLOCATION_FAILED("ERR_ALLOC", "할당 실패 - 로케이션/재고 할당 과정에서 실패"),
    ECS_SEND_FAILED("ERR_ECS", "ECS 전송 실패 - ECS로 명령 전송 중 오류 발생"),
    NO_AVAILABLE_STOCK("ERR_NO_AVAILABLE_STOCK", "가용 재고 없음 - 해당 SKU/LOT에 대해 출고 가능한 재고가 없음"),

    INVALID_REQUEST("ERR_BAD_REQUEST", "요청 형식 오류 - JSON 구조/필드 값이 요구사항과 다름"),
    INVALID_ORDER_ITEM("ERR_BAD_ITEM", "주문 아이템 오류 - item list/lineNo/qty 등 라인 검증 실패"),
    LOCATION_LOCKED("ERR_LOC_LOCKED", "로케이션 잠금 실패 - 다른 작업이 점유 중이거나 락 획득 실패"),
    ORDER_NOT_FOUND("ERR_ORDER_NOT_FOUND", "주문 미존재 - 해당 orderKey/order를 찾지 못함"),
    STOCK_RESERVATION_FAILED("ERR_RESERVE", "재고 예약 실패 - 재고 할당 또는 reservation 처리 실패"),
    INVALID_PARAMETER("ERR_INVALID_PARAM", "파라미터 오류 - 필수 값이 null이거나 형식이 잘못됨"),
    INTERNAL_ERROR("ERR_INTERNAL", "내부 오류 - 예상치 못한 시스템 오류");

    private final String code;
    private final String desc;

    ErrorEnumCode(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public Object code() {
        return code;
    }

    @Override
    public String desc() {
        return desc;
    }

    public List<String> aliases() {
        return List.of();
    }

    public static ErrorEnumCode from(Object codeOrAlias) {
        return EnumCodeUtil.fromCodeOrNull(ErrorEnumCode.class, codeOrAlias, true, ErrorEnumCode::aliases);
    }
}