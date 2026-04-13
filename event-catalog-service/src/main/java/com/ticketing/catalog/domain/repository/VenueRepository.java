package com.ticketing.catalog.domain.repository;

import com.ticketing.catalog.domain.model.Venue;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface VenueRepository extends ReactiveMongoRepository<Venue, String> {
    Flux<Venue> findByProvinceCode(String provinceCode);
}
