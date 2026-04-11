package com.ticketing.common.config.web;

import com.ticketing.common.dto.constants.Constants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.WebFilter;

import java.util.UUID;

/**
 * WebFlux shared configuration.
 * CorrelationIdFilter propagates X-Correlation-Id từ incoming request sang
 * tất cả outgoing calls — cần thiết để trace request xuyên suốt các service.
 */
@Configuration
public class WebFluxConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    /**
     * Propagate hoặc generate X-Correlation-Id cho mọi request.
     * Gateway đã inject X-Correlation-Id, filter này giữ nguyên để forward xuống service.
     */
    @Bean
    public WebFilter correlationIdFilter() {
        return (exchange, chain) -> {
            String correlationId = exchange.getRequest().getHeaders()
                    .getFirst(Constants.Headers.CORRELATION_ID);

            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }

            final String finalCorrelationId = correlationId;
            return chain.filter(
                    exchange.mutate()
                            .request(exchange.getRequest().mutate()
                                    .header(Constants.Headers.CORRELATION_ID, finalCorrelationId)
                                    .build())
                            .build()
            );
        };
    }
}
