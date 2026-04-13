package com.ticketing.catalog.domain.model;

import com.ticketing.catalog.domain.model.enums.AgeRating;
import com.ticketing.catalog.domain.model.enums.MovieFormat;
import java.util.List;

public record MovieDetail(
    String director,
    List<String> cast,
    MovieFormat format,
    Integer durationMinutes,
    AgeRating rating
) implements EventDetail {}
