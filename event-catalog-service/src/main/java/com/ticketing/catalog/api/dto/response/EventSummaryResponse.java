package com.ticketing.catalog.api.dto.response;

import com.ticketing.catalog.domain.model.enums.EventType;
import com.ticketing.common.dto.enums.EventStatus;

import java.time.LocalDateTime;
import java.util.List;

public record EventSummaryResponse(
    String id,
    String title,
    EventType type,
    EventStatus status,
    String venueName,
    LocalDateTime startTime,
    List<String> imageUrls
) {}
