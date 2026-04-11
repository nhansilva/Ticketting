package com.ticketing.common.exception.handler;

import com.ticketing.common.dto.constants.Constants;
import com.ticketing.common.dto.response.ErrorResponse;
import com.ticketing.common.exception.TicketingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Global exception handler — WebFlux reactive.
 * Đọc HttpStatus trực tiếp từ TicketingException.httpStatus (không còn switch-case).
 * Tất cả services import common-base đều dùng handler này.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TicketingException.class)
    public ResponseEntity<ErrorResponse> handleTicketingException(
            TicketingException ex, ServerWebExchange exchange) {
        log.error("[{}] {}: {}", ex.getHttpStatus(), ex.getErrorCode(), ex.getMessage());

        String traceId = exchange.getRequest().getHeaders()
                .getFirst(Constants.Headers.CORRELATION_ID);

        ErrorResponse error = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .timestamp(Instant.now())
                .path(exchange.getRequest().getPath().value())
                .traceId(traceId)
                .build();

        return ResponseEntity.status(ex.getHttpStatus()).body(error);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            WebExchangeBindException ex, ServerWebExchange exchange) {
        log.warn("Validation failed: {}", ex.getMessage());

        String traceId = exchange.getRequest().getHeaders()
                .getFirst(Constants.Headers.CORRELATION_ID);

        ErrorResponse error = ErrorResponse.builder()
                .errorCode(Constants.ErrorCodes.VALIDATION_ERROR)
                .message("Validation failed")
                .timestamp(Instant.now())
                .path(exchange.getRequest().getPath().value())
                .traceId(traceId)
                .fieldErrors(ex.getBindingResult().getFieldErrors().stream()
                        .map(f -> new ErrorResponse.FieldError(f.getField(), f.getDefaultMessage(), f.getRejectedValue()))
                        .collect(Collectors.toList()))
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex, ServerWebExchange exchange) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        String traceId = exchange.getRequest().getHeaders()
                .getFirst(Constants.Headers.CORRELATION_ID);

        ErrorResponse error = ErrorResponse.builder()
                .errorCode(Constants.ErrorCodes.INTERNAL_ERROR)
                .message("An unexpected error occurred")
                .timestamp(Instant.now())
                .path(exchange.getRequest().getPath().value())
                .traceId(traceId)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
