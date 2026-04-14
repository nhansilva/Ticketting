package com.ticketing.catalog.api.controller;

import com.ticketing.catalog.api.dto.request.CreateVenueRequest;
import com.ticketing.catalog.api.dto.response.VenueResponse;
import com.ticketing.catalog.application.service.VenueService;
import com.ticketing.common.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<VenueResponse>>> createVenue(
            @Valid @RequestBody CreateVenueRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!"ADMIN".equals(role)) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Admin access required", "FORBIDDEN")));
        }
        return venueService.createVenue(request)
            .map(v -> ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(v)));
    }

    @GetMapping
    public Mono<ApiResponse<List<VenueResponse>>> listVenues(
            @RequestParam(required = false) String provinceCode) {
        return venueService.findAll(provinceCode).collectList().map(ApiResponse::success);
    }

    @GetMapping("/{id}")
    public Mono<ApiResponse<VenueResponse>> getVenue(@PathVariable String id) {
        return venueService.findById(id).map(ApiResponse::success);
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<VenueResponse>>> updateVenue(
            @PathVariable String id,
            @Valid @RequestBody CreateVenueRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!"ADMIN".equals(role)) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Admin access required", "FORBIDDEN")));
        }
        return venueService.updateVenue(id, request)
            .map(v -> ResponseEntity.ok(ApiResponse.success(v)));
    }
}
