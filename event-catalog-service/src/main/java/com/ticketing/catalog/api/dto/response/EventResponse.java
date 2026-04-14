package com.ticketing.catalog.api.dto.response;

import com.ticketing.catalog.domain.model.EventDetail;
import com.ticketing.catalog.domain.model.EventZone;
import com.ticketing.catalog.domain.model.enums.EventType;
import com.ticketing.common.dto.enums.EventStatus;

import java.time.LocalDateTime;
import java.util.List;

public record EventResponse(
    String id,
    String title,
    String description,
    EventType type,
    EventStatus status,
    String venueId,
    String venueName,
    LocalDateTime startTime,
    LocalDateTime endTime,
    List<EventZone> zones,
    List<String> imageUrls,
    EventDetail detail,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
