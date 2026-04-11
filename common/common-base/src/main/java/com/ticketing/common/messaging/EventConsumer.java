package com.ticketing.common.messaging;

import reactor.core.publisher.Mono;

/**
 * Event consumer interface — Observer pattern.
 * Service implement interface này và đăng ký @KafkaListener hoặc @RabbitListener.
 *
 * @param <E> Payload type
 */
public interface EventConsumer<E> {

    /**
     * Xử lý event nhận được.
     * Implement phải idempotent — Kafka có thể deliver nhiều lần (at-least-once).
     *
     * @param event DomainEvent nhận được
     */
    Mono<Void> consume(DomainEvent<E> event);
}
