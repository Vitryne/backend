package com.vitryne.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleProductNotFound(ProductNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "timestamp", Instant.now(),
                        "status", 404,
                        "error", "Not Found",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler({InvalidStartDateException.class, InvalidEndDateException.class,
            InvalidPromotionalPriceException.class, NullPromotionArgumentException.class})
    public ResponseEntity<Map<String, Object>> handleInvalidPromotion(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "timestamp", Instant.now(), "status", 400, "error", "Bad Request", "message", ex.getMessage()
        ));
    }
}
