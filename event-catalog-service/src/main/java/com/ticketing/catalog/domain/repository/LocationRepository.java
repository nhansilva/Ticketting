package com.ticketing.catalog.domain.repository;

import com.ticketing.catalog.domain.model.Location;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LocationRepository extends ReactiveMongoRepository<Location, String> {
    Flux<Location> findByType(String type);
    Flux<Location> findByParentCode(String parentCode);
    Mono<Boolean> existsByCode(String code);
}
