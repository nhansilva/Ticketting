package com.ticketing.catalog.api.dto.request;

import com.ticketing.catalog.domain.model.EventDetail;
import com.ticketing.catalog.domain.model.EventZone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record UpdateEventRequest(
    @NotBlank String title,
    String description,
    @NotNull LocalDateTime startTime,
    @NotNull LocalDateTime endTime,
    @NotNull List<EventZone> zones,
    @NotNull EventDetail detail
) {}
