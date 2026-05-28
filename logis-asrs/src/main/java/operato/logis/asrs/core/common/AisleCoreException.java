package operato.logis.asrs.core.common;

/**
 * AisleCore 공통 업무 예외 클래스.
 *
 * <p>
 * 단순 RuntimeException 이 아니라 표준 에러코드와 함께 던져서,
 * 추후 REST 응답 표준화나 로그 추적 시 일관되게 활용할 수 있도록 한다.
 * </p>
 */
public class AisleCoreException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 표준화된 업무 에러코드 */
    private final AisleCoreErrorCode errorCode;

    /**
     * 기본 생성자.
     *
     * @param errorCode AisleCore 표준 에러코드
     * @param message 사용자/로그용 메시지
     */
    public AisleCoreException(AisleCoreErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 원인 예외를 포함하는 생성자.
     *
     * @param errorCode AisleCore 표준 에러코드
     * @param message 사용자/로그용 메시지
     * @param cause 원인 예외
     */
    public AisleCoreException(AisleCoreErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public AisleCoreErrorCode getErrorCode() {
        return errorCode;
    }
}