package com.ticketing.catalog.api.controller;

import com.ticketing.catalog.api.dto.response.SeatResponse;
import com.ticketing.catalog.application.service.SeatService;
import com.ticketing.common.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/events/{eventId}/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @GetMapping
    public Mono<ApiResponse<Map<String, List<SeatResponse>>>> getSeats(
            @PathVariable String eventId,
            @RequestParam(name = "zone", required = false) String zone) {
        return seatService.findByEvent(eventId, zone).map(ApiResponse::success);
    }
}
