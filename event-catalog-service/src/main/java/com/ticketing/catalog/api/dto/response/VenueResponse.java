package com.ticketing.catalog.api.dto.response;

import com.ticketing.catalog.domain.model.ZoneConfig;

import java.time.LocalDateTime;
import java.util.List;

public record VenueResponse(
    String id,
    String name,
    String provinceCode,
    String provinceName,
    String districtCode,
    String districtName,
    String streetAddress,
    Double lat,
    Double lng,
    int totalCapacity,
    List<ZoneConfig> zones,
    LocalDateTime createdAt
) {}
