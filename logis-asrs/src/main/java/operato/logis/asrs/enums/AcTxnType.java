package operato.logis.asrs.enums;

/**
 * 재고 트랜잭션 유형 공통 코드.
 *
 * <p>
 * tb_ac_stock_txn.txn_type 에 저장되는 업무 이벤트 유형을 표준화한다.
 * </p>
 */
public enum AcTxnType {

    /** 입고 등록 */
    INBOUND,

    /** 적치(버퍼/입고지점 -> 저장 로케이션) */
    PUTAWAY,

    /** 내부 이동 */
    MOVE,

    /** 출고 또는 작업 예약 할당 */
    ALLOCATE,

    /** 기존 할당 해제 */
    RELEASE_ALLOC,

    /** 부분출고 */
    PARTIAL_OUT,

    /** 전체출고 */
    FULL_OUT,

    /** 재입고 */
    RETURN_IN,

    /** 로케이션 삭재로 재고 정리 */
    LOCATION_DELETE,

    /** 재고 조정 */
    ADJUST;

    /**
     * 문자열을 트랜잭션 유형 Enum 으로 변환한다.
     *
     * @param value 입력 문자열
     * @return AcTxnType
     */
    public static AcTxnType from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Transaction type is empty.");
        }

        for (AcTxnType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }

        throw new IllegalArgumentException("Invalid transaction type: " + value);
    }
}