package operato.logis.asrs.enums;

/**
 * 재고 상태 공통 코드.
 *
 * <p>
 * stock_unit 의 현재 상태를 표현하며,
 * 재고 할당/출고/보류 처리 시 주요 검증 기준으로 사용된다.
 * </p>
 */
public enum AcStockStatus {

    /** 정상 가용 재고 */
    AVAILABLE,

    /** 일부 또는 전체 수량이 예약된 재고 */
    RESERVED,

    /** 품질/운영상 보류 상태 재고 */
    HOLD,

    /** 출고 완료 또는 더 이상 현재고로 취급하지 않는 상태 */
    OUT;

    /**
     * 문자열을 재고상태 Enum 으로 변환한다.
     *
     * @param value 입력 문자열
     * @return AcStockStatus
     */
    public static AcStockStatus from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Stock status is empty.");
        }

        for (AcStockStatus status : values()) {
            if (status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }

        throw new IllegalArgumentException("Invalid stock status: " + value);
    }

    /**
     * 예약 또는 추가 할당이 가능한 상태인지 여부.
     *
     * <p>
     * 1차 기준에서는 AVAILABLE, RESERVED 상태만 할당 대상이 될 수 있도록 본다.
     * </p>
     */
    public boolean isAllocatable() {
        return this == AVAILABLE || this == RESERVED;
    }

    /**
     * 보류 상태인지 여부.
     */
    public boolean isHold() {
        return this == HOLD;
    }

    /**
     * 출고 완료 상태인지 여부.
     */
    public boolean isOut() {
        return this == OUT;
    }
}