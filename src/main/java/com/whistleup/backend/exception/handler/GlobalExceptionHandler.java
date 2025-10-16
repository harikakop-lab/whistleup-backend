package com.whistleup.backend.exception.handler;

import com.whistleup.backend.exception.ApiException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApiExceptions(ApiException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", ex.getStatus().value());
        body.put("error", ex.getErrorCode());
        body.put("message", ex.getMessage());
        if (ex.getDetails() != null) {
            body.put("details", ex.getDetails());
        }
        return new ResponseEntity<>(body, ex.getStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericExceptions(Exception ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", 500);
        body.put("error", "INTERNAL_SERVER_ERROR");
        body.put("message", "An unexpected error occurred.");
        body.put("details", ex.getMessage());
        return ResponseEntity.internalServerError().body(body);
    }
}
