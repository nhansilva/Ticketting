package com.ticketing.catalog.service;

import com.ticketing.catalog.api.dto.request.CreateEventRequest;
import com.ticketing.catalog.api.dto.request.UpdateEventStatusRequest;
import com.ticketing.catalog.api.dto.response.EventResponse;
import com.ticketing.catalog.application.service.EventService;
import com.ticketing.catalog.application.service.SeatService;
import com.ticketing.catalog.common.exception.EventNotFoundException;
import com.ticketing.catalog.common.exception.InvalidStatusTransitionException;
import com.ticketing.catalog.common.exception.VenueNotFoundException;
import com.ticketing.catalog.domain.model.Event;
import com.ticketing.catalog.domain.model.EventZone;
import com.ticketing.catalog.domain.model.Venue;
import com.ticketing.catalog.domain.model.ZoneConfig;
import com.ticketing.catalog.domain.model.enums.EventType;
import com.ticketing.catalog.domain.repository.EventRepository;
import com.ticketing.catalog.domain.repository.VenueRepository;
import com.ticketing.catalog.infrastructure.mapper.EventMapper;
import com.ticketing.common.dto.enums.EventStatus;
import com.ticketing.common.messaging.EventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock EventRepository eventRepository;
    @Mock VenueRepository venueRepository;
    @Mock EventMapper eventMapper;
    @Mock SeatService seatService;
    @Mock EventPublisher eventPublisher;
    @InjectMocks EventService eventService;

    private Venue sampleVenue() {
        return Venue.builder().id("v1").name("Nhà hát Lớn")
            .zones(List.of(new ZoneConfig("VIP", 5, 10, BigDecimal.valueOf(500000), "A")))
            .build();
    }

    private Event sampleEvent(EventStatus status) {
        return Event.builder()
            .id("e1").title("Concert A").type(EventType.CONCERT)
            .status(status).venueId("v1").venueName("Nhà hát Lớn")
            .zones(List.of(new EventZone("VIP", 5, 10, BigDecimal.valueOf(500000), "A")))
            .startTime(LocalDateTime.now().plusDays(7))
            .endTime(LocalDateTime.now().plusDays(7).plusHours(3))
            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
            .build();
    }

    private EventResponse sampleResponse(Event event) {
        return new EventResponse(event.getId(), event.getTitle(), null, event.getType(),
            event.getStatus(), event.getVenueId(), event.getVenueName(),
            event.getStartTime(), event.getEndTime(), event.getZones(),
            List.of(), null, event.getCreatedAt(), event.getUpdatedAt());
    }

    @Test
    void shouldCreateEventWithDraftStatus() {
        var request = new CreateEventRequest("Concert A", "Desc", EventType.CONCERT,
            "v1", LocalDateTime.now().plusDays(7), LocalDateTime.now().plusDays(7).plusHours(3),
            List.of(new EventZone("VIP", 5, 10, BigDecimal.valueOf(500000), "A")), null);

        var event = sampleEvent(EventStatus.DRAFT);
        var response = sampleResponse(event);

        when(venueRepository.findById("v1")).thenReturn(Mono.just(sampleVenue()));
        when(eventMapper.toEntity(request)).thenReturn(event);
        when(eventRepository.save(any())).thenReturn(Mono.just(event));
        when(eventMapper.toResponse(event)).thenReturn(response);

        StepVerifier.create(eventService.createEvent(request, "user1"))
            .expectNextMatches(r -> r.status() == EventStatus.DRAFT && r.title().equals("Concert A"))
            .verifyComplete();
    }

    @Test
    void shouldThrowWhenVenueNotFoundOnCreate() {
        var request = new CreateEventRequest("Concert A", "Desc", EventType.CONCERT,
            "not-exist", LocalDateTime.now().plusDays(7), LocalDateTime.now().plusDays(7).plusHours(3),
            List.of(), null);

        when(venueRepository.findById("not-exist")).thenReturn(Mono.empty());

        StepVerifier.create(eventService.createEvent(request, "user1"))
            .expectError(VenueNotFoundException.class)
            .verify();
    }

    @Test
    void shouldThrowWhenEventNotFound() {
        when(eventRepository.findById("not-exist")).thenReturn(Mono.empty());

        StepVerifier.create(eventService.findById("not-exist"))
            .expectError(EventNotFoundException.class)
            .verify();
    }

    @Test
    void shouldThrowWhenInvalidTransitionOnSaleToDraft() {
        var event = sampleEvent(EventStatus.ON_SALE);
        var request = new UpdateEventStatusRequest(EventStatus.DRAFT);

        when(eventRepository.findById("e1")).thenReturn(Mono.just(event));

        StepVerifier.create(eventService.updateStatus("e1", request))
            .expectError(InvalidStatusTransitionException.class)
            .verify();
    }

    @Test
    void shouldGenerateSeatsAndPublishWhenTransitionToOnSale() {
        var event = sampleEvent(EventStatus.DRAFT);
        var updatedEvent = sampleEvent(EventStatus.ON_SALE);
        var response = sampleResponse(updatedEvent);
        var request = new UpdateEventStatusRequest(EventStatus.ON_SALE);

        when(eventRepository.findById("e1")).thenReturn(Mono.just(event));
        when(seatService.generateSeats(anyString(), any())).thenReturn(Flux.empty());
        when(eventRepository.save(any())).thenReturn(Mono.just(updatedEvent));
        when(eventPublisher.publish(anyString(), anyString(), any())).thenReturn(Mono.empty());
        when(eventMapper.toResponse(any())).thenReturn(response);

        StepVerifier.create(eventService.updateStatus("e1", request))
            .expectNextMatches(r -> r.status() == EventStatus.ON_SALE)
            .verifyComplete();
    }
}
