package operato.logis.asrs.core.common;

/**
 * AisleCore 업무 공통 에러코드 정의.
 *
 * <p>
 * 재고, 위치, 접근성 재산출, 정책, 전략 실행 등
 * AisleCore 전반에서 발생하는 예외 상황을 코드 단위로 표준화하기 위해 사용한다.
 * </p>
 */
public enum AisleCoreErrorCode {

    /** 요청 파라미터 또는 입력값이 유효하지 않은 경우 */
    INVALID_REQUEST,

    /** 조회 대상 엔티티가 존재하지 않는 경우 */
    ENTITY_NOT_FOUND,

    /** 중복 데이터가 존재하거나 유니크 조건을 위반하는 경우 */
    DUPLICATE_DATA,

    /** 동일 좌표 또는 동일 코드의 로케이션이 이미 존재하는 경우 */
    DUPLICATE_LOCATION,

    /** 허용되지 않은 로케이션 Side 값(L/R 외)이 들어온 경우 */
    INVALID_LOCATION_SIDE,

    /** Aisle/Bay/Level/Depth 좌표값이 비정상인 경우 */
    INVALID_LOCATION_COORDINATE,

    /** 이동 대상 로케이션이 적치/출고/이동 조건에 맞지 않는 경우 */
    INVALID_LOCATION_TARGET,

    /** 동일 stock_unit_no 가 이미 존재하는 경우 */
    DUPLICATE_STOCK_UNIT,

    /** 현재 재고 상태에서 허용되지 않는 작업을 수행하려는 경우 */
    INVALID_STOCK_STATUS,

    /** 재고 수량이 부족한 경우 */
    INSUFFICIENT_QTY,

    /** 예약 수량이 0 이하이거나 가용 수량을 초과하는 경우 */
    INVALID_ALLOCATION_QTY,

    /** 출고 수량이 0 이하이거나 출고 가능 수량을 초과하는 경우 */
    INVALID_OUTBOUND_QTY,

    /** 상품 정책 해석(센터/상품군/SKU 예외 병합)에 실패한 경우 */
    ITEM_POLICY_RESOLVE_FAILED,

    /** Access Point가 존재하지 않거나 접근 목적 기준 조회에 실패한 경우 */
    ACCESS_POINT_NOT_FOUND,

    /** 로케이션 접근성 재산출에 필요한 후보 로케이션이 없는 경우 */
    LOCATION_ACCESS_CANDIDATE_NOT_FOUND,

    /** 로케이션 등급 또는 접근성 점수 계산에 실패한 경우 */
    LOCATION_ACCESS_RECALCULATION_FAILED,

    /** 전략 세트가 존재하지 않는 경우 */
    STRATEGY_SET_NOT_FOUND,

    /** 활성화된 전략 룰이 존재하지 않는 경우 */
    STRATEGY_RULE_NOT_FOUND,

    /** 전략 룰의 condition_json / action_json 해석에 실패한 경우 */
    INVALID_STRATEGY_RULE_JSON,

    /** 전략 룰 타입이 지원되지 않는 경우 */
    UNSUPPORTED_STRATEGY_RULE_TYPE,

    /** 리로케이션 대상 재고 후보가 존재하지 않는 경우 */
    RELOCATION_SOURCE_NOT_FOUND,

    /** 리로케이션 목적지 후보가 존재하지 않는 경우 */
    RELOCATION_TARGET_NOT_FOUND,

    /** 리로케이션 swap 후보가 존재하지 않는 경우 */
    RELOCATION_SWAP_TARGET_NOT_FOUND,

    /** 전략 실행 또는 후보 산출 과정에서 실패한 경우 */
    STRATEGY_EXECUTION_FAILED,

    /** 내부 처리 중 예기치 못한 오류가 발생한 경우 */
    INTERNAL_SERVER_ERROR
}