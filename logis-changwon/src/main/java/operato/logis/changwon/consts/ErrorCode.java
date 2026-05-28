package operato.logis.changwon.consts;

import lombok.Getter;

@Getter
public enum ErrorCode {

    UNKNOWN(0, "정의되지 않은 에러 코드"),
    INVALID_PARAMETER(1001, "입력 파라미터가 유효하지 않습니다."),
    INVALID_LOCATION(2001, "해당 로케이션을 사용할 수 없습니다."),
    INVALID_STOCK(2002, "해당 재고를 사용할 수 없습니다."),
    EMPTY_LOCATION_FOR_SORTING(3001, "작업 수행을 위한 공간이 충분하지 않습니다."),
    EMPTY_VALID_LOCATION(3002, "하이랙에 사용 가능한 공간이 부족합니다."),
    EMPTY_VALID_STOCK(3003, "하이랙에 사용 가능한 재고가 부족합니다."),
    ORDER_CANCELLATION_BY_USER(4001, "사용자에 의한 작업 취소"),
    STOCK_REMOVAL_BY_USER(4002, "사용자에 의한 재고 배출"),
    NOT_EXIST_TASK(5001, "해당 작업은 존재하지 않습니다."),
    COMPLETED_TASK(5002, "해당 작업은 이미 완료 혹은 취소되었습니다.");

    private final Integer errorCode;
    private final String description;

    ErrorCode(Integer errorCode, String description) {
        this.errorCode = errorCode;
        this.description = description;
    }

    public static String convertErrorCodeFromDesc(Integer errorCode) {
        for (ErrorCode code : values()) {
            if (code.getErrorCode().equals(errorCode)) {
                return code.getDescription();
            }
        }
        return UNKNOWN.getDescription();
    }
}