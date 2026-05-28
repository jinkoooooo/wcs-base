package operato.logis.kmat_2026.biz.ecs.sineva.support;

/**
 * ============================================================================
 * ECS 공통 RuntimeException
 * ============================================================================
 */
public class EcsException extends RuntimeException {

    private final EcsErrorCode errorCode;

    public EcsException(EcsErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public EcsException(EcsErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public EcsErrorCode getErrorCode() {
        return errorCode;
    }
}