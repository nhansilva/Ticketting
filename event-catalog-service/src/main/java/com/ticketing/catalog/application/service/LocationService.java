package com.ticketing.catalog.application.service;

import com.ticketing.catalog.api.dto.response.LocationResponse;
import com.ticketing.catalog.domain.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;

    public Flux<LocationResponse> getProvinces() {
        return locationRepository.findByType("PROVINCE")
            .map(l -> new LocationResponse(l.getCode(), l.getName(), null));
    }

    public Flux<LocationResponse> getDistrictsByProvince(String provinceCode) {
        return locationRepository.findByParentCode(provinceCode)
            .map(l -> new LocationResponse(l.getCode(), l.getName(), l.getParentCode()));
    }
}
