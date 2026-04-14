package com.ticketing.catalog.domain.model;

import java.util.List;

public record ConcertDetail(
    List<String> artists,
    List<String> genres,
    Integer ageRestriction
) implements EventDetail {}
