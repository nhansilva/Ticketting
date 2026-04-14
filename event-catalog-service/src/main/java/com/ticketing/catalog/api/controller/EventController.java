package com.ticketing.catalog.api.controller;

import com.ticketing.catalog.api.dto.request.CreateEventRequest;
import com.ticketing.catalog.api.dto.request.UpdateEventRequest;
import com.ticketing.catalog.api.dto.request.UpdateEventStatusRequest;
import com.ticketing.catalog.api.dto.response.EventResponse;
import com.ticketing.catalog.api.dto.response.EventSummaryResponse;
import com.ticketing.catalog.application.service.EventService;
import com.ticketing.catalog.domain.model.enums.EventType;
import com.ticketing.common.dto.enums.EventStatus;
import com.ticketing.common.dto.response.ApiResponse;
import com.ticketing.common.exception.TicketingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<EventResponse>>> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (!"ADMIN".equals(role)) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Admin access required", "FORBIDDEN")));
        }
        return eventService.createEvent(request, userId)
            .map(e -> ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(e)));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<EventResponse>>> getEvent(
            @PathVariable("id") String id) {
        return eventService.findById(id)
            .map(e -> ResponseEntity.ok(ApiResponse.success(e)))
            .onErrorResume(TicketingException.class, ex ->
                Mono.just(ResponseEntity.status(ex.getHttpStatus())
                    .body(ApiResponse.error(ex.getMessage(), ex.getErrorCode()))));
    }

    @GetMapping
    public Mono<ApiResponse<List<EventSummaryResponse>>> listEvents(
            @RequestParam(required = false) EventType type,
            @RequestParam(required = false) EventStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return eventService.findAll(type, status, page, size)
            .collectList()
            .map(ApiResponse::success);
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<EventResponse>>> updateEvent(
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateEventRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (!"ADMIN".equals(role)) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Admin access required", "FORBIDDEN")));
        }
        return eventService.updateEvent(id, request, userId)
            .map(e -> ResponseEntity.ok(ApiResponse.success(e)));
    }

    @PutMapping("/{id}/status")
    public Mono<ResponseEntity<ApiResponse<EventResponse>>> updateStatus(
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateEventStatusRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!"ADMIN".equals(role)) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Admin access required", "FORBIDDEN")));
        }
        return eventService.updateStatus(id, request)
            .map(e -> ResponseEntity.ok(ApiResponse.success(e)));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<EventResponse>>> cancelEvent(
            @PathVariable("id") String id,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!"ADMIN".equals(role)) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Admin access required", "FORBIDDEN")));
        }
        return eventService.cancelEvent(id)
            .map(e -> ResponseEntity.ok(ApiResponse.success(e)));
    }
}
