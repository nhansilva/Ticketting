package com.ticketing.catalog.api.dto.request;

import com.ticketing.catalog.domain.model.ZoneConfig;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateVenueRequest(
    @NotBlank String name,
    @NotBlank String provinceCode,
    @NotBlank String provinceName,
    @NotBlank String districtCode,
    @NotBlank String districtName,
    @NotBlank String streetAddress,
    Double lat,
    Double lng,
    @NotNull @Size(min = 1) List<ZoneConfig> zones
) {}
