package com.carlos.finhawk_refac.config;

import com.carlos.finhawk_refac.exception.TooManyRequestsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    record ErrorResponse(int status, String error, String message) {}

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyRequests(TooManyRequestsException ex) {
        return ResponseEntity.status(429)
                .body(new ErrorResponse(429, "Too Many Requests", ex.getMessage()));
    }

    // Spring Security lanca isso (ex: BadCredentialsException) direto do
    // AuthenticationManager.authenticate() no AuthenticationController --
    // sem esse handler especifico, cai no fallback generico do
    // handleRuntimeException abaixo porque a mensagem padrao ("Bad
    // credentials") nao bate em nenhum dos substrings verificados ali,
    // e login errado virava 500 em vez de 401.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        return ResponseEntity.status(401)
                .body(new ErrorResponse(401, "Unauthorized", "E-mail ou senha inválidos."));
    }

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

        if (msg.contains("already registered")) {
            return ResponseEntity.status(400)
                    .body(new ErrorResponse(400, "Bad Request", msg));
        }

        if (msg.toLowerCase().contains("inválido") || msg.toLowerCase().contains("invalid")) {
            return ResponseEntity.status(400)
                    .body(new ErrorResponse(400, "Bad Request", msg));
        }

        return ResponseEntity.status(500)
                .body(new ErrorResponse(500, "Internal Server Error", "An unexpected error occurred"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Bad Request", ex.getMessage()));
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
