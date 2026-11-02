package com.vitryne.api.shared.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
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

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleStatus(
            ResponseStatusException exception
    ) {
        String message = exception.getReason() == null
                ? "Não foi possível processar o cadastro"
                : exception.getReason();

        return ResponseEntity.status(exception.getStatusCode())
                .body(Map.of("message", message));
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ProblemDetail> handleInvalidInput(
            InvalidInputException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
        problem.setTitle("Dados inválidos");

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException exception
    ) {
        Map<String, String> fields = new LinkedHashMap<>();

        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fields.put(
                        error.getField(),
                        error.getDefaultMessage() == null
                                ? "Valor inválido"
                                : error.getDefaultMessage()
                ));

        return ResponseEntity.badRequest()
                .body(Map.<String, Object>of(
                        "message", "Dados inválidos",
                        "fields", fields
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidArgument(
            IllegalArgumentException exception
    ) {
        return ResponseEntity.badRequest()
                .body(Map.<String, Object>of(
                        "message",
                        exception.getMessage() == null
                                ? "Dados inválidos"
                                : exception.getMessage()
                ));
    }

    @ExceptionHandler(ArithmeticException.class)
    public ResponseEntity<Map<String, Object>> handleOverflow(
            ArithmeticException exception
    ) {
        return ResponseEntity.badRequest()
                .body(Map.<String, Object>of(
                        "message",
                        "A quantidade resultante excede o limite permitido"
                ));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleConcurrentUpdate(
            OptimisticLockingFailureException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.<String, Object>of(
                        "message",
                        "O estoque foi alterado por outra operação. "
                                + "Atualize os dados e tente novamente."
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleIntegrity(
            DataIntegrityViolationException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.<String, Object>of(
                        "message",
                        "A operação conflita com os dados existentes. "
                                + "Verifique se o tamanho já está cadastrado."
                ));
    }
}
