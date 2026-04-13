package com.ticketing.catalog.infrastructure.mapper;

import com.ticketing.catalog.api.dto.request.CreateVenueRequest;
import com.ticketing.catalog.api.dto.response.VenueResponse;
import com.ticketing.catalog.domain.model.Venue;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VenueMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "totalCapacity", expression = "java(request.zones().stream().mapToInt(z -> z.rows() * z.seatsPerRow()).sum())")
    Venue toEntity(CreateVenueRequest request);

    VenueResponse toResponse(Venue venue);
}
