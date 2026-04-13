package com.ticketing.catalog.application.service;

import com.ticketing.catalog.api.dto.request.CreateVenueRequest;
import com.ticketing.catalog.api.dto.response.VenueResponse;
import com.ticketing.catalog.common.exception.VenueNotFoundException;
import com.ticketing.catalog.domain.repository.VenueRepository;
import com.ticketing.catalog.infrastructure.mapper.VenueMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VenueService {

    private final VenueRepository venueRepository;
    private final VenueMapper venueMapper;

    public Mono<VenueResponse> createVenue(CreateVenueRequest request) {
        var venue = venueMapper.toEntity(request);
        venue.setCreatedAt(LocalDateTime.now());
        return venueRepository.save(venue).map(venueMapper::toResponse);
    }

    public Mono<VenueResponse> findById(String id) {
        return venueRepository.findById(id)
            .switchIfEmpty(Mono.error(new VenueNotFoundException(id)))
            .map(venueMapper::toResponse);
    }

    public Flux<VenueResponse> findAll(String provinceCode) {
        var flux = (provinceCode != null && !provinceCode.isBlank())
            ? venueRepository.findByProvinceCode(provinceCode)
            : venueRepository.findAll();
        return flux.map(venueMapper::toResponse);
    }

    public Mono<VenueResponse> updateVenue(String id, CreateVenueRequest request) {
        return venueRepository.findById(id)
            .switchIfEmpty(Mono.error(new VenueNotFoundException(id)))
            .flatMap(existing -> {
                var updated = venueMapper.toEntity(request);
                updated.setId(existing.getId());
                updated.setCreatedAt(existing.getCreatedAt());
                return venueRepository.save(updated);
            })
            .map(venueMapper::toResponse);
    }
}
