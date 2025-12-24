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

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Global exception handler for reactive Spring WebFlux
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TicketingException.class)
    public ResponseEntity<ErrorResponse> handleTicketingException(
            TicketingException ex,
            ServerWebExchange exchange) {
        log.error("Ticketing exception: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(exchange.getRequest().getPath().value())
                .build();

        HttpStatus status = determineHttpStatus(ex.getErrorCode());
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            WebExchangeBindException ex,
            ServerWebExchange exchange) {
        log.error("Validation exception: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(Constants.ErrorCodes.VALIDATION_ERROR)
                .message("Validation failed")
                .timestamp(LocalDateTime.now())
                .path(exchange.getRequest().getPath().value())
                .fieldErrors(ex.getBindingResult().getFieldErrors().stream()
                        .map(error -> ErrorResponse.FieldError.builder()
                                .field(error.getField())
                                .message(error.getDefaultMessage())
                                .rejectedValue(error.getRejectedValue())
                                .build())
                        .collect(Collectors.toList()))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            ServerWebExchange exchange) {
        log.error("Unexpected exception: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(Constants.ErrorCodes.INTERNAL_ERROR)
                .message("An unexpected error occurred")
                .timestamp(LocalDateTime.now())
                .path(exchange.getRequest().getPath().value())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    private HttpStatus determineHttpStatus(String errorCode) {
        return switch (errorCode) {
            case Constants.ErrorCodes.TICKET_NOT_AVAILABLE,
                 Constants.ErrorCodes.INSUFFICIENT_INVENTORY -> HttpStatus.NOT_FOUND;
            case Constants.ErrorCodes.TICKET_ALREADY_RESERVED,
                 Constants.ErrorCodes.BOOKING_EXPIRED -> HttpStatus.CONFLICT;
            case Constants.ErrorCodes.BOOKING_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case Constants.ErrorCodes.PAYMENT_FAILED -> HttpStatus.PAYMENT_REQUIRED;
            case Constants.ErrorCodes.RATE_LIMIT_EXCEEDED -> HttpStatus.TOO_MANY_REQUESTS;
            case Constants.ErrorCodes.VALIDATION_ERROR -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}

