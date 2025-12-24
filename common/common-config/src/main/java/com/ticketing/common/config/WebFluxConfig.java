package com.ticketing.common.config;

import com.ticketing.common.dto.constants.Constants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.WebFilter;

/**
 * WebFlux configuration
 */
@Configuration
public class WebFluxConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    /**
     * Filter to add correlation ID to requests
     */
    @Bean
    public WebFilter correlationIdFilter() {
        return (exchange, chain) -> {
            String correlationId = exchange.getRequest().getHeaders()
                    .getFirst(Constants.Headers.CORRELATION_ID);
            
            if (correlationId == null || correlationId.isEmpty()) {
                correlationId = java.util.UUID.randomUUID().toString();
            }

            return chain.filter(
                    exchange.mutate()
                            .request(exchange.getRequest().mutate()
                                    .header(Constants.Headers.CORRELATION_ID, correlationId)
                                    .build())
                            .build()
            );
        };
    }
}

