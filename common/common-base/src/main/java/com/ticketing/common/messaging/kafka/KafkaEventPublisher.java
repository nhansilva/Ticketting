package com.ticketing.common.messaging.kafka;

import com.ticketing.common.messaging.DomainEvent;
import com.ticketing.common.messaging.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Kafka implementation của EventPublisher.
 *
 * KafkaTemplate là synchronous — wrap trong Mono.fromCallable + subscribeOn(boundedElastic)
 * để không block Netty thread.
 *
 * @param <E> Payload type
 */
@Slf4j
@RequiredArgsConstructor
public class KafkaEventPublisher<E> implements EventPublisher<E> {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String sourceService;

    @Override
    public Mono<Void> publish(String topic, DomainEvent<E> event) {
        return Mono.fromCallable(() -> kafkaTemplate.send(topic, event))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(r -> log.debug("Published [{}] eventId={} to topic={}",
                        event.eventType(), event.eventId(), topic))
                .doOnError(e -> log.error("Failed to publish [{}] to topic={}: {}",
                        event.eventType(), topic, e.getMessage()))
                .then();
    }

    @Override
    public Mono<Void> publish(String topic, String key, DomainEvent<E> event) {
        return Mono.fromCallable(() -> kafkaTemplate.send(topic, key, event))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(r -> log.debug("Published [{}] key={} to topic={}",
                        event.eventType(), key, topic))
                .doOnError(e -> log.error("Failed to publish [{}] key={} to topic={}: {}",
                        event.eventType(), key, topic, e.getMessage()))
                .then();
    }
}
