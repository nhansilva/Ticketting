package com.ticketing.catalog.api.controller;

import com.ticketing.catalog.api.dto.response.LocationResponse;
import com.ticketing.catalog.application.service.LocationService;
import com.ticketing.common.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/provinces")
    public Mono<ApiResponse<List<LocationResponse>>> getProvinces() {
        return locationService.getProvinces()
            .collectList()
            .map(ApiResponse::success);
    }

    @GetMapping("/provinces/{code}/districts")
    public Mono<ApiResponse<List<LocationResponse>>> getDistricts(@PathVariable String code) {
        return locationService.getDistrictsByProvince(code)
            .collectList()
            .map(ApiResponse::success);
    }
}
