package operato.logis.kmat_2026.biz.ecs.sineva.support;

/**
 * ============================================================================
 * ECS 공통 에러 코드
 * ============================================================================
 *
 * [설계 원칙]
 * - 예외 클래스를 여러 개로 쪼개지 않는다.
 * - 공통 예외 + 에러 코드 조합으로 운영 추적성을 높인다.
 */
public enum EcsErrorCode {
    ORDER_NOT_FOUND,
    INVALID_ORDER_STATUS,
    INVALID_PARAMETER,
    INVALID_CALLBACK,
    LOCK_CONFLICT,
    LOCK_OWNER_MISMATCH,
    CANCEL_NOT_ALLOWED,
    LOCATION_NOT_FOUND,
    ECS_COMMAND_FAILED,
    INTERNAL_ERROR
}