package com.ticketing.catalog.api.dto.request;

import com.ticketing.catalog.domain.model.EventDetail;
import com.ticketing.catalog.domain.model.EventZone;
import com.ticketing.catalog.domain.model.enums.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record CreateEventRequest(
    @NotBlank String title,
    String description,
    @NotNull EventType type,
    @NotBlank String venueId,
    @NotNull LocalDateTime startTime,
    @NotNull LocalDateTime endTime,
    @NotNull @Size(min = 1) List<EventZone> zones,
    @NotNull EventDetail detail
) {}
