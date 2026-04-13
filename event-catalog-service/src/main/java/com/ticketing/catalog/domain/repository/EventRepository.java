package com.ticketing.catalog.domain.repository;

import com.ticketing.catalog.domain.model.Event;
import com.ticketing.catalog.domain.model.enums.EventType;
import com.ticketing.common.dto.enums.EventStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface EventRepository extends ReactiveMongoRepository<Event, String> {
    Flux<Event> findByTypeAndStatus(EventType type, EventStatus status, Pageable pageable);
    Flux<Event> findByStatus(EventStatus status, Pageable pageable);
    Flux<Event> findByType(EventType type, Pageable pageable);
    Flux<Event> findAllBy(Pageable pageable);
}
