package com.ticketing.catalog.domain.model;

import java.math.BigDecimal;

public record EventZone(
    String name,
    int rows,
    int seatsPerRow,
    BigDecimal price,
    String rowPrefix
) {}
