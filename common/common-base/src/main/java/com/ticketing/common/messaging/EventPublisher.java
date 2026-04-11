package com.ticketing.common.messaging;

import reactor.core.publisher.Mono;

/**
 * Event publisher interface — Observer pattern.
 * Abstraction trên Kafka / RabbitMQ — service không phụ thuộc implementation cụ thể.
 *
 * Cách dùng:
 * <pre>
 * {@code
 * @Service
 * @RequiredArgsConstructor
 * public class BookingServiceImpl {
 *     private final EventPublisher<BookingCreatedEvent> eventPublisher;
 *
 *     public Mono<BookingResponse> createBooking(...) {
 *         return bookingRepository.save(booking)
 *             .flatMap(saved -> eventPublisher.publish(
 *                 KafkaTopics.BOOKING_CREATED,
 *                 DomainEvent.of("booking.created", "booking-service", new BookingCreatedEvent(saved))
 *             ).thenReturn(mapper.toDto(saved)));
 *     }
 * }
 * }
 * </pre>
 *
 * @param <E> Payload type
 */
public interface EventPublisher<E> {

    /**
     * Publish event lên topic.
     *
     * @param topic   Kafka topic hoặc RabbitMQ routing key
     * @param event   DomainEvent wrapping payload
     */
    Mono<Void> publish(String topic, DomainEvent<E> event);

    /**
     * Publish với partition key (Kafka) để đảm bảo ordering.
     *
     * @param topic   Kafka topic
     * @param key     Partition key (bookingId, userId...)
     * @param event   DomainEvent wrapping payload
     */
    Mono<Void> publish(String topic, String key, DomainEvent<E> event);
}
