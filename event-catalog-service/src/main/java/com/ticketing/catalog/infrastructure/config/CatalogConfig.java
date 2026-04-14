package com.ticketing.catalog.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketing.catalog.api.dto.response.EventResponse;
import com.ticketing.common.cache.redis.RedisCacheStrategy;
import com.ticketing.common.messaging.EventPublisher;
import com.ticketing.common.messaging.kafka.KafkaEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class CatalogConfig {

    @Bean
    public EventPublisher<Object> eventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        return new KafkaEventPublisher<>(kafkaTemplate, "event-catalog-service");
    }

    @Bean
    public RedisCacheStrategy<EventResponse> eventCacheStrategy(
            ReactiveRedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper) {
        return new RedisCacheStrategy<>(redisTemplate, objectMapper, EventResponse.class);
    }
}
