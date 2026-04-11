package com.ticketing.gateway.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketing.gateway.exception.GatewayAuthException;
import com.ticketing.gateway.exception.GatewayRateLimitException;
import com.ticketing.gateway.filter.CorrelationIdFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global error handler cho Gateway.
 * Override Spring Boot's DefaultErrorWebExceptionHandler.
 * Format response theo chuẩn CLAUDE.md:
 * { "error": { "code": "...", "message": "...", "traceId": "..." } }
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class GatewayErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status;
        String errorCode;
        String message;

        if (ex instanceof GatewayAuthException authEx) {
            status    = HttpStatus.UNAUTHORIZED;
            errorCode = authEx.getErrorCode();
            message   = authEx.getMessage();
            log.warn("Auth error [{}]: {}", exchange.getRequest().getPath(), message);

        } else if (ex instanceof GatewayRateLimitException) {
            status    = HttpStatus.TOO_MANY_REQUESTS;
            errorCode = "RATE_LIMIT_EXCEEDED";
            message   = ex.getMessage();
            log.warn("Rate limit [{}]", exchange.getRequest().getPath());

        } else if (ex instanceof ResponseStatusException rse) {
            status    = HttpStatus.valueOf(rse.getStatusCode().value());
            errorCode = "GATEWAY_ERROR";
            message   = rse.getReason() != null ? rse.getReason() : "Request failed";

        } else {
            status    = HttpStatus.INTERNAL_SERVER_ERROR;
            errorCode = "INTERNAL_ERROR";
            message   = "An unexpected error occurred";
            log.error("Unhandled gateway error [{}]:", exchange.getRequest().getPath(), ex);
        }

        String traceId = exchange.getRequest().getHeaders()
                .getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
        if (traceId == null) traceId = UUID.randomUUID().toString();

        // Build error body theo chuẩn CLAUDE.md
        Map<String, Object> errorDetail = new LinkedHashMap<>();
        errorDetail.put("code",      errorCode);
        errorDetail.put("message",   message);
        errorDetail.put("traceId",   traceId);
        errorDetail.put("timestamp", Instant.now().toString());

        Map<String, Object> body = Map.of("error", errorDetail);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            bytes = "{\"error\":{\"code\":\"SERIALIZATION_ERROR\",\"message\":\"Failed to serialize error\"}}"
                    .getBytes(StandardCharsets.UTF_8);  // fix: explicit charset
        }

        // fix: response đã committed thì không ghi nữa (tránh IllegalStateException)
        if (exchange.getResponse().isCommitted()) {
            log.warn("Response already committed, cannot write error body");
            return Mono.empty();
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().set(CorrelationIdFilter.CORRELATION_ID_HEADER, traceId);

        if (status == HttpStatus.TOO_MANY_REQUESTS) {
            exchange.getResponse().getHeaders().set("Retry-After", "60");
        }

        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
