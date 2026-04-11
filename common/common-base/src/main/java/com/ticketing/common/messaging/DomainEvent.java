package com.ticketing.common.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event wrapper — Observer pattern.
 * Mọi Kafka/RabbitMQ message đều được wrap trong DomainEvent để có metadata chuẩn.
 *
 * Dùng record (Java 25) — immutable, tự có equals/hashCode.
 *
 * @param <T> Payload type (booking created, payment completed, etc.)
 */
public record DomainEvent<T>(
        /** Unique event ID để idempotent check phía consumer */
        String eventId,
        /** Tên event: "booking.created", "payment.completed", ... */
        String eventType,
        /** Service phát sinh event: "user-service", "booking-service", ... */
        String sourceService,
        /** Thời điểm event xảy ra */
        Instant occurredAt,
        /** Payload chứa data của event */
        T payload
) {
    /** Factory method — tự sinh eventId và occurredAt */
    public static <T> DomainEvent<T> of(String eventType, String sourceService, T payload) {
        return new DomainEvent<>(
                UUID.randomUUID().toString(),
                eventType,
                sourceService,
                Instant.now(),
                payload
        );
    }
}
