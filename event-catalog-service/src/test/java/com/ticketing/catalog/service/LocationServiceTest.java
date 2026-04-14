package com.ticketing.catalog.service;

import com.ticketing.catalog.application.service.LocationService;
import com.ticketing.catalog.domain.model.Location;
import com.ticketing.catalog.domain.repository.LocationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock LocationRepository locationRepository;
    @InjectMocks LocationService locationService;

    @Test
    void shouldReturnProvinces() {
        var province = Location.builder()
            .type("PROVINCE").code("HN").name("Hà Nội").build();

        when(locationRepository.findByType("PROVINCE")).thenReturn(Flux.just(province));

        StepVerifier.create(locationService.getProvinces())
            .expectNextMatches(r -> r.code().equals("HN") && r.name().equals("Hà Nội"))
            .verifyComplete();
    }

    @Test
    void shouldReturnDistrictsByProvince() {
        var district = Location.builder()
            .type("DISTRICT").code("HN-HK").name("Hoàn Kiếm").parentCode("HN").build();

        when(locationRepository.findByParentCode("HN")).thenReturn(Flux.just(district));

        StepVerifier.create(locationService.getDistrictsByProvince("HN"))
            .expectNextMatches(r -> r.code().equals("HN-HK") && r.parentCode().equals("HN"))
            .verifyComplete();
    }
}
