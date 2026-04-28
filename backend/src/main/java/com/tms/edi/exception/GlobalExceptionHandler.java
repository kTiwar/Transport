package com.tms.edi.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(EdiException.class)
    public ResponseEntity<ErrorResponse> handleEdiException(EdiException ex, WebRequest req) {
        log.warn("EDI error: {}", ex.getMessage());
        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(ErrorResponse.of(ex.getHttpStatus(), ex.getMessage(), req));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, WebRequest req) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(HttpStatus.FORBIDDEN, "Access denied", req));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthenticationException ex, WebRequest req) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(HttpStatus.UNAUTHORIZED, "Authentication required", req));
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ErrorResponse> handleUnsupported(UnsupportedOperationException ex, WebRequest req) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), req));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest req) {

        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(FieldError::getField,
                        e -> e.getDefaultMessage() != null ? e.getDefaultMessage() : "Invalid",
                        (a, b) -> a));

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Request validation failed")
                .path(req.getDescription(false))
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, WebRequest req) {
        log.error("Unhandled exception", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Internal server error — please contact support", req));
    }

    // ── Response body ─────────────────────────────────────────────────────────

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ErrorResponse {
        private OffsetDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private String path;
        private Map<String, String> fieldErrors;

        static ErrorResponse of(HttpStatus hs, String message, WebRequest req) {
            return ErrorResponse.builder()
                    .timestamp(OffsetDateTime.now())
                    .status(hs.value())
                    .error(hs.getReasonPhrase())
                    .message(message)
                    .path(req.getDescription(false))
                    .build();
        }
    }
}
