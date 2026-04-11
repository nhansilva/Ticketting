package com.ticketing.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * GlobalFilter chạy đầu tiên — inject X-Correlation-Id và X-Request-Id vào mọi request.
 * Downstream services đọc X-Correlation-Id để ghi log, traceId trong error response.
 */
@Slf4j
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Giữ nguyên nếu client đã gửi correlation ID, otherwise generate mới
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        String requestId = UUID.randomUUID().toString();

        final String finalCorrelationId = correlationId;

        ServerHttpRequest mutatedRequest = request.mutate()
                .header(CORRELATION_ID_HEADER, finalCorrelationId)
                .header(REQUEST_ID_HEADER, requestId)
                .build();

        // Propagate correlation ID ra response header
        exchange.getResponse().getHeaders().set(CORRELATION_ID_HEADER, finalCorrelationId);

        log.debug("Request: method={} path={} correlationId={}",
                request.getMethod(), request.getPath(), finalCorrelationId);

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        // Chạy trước tất cả filters khác — correlation ID phải có trước JwtAuthFilter
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
