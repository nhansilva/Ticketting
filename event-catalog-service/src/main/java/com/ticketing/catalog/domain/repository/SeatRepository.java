package com.ticketing.catalog.domain.repository;

import com.ticketing.catalog.domain.model.Seat;
import com.ticketing.catalog.domain.model.enums.SeatStatus;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SeatRepository extends ReactiveMongoRepository<Seat, String> {
    Flux<Seat> findByEventId(String eventId);
    Flux<Seat> findByEventIdAndZone(String eventId, String zone);
    Mono<Void> deleteByEventId(String eventId);
    Mono<Long> countByEventIdAndStatus(String eventId, SeatStatus status);
}
