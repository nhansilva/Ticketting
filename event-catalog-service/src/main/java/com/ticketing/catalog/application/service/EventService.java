package com.ticketing.catalog.application.service;

import com.ticketing.catalog.api.dto.request.CreateEventRequest;
import com.ticketing.catalog.api.dto.request.UpdateEventRequest;
import com.ticketing.catalog.api.dto.request.UpdateEventStatusRequest;
import com.ticketing.catalog.api.dto.response.EventResponse;
import com.ticketing.catalog.api.dto.response.EventSummaryResponse;
import com.ticketing.catalog.common.exception.EventNotFoundException;
import com.ticketing.catalog.common.exception.InvalidStatusTransitionException;
import com.ticketing.catalog.common.exception.VenueNotFoundException;
import com.ticketing.catalog.domain.model.Event;
import com.ticketing.catalog.domain.model.enums.EventType;
import com.ticketing.catalog.domain.repository.EventRepository;
import com.ticketing.catalog.domain.repository.VenueRepository;
import com.ticketing.catalog.infrastructure.mapper.EventMapper;
import com.ticketing.common.dto.constants.Constants;
import com.ticketing.common.dto.enums.EventStatus;
import com.ticketing.common.exception.domain.ConflictException;
import com.ticketing.common.messaging.DomainEvent;
import com.ticketing.common.messaging.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private static final Set<EventStatus> ALLOWED_UPDATE_STATUSES = Set.of(EventStatus.DRAFT);

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final EventMapper eventMapper;
    private final SeatService seatService;
    @SuppressWarnings("rawtypes")
    private final EventPublisher eventPublisher;

    public Mono<EventResponse> createEvent(CreateEventRequest request, String createdBy) {
        return venueRepository.findById(request.venueId())
            .switchIfEmpty(Mono.error(new VenueNotFoundException(request.venueId())))
            .flatMap(venue -> {
                var event = eventMapper.toEntity(request);
                event.setVenueName(venue.getName());
                event.setStatus(EventStatus.DRAFT);
                event.setCreatedBy(createdBy);
                event.setCreatedAt(LocalDateTime.now());
                event.setUpdatedAt(LocalDateTime.now());
                return eventRepository.save(event);
            })
            .map(eventMapper::toResponse);
    }

    public Mono<EventResponse> findById(String id) {
        return eventRepository.findById(id)
            .switchIfEmpty(Mono.error(new EventNotFoundException(id)))
            .map(eventMapper::toResponse);
    }

    public Flux<EventSummaryResponse> findAll(EventType type, EventStatus status, int page, int size) {
        var pageable = PageRequest.of(page, size);
        Flux<Event> flux;
        if (type != null && status != null) {
            flux = eventRepository.findByTypeAndStatus(type, status, pageable);
        } else if (type != null) {
            flux = eventRepository.findByType(type, pageable);
        } else if (status != null) {
            flux = eventRepository.findByStatus(status, pageable);
        } else {
            flux = eventRepository.findAllBy(pageable);
        }
        return flux.map(eventMapper::toSummaryResponse);
    }

    public Mono<EventResponse> updateEvent(String id, UpdateEventRequest request, String userId) {
        return eventRepository.findById(id)
            .switchIfEmpty(Mono.error(new EventNotFoundException(id)))
            .flatMap(event -> {
                if (!ALLOWED_UPDATE_STATUSES.contains(event.getStatus())) {
                    return Mono.error(new ConflictException(
                        "Event can only be updated when DRAFT, current: " + event.getStatus(),
                        "EVENT_NOT_UPDATABLE"));
                }
                event.setTitle(request.title());
                event.setDescription(request.description());
                event.setStartTime(request.startTime());
                event.setEndTime(request.endTime());
                event.setZones(request.zones());
                event.setDetail(request.detail());
                event.setUpdatedAt(LocalDateTime.now());
                return eventRepository.save(event);
            })
            .flatMap(saved -> publishEventUpdated(saved).thenReturn(saved))
            .map(eventMapper::toResponse);
    }

    public Mono<EventResponse> updateStatus(String id, UpdateEventStatusRequest request) {
        return eventRepository.findById(id)
            .switchIfEmpty(Mono.error(new EventNotFoundException(id)))
            .flatMap(event -> validateAndTransition(event, request.status()))
            .map(eventMapper::toResponse);
    }

    public Mono<EventResponse> cancelEvent(String id) {
        return eventRepository.findById(id)
            .switchIfEmpty(Mono.error(new EventNotFoundException(id)))
            .flatMap(event -> {
                if (event.getStatus() == EventStatus.COMPLETED) {
                    return Mono.error(new InvalidStatusTransitionException(
                        event.getStatus(), EventStatus.CANCELLED));
                }
                event.setStatus(EventStatus.CANCELLED);
                event.setUpdatedAt(LocalDateTime.now());
                return seatService.deleteByEvent(id)
                    .then(eventRepository.save(event));
            })
            .map(eventMapper::toResponse);
    }

    private Mono<Event> validateAndTransition(Event event, EventStatus newStatus) {
        boolean valid = switch (event.getStatus()) {
            case DRAFT -> newStatus == EventStatus.ON_SALE || newStatus == EventStatus.CANCELLED;
            case ON_SALE -> newStatus == EventStatus.CANCELLED || newStatus == EventStatus.COMPLETED;
            case SOLD_OUT -> newStatus == EventStatus.COMPLETED;
            default -> false;
        };

        if (!valid) {
            return Mono.error(new InvalidStatusTransitionException(event.getStatus(), newStatus));
        }

        event.setStatus(newStatus);
        event.setUpdatedAt(LocalDateTime.now());

        if (newStatus == EventStatus.ON_SALE) {
            return seatService.generateSeats(event.getId(), event.getZones())
                .then(eventRepository.save(event))
                .flatMap(saved -> publishEventPublished(saved).thenReturn(saved));
        }

        if (newStatus == EventStatus.CANCELLED) {
            return seatService.deleteByEvent(event.getId())
                .then(eventRepository.save(event));
        }

        return eventRepository.save(event);
    }

    @SuppressWarnings("unchecked")
    private Mono<Void> publishEventPublished(Event event) {
        record EventPublishedPayload(String eventId, String title, String type,
            String venueId, LocalDateTime startTime, String status) {}
        var payload = new EventPublishedPayload(event.getId(), event.getTitle(),
            event.getType().name(), event.getVenueId(), event.getStartTime(),
            event.getStatus().name());
        return eventPublisher.publish(
            Constants.KafkaTopics.EVENT_PUBLISHED, event.getId(),
            DomainEvent.of("event.published", "event-catalog-service", payload));
    }

    @SuppressWarnings("unchecked")
    private Mono<Void> publishEventUpdated(Event event) {
        record EventUpdatedPayload(String eventId) {}
        return eventPublisher.publish(
            Constants.KafkaTopics.EVENT_UPDATED, event.getId(),
            DomainEvent.of("event.updated", "event-catalog-service",
                new EventUpdatedPayload(event.getId())));
    }
}
