package com.ticketing.catalog.domain.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ConcertDetail.class, name = "CONCERT"),
    @JsonSubTypes.Type(value = MovieDetail.class, name = "MOVIE")
})
public sealed interface EventDetail permits ConcertDetail, MovieDetail {}
