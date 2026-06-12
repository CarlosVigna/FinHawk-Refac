package com.carlos.finhawk_refac.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    record ErrorResponse(int status, String error, String message) {}

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "";

        if (msg.contains("Unauthenticated")) {
            return ResponseEntity.status(401)
                    .body(new ErrorResponse(401, "Unauthorized", msg));
        }

        if (msg.contains("not allowed") || msg.contains("Access denied")) {
            return ResponseEntity.status(403)
                    .body(new ErrorResponse(403, "Forbidden", msg));
        }

        if (msg.toLowerCase().contains("not found")) {
            return ResponseEntity.status(404)
                    .body(new ErrorResponse(404, "Not Found", msg));
        }

        if (msg.contains("must belong")) {
            return ResponseEntity.status(422)
                    .body(new ErrorResponse(422, "Unprocessable Entity", msg));
        }

        return ResponseEntity.status(500)
                .body(new ErrorResponse(500, "Internal Server Error", "An unexpected error occurred"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity.status(400)
                .body(new ErrorResponse(400, "Bad Request", details));
    }
}
