package com.ticketing.catalog.service;

import com.ticketing.catalog.api.dto.request.CreateVenueRequest;
import com.ticketing.catalog.api.dto.response.VenueResponse;
import com.ticketing.catalog.application.service.VenueService;
import com.ticketing.catalog.common.exception.VenueNotFoundException;
import com.ticketing.catalog.domain.model.Venue;
import com.ticketing.catalog.domain.model.ZoneConfig;
import com.ticketing.catalog.domain.repository.VenueRepository;
import com.ticketing.catalog.infrastructure.mapper.VenueMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VenueServiceTest {

    @Mock VenueRepository venueRepository;
    @Mock VenueMapper venueMapper;
    @InjectMocks VenueService venueService;

    private Venue sampleVenue() {
        return Venue.builder()
            .id("v1").name("Nhà hát Lớn")
            .provinceCode("HN").provinceName("Hà Nội")
            .districtCode("HN-HK").districtName("Hoàn Kiếm")
            .streetAddress("1 Tràng Tiền")
            .totalCapacity(500)
            .createdAt(LocalDateTime.now())
            .zones(List.of(new ZoneConfig("VIP", 5, 10, BigDecimal.valueOf(500000), "A")))
            .build();
    }

    private VenueResponse sampleResponse(Venue venue) {
        return new VenueResponse("v1", "Nhà hát Lớn", "HN", "Hà Nội",
            "HN-HK", "Hoàn Kiếm", "1 Tràng Tiền", 21.02, 105.84, 500,
            venue.getZones(), venue.getCreatedAt());
    }

    @Test
    void shouldCreateVenue() {
        var request = new CreateVenueRequest("Nhà hát Lớn", "HN", "Hà Nội",
            "HN-HK", "Hoàn Kiếm", "1 Tràng Tiền", 21.02, 105.84,
            List.of(new ZoneConfig("VIP", 5, 10, BigDecimal.valueOf(500000), "A")));

        var venue = sampleVenue();
        when(venueMapper.toEntity(request)).thenReturn(venue);
        when(venueRepository.save(any())).thenReturn(Mono.just(venue));
        when(venueMapper.toResponse(venue)).thenReturn(sampleResponse(venue));

        StepVerifier.create(venueService.createVenue(request))
            .expectNextMatches(r -> r.name().equals("Nhà hát Lớn"))
            .verifyComplete();
    }

    @Test
    void shouldThrowWhenVenueNotFound() {
        when(venueRepository.findById("not-exist")).thenReturn(Mono.empty());

        StepVerifier.create(venueService.findById("not-exist"))
            .expectError(VenueNotFoundException.class)
            .verify();
    }
}
