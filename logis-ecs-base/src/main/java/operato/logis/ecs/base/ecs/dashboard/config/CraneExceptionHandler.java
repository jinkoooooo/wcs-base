package operato.logis.ecs.base.ecs.dashboard.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import xyz.elidom.exception.ElidomException;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.exception.server.ElidomValidationException;
import xyz.elidom.util.ValueUtil;

import java.util.HashMap;
import java.util.Map;

/** 4-Way Shuttle API 전역 예외 처리기 */
@RestControllerAdvice(basePackages = "tspg4way.dashboard")
public class CraneExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(CraneExceptionHandler.class);

    /** 유효성 검증 예외 처리 */
    @ExceptionHandler(ElidomValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            ElidomValidationException ex, HttpServletRequest request) {

        logger.warn("Validation error at {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> response = createErrorResponse(
                "VALIDATION_ERROR",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /** 레코드 미발견 예외 처리 */
    @ExceptionHandler(ElidomRuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleNotFoundException(
            ElidomRuntimeException ex, HttpServletRequest request) {

        logger.warn("Record not found at {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> response = createErrorResponse(
                "NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /** Elidom 프레임워크 예외 처리 */
    @ExceptionHandler(ElidomException.class)
    public ResponseEntity<Map<String, Object>> handleElidomException(
            ElidomException ex, HttpServletRequest request) {

        logger.error("Elidom exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (ValueUtil.isEmpty(ex.getStatus())) {
            status = HttpStatus.valueOf(ex.getStatus());
        }

        Map<String, Object> response = createErrorResponse(
                "ELIDOM_ERROR",
                ex.getMessage(),
                status.value(),
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(response);
    }

    /** Spring 유효성 검증 예외 처리 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        logger.warn("Method argument validation error at {}", request.getRequestURI());

        StringBuilder errors = new StringBuilder();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errors.append(error.getField())
                    .append(": ")
                    .append(error.getDefaultMessage())
                    .append("; ");
        });

        Map<String, Object> response = createErrorResponse(
                "VALIDATION_ERROR",
                errors.toString().trim(),
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /** IllegalArgumentException 처리 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {

        logger.warn("Illegal argument at {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> response = createErrorResponse(
                "BAD_REQUEST",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /** NullPointerException 처리 */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Map<String, Object>> handleNullPointer(
            NullPointerException ex, HttpServletRequest request) {

        logger.error("NullPointerException at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        Map<String, Object> response = createErrorResponse(
                "INTERNAL_ERROR",
                "An unexpected error occurred. Please contact support.",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /** 일반 예외 처리 (폴백) */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(
            Exception ex, HttpServletRequest request) {

        logger.error("Unexpected error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        Map<String, Object> response = createErrorResponse(
                "INTERNAL_ERROR",
                "An unexpected error occurred: " + ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /** 에러 응답 생성 헬퍼 */
    private Map<String, Object> createErrorResponse(String code, String message, int status, String path) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("errorCode", code);
        response.put("message", message);
        response.put("status", status);
        response.put("path", path);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}
