package operato.logis.asrs.core.common;


import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import operato.logis.asrs.core.common.AisleCoreException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice(basePackages = "operato.logis.asrs")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AisleCoreRestExceptionHandler {

    @ExceptionHandler(AisleCoreException.class)
    public ResponseEntity<Map<String, Object>> handleAisleCoreException(
            AisleCoreException e,
            HttpServletRequest request
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("code", e.getErrorCode().name());
        body.put("msg", e.getMessage());
        body.put("status", 422);
        body.put("path", request.getRequestURI());
        body.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }
}