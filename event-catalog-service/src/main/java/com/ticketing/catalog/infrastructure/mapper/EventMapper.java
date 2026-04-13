package com.ticketing.catalog.infrastructure.mapper;

import com.ticketing.catalog.api.dto.request.CreateEventRequest;
import com.ticketing.catalog.api.dto.response.EventResponse;
import com.ticketing.catalog.api.dto.response.EventSummaryResponse;
import com.ticketing.catalog.domain.model.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", expression = "java(com.ticketing.common.dto.enums.EventStatus.DRAFT)")
    @Mapping(target = "imageUrls", expression = "java(new java.util.ArrayList<>())")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "venueName", ignore = true)
    Event toEntity(CreateEventRequest request);

    EventResponse toResponse(Event event);

    EventSummaryResponse toSummaryResponse(Event event);
}
