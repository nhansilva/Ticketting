package com.ticketing.catalog.domain.model;

import com.ticketing.catalog.domain.model.enums.EventType;
import com.ticketing.common.dto.enums.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("events")
public class Event {
    @Id
    private String id;
    private String title;
    private String description;
    private EventType type;
    private EventStatus status;
    private String venueId;
    private String venueName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<EventZone> zones;
    private List<String> imageUrls;
    private EventDetail detail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
}
