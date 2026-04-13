package com.ticketing.catalog.domain.model;

import java.math.BigDecimal;

public record ZoneConfig(
    String name,
    int rows,
    int seatsPerRow,
    BigDecimal defaultPrice,
    String rowPrefix
) {}
